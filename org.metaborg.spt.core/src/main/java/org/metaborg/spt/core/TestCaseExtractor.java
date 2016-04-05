package org.metaborg.spt.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.analysis.AnalysisException;
import org.metaborg.core.analysis.AnalysisFileResult;
import org.metaborg.core.analysis.AnalysisResult;
import org.metaborg.core.analysis.IAnalysisService;
import org.metaborg.core.context.ContextException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageBuilder;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.syntax.ISyntaxService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.core.syntax.ParseResult;
import org.metaborg.spt.core.ITestCase.ExpectationPair;
import org.metaborg.spt.core.util.SPTUtil;
import org.metaborg.util.iterators.Iterables2;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;
import org.spoofax.terms.TermVisitor;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;

public class TestCaseExtractor implements ITestCaseExtractor {

    private final ISyntaxService<IStrategoTerm> parseService;
    private final IAnalysisService<IStrategoTerm, IStrategoTerm> analysisService;
    private final IContextService contextService;
    private final ITestCaseBuilder testBuilder;

    @Inject public TestCaseExtractor(ISyntaxService<IStrategoTerm> parseService,
        IAnalysisService<IStrategoTerm, IStrategoTerm> analysisService, IContextService contextService,
        ITestCaseBuilder builder) {
        this.parseService = parseService;
        this.analysisService = analysisService;
        this.contextService = contextService;
        this.testBuilder = builder;
    }

    @Override public ITestCaseExtractionResult extract(ILanguageImpl spt, IProject project,
        final FileObject testSuite) {
        InputStream in;
        final ParseResult<IStrategoTerm> parseResult;
        final AnalysisResult<IStrategoTerm, IStrategoTerm> analysisResult;
        try {
            in = testSuite.getContent().getInputStream();
            String text = IOUtils.toString(in);
            in.close();
            parseResult = parseService.parse(text, testSuite, spt, null);
            if(parseResult.result == null) {
                // parse failed and couldn't recover
                return new TestCaseExtractionResult(parseResult, null, Iterables2.<IMessage>empty(),
                    Iterables2.<ITestCase>empty());
            }
        } catch(IOException ioe) {
            // @formatter:off
            IMessage error = MessageBuilder.create()
                .asInternal()
                .withException(ioe)
                .withSeverity(MessageSeverity.ERROR)
                .withSource(testSuite)
                .withMessage("Failed to read the testsuite " + testSuite.getName().getBaseName())
                .build();
            // @formatter:on
            return new TestCaseExtractionResult(null, null, Iterables2.singleton(error), Iterables2.<ITestCase>empty());
        } catch(ParseException pe) {
            // @formatter:off
            IMessage error = MessageBuilder.create()
                .asParser()
                .withException(pe)
                .withSeverity(MessageSeverity.ERROR)
                .withSource(testSuite)
                .withMessage(pe.getMessage())
                .build();
            // @formatter:on
            return new TestCaseExtractionResult(null, null, Iterables2.singleton(error), Iterables2.<ITestCase>empty());
        }

        try {
            // even if parsing fails we can still analyze
            // the result will just be empty
            IContext ctx = contextService.get(testSuite, project, spt);
            analysisResult = analysisService.analyze(Iterables2.singleton(parseResult), ctx);
        } catch(ContextException | AnalysisException ae) {
            // @formatter:off
            IMessage error = MessageBuilder.create()
                .asAnalysis()
                .withException(ae)
                .withSeverity(MessageSeverity.ERROR)
                .withSource(testSuite)
                .withMessage(ae.getMessage())
                .build();
            // @formatter:on
            return new TestCaseExtractionResult(parseResult, null, Iterables2.singleton(error),
                Iterables2.<ITestCase>empty());
        }

        // Retrieve the AST from the analysis result
        Iterable<AnalysisFileResult<IStrategoTerm, IStrategoTerm>> analysisFileResults = analysisResult.fileResults;
        if(Iterables.isEmpty(analysisFileResults)) {
            // analysis apparently failed? We have no analyzed ast.
            return new TestCaseExtractionResult(parseResult, analysisResult, Iterables2.<IMessage>empty(),
                Iterables2.<ITestCase>empty());
        }
        final IStrategoTerm ast = analysisResult.fileResults.iterator().next().value();

        // build each test case and gather messages for missing ITestExpectations
        // for now, we will consider these missing evaluators to be an error
        final List<IMessage> extraMessages = new LinkedList<>();
        final List<ITestCase> tests = new ArrayList<>();
        new TermVisitor() {
            @Override public void preVisit(IStrategoTerm term) {
                if(Term.isTermAppl(term)) {
                    if(SPTUtil.TEST_CONS.equals(SPTUtil.consName(term))) {
                        // TODO: this doesn't seem like a proper use of builders
                        // are we allowed to reuse a builder like this?
                        ITestCase test = testBuilder.withTest(term, testSuite).build();
                        tests.add(test);
                        for(ExpectationPair expectation : test.getExpectations()) {
                            if(expectation.evaluator == null) {
                                ISourceRegion region = SPTUtil.getRegion(expectation.expectation);
                                // @formatter:off
                                IMessage m = MessageBuilder.create()
                                    .asError()
                                    .asAnalysis()
                                    .withSource(test.getResource())
                                    .withRegion(region)
                                    .withMessage(
                                        "Unable to evaluate this test expectation. No ITestExpectation found that can handle this.")
                                    .build();
                                // @formatter:on
                                extraMessages.add(m);
                            }
                        }
                    }
                }
            }
        }.visit(ast);

        return new TestCaseExtractionResult(parseResult, analysisResult, extraMessages, tests);
    }
}
