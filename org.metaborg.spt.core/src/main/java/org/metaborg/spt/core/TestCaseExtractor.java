package org.metaborg.spt.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.analysis.AnalysisException;
import org.metaborg.core.context.ContextException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageBuilder;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalysisService;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnitService;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.ITestCase.ExpectationPair;
import org.metaborg.spt.core.util.SPTUtil;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;
import org.spoofax.terms.TermVisitor;

import com.google.inject.Inject;

public class TestCaseExtractor implements ITestCaseExtractor {

    private static final ILogger logger = LoggerUtils.logger(TestCaseExtractor.class);

    private final ISpoofaxTracingService traceService;
    private final ISpoofaxInputUnitService inputService;
    private final ISpoofaxSyntaxService parseService;
    private final ISpoofaxAnalysisService analysisService;
    private final IContextService contextService;
    private final ITestCaseBuilder testBuilder;

    @Inject public TestCaseExtractor(ISpoofaxTracingService traceService, ISpoofaxInputUnitService inputService,
        ISpoofaxSyntaxService parseService, ISpoofaxAnalysisService analysisService, IContextService contextService,
        ITestCaseBuilder builder) {
        this.traceService = traceService;
        this.inputService = inputService;
        this.parseService = parseService;
        this.analysisService = analysisService;
        this.contextService = contextService;
        this.testBuilder = builder;
    }

    @Override public ITestCaseExtractionResult extract(ILanguageImpl spt, final IProject project,
        final FileObject testSuite) {
        InputStream in;
        final ISpoofaxParseUnit parseResult;
        final ISpoofaxAnalyzeUnit analysisResult;
        try {
            in = testSuite.getContent().getInputStream();
            String text = IOUtils.toString(in);
            in.close();
            // TODO: do we need a dialect for SPT?
            parseResult = parseService.parse(inputService.inputUnit(testSuite, text, spt, null));
            if(!parseResult.valid()) {
                // parse failed and couldn't recover
                return new TestCaseExtractionResult(parseResult, null, Iterables2.<IMessage>empty(),
                    Iterables2.<ITestCase>empty());
            }
        } catch(IOException ioe) {
            // @formatter:off
            IMessage error = MessageBuilder.create()
                .asInternal()
                .asError()
                .withException(ioe)
                .withSource(testSuite)
                .withMessage("Failed to read the testsuite " + testSuite.getName().getBaseName())
                .build();
            // @formatter:on
            return new TestCaseExtractionResult(null, null, Iterables2.singleton(error), Iterables2.<ITestCase>empty());
        } catch(ParseException pe) {
            // @formatter:off
            IMessage error = MessageBuilder.create()
                .asParser()
                .asError()
                .withException(pe)
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
            analysisResult = analysisService.analyze(parseResult, ctx).result();
        } catch(ContextException | AnalysisException ae) {
            // @formatter:off
            IMessage error = MessageBuilder.create()
                .asAnalysis()
                .asError()
                .withException(ae)
                .withSource(testSuite)
                .withMessage(ae.getMessage())
                .build();
            // @formatter:on
            return new TestCaseExtractionResult(parseResult, null, Iterables2.singleton(error),
                Iterables2.<ITestCase>empty());
        }

        // Retrieve the AST from the analysis result
        if(!analysisResult.valid() || !analysisResult.hasAst()) {
            return new TestCaseExtractionResult(parseResult, analysisResult,
                Iterables2.singleton(MessageBuilder.create()
                    // @formatter:off
                    .asInternal()
                    .asError()
                    .withSource(testSuite)
                    .withMessage("The analysis of SPT did not return an AST.")
                    .build()
                    // @formatter:on
            ), Iterables2.<ITestCase>empty());
        }
        final IStrategoTerm ast = analysisResult.ast();

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
                        ITestCase test =
                            testBuilder.withProject(project).withResource(testSuite).withTest(term).build();
                        tests.add(test);
                        for(ExpectationPair expectation : test.getExpectations()) {
                            if(expectation.evaluator == null) {
                                logger.debug("No evaluator found for " + expectation.expectation);
                                ISourceLocation loc = traceService.location(expectation.expectation);
                                final ISourceRegion region;
                                if(loc == null) {
                                    region = test.getDescriptionRegion();
                                } else {
                                    region = loc.region();
                                }
                                // @formatter:off
                                IMessage m = MessageBuilder.create()
                                    .asAnalysis()
                                    .asError()
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
