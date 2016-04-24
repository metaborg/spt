package org.metaborg.spt.core.spoofax;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.analysis.AnalysisException;
import org.metaborg.core.context.ContextException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageBuilder;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalysisService;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.ITestCase;
import org.metaborg.spt.core.ITestExpectation;
import org.metaborg.spt.core.expectations.NoExpectationError;
import org.metaborg.spt.core.util.SPTUtil;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;
import org.spoofax.terms.TermVisitor;

import com.google.inject.Inject;

public class SpoofaxTestCaseExtractor implements ISpoofaxTestCaseExtractor {

    private static final ILogger logger = LoggerUtils.logger(SpoofaxTestCaseExtractor.class);

    private final ISpoofaxSyntaxService parseService;
    private final ISpoofaxAnalysisService analysisService;
    private final IContextService contextService;
    private final ISpoofaxTestCaseBuilder testBuilder;

    @Inject public SpoofaxTestCaseExtractor(ISpoofaxSyntaxService parseService, ISpoofaxAnalysisService analysisService,
        IContextService contextService, ISpoofaxTestCaseBuilder builder) {
        this.parseService = parseService;
        this.analysisService = analysisService;
        this.contextService = contextService;
        this.testBuilder = builder;
    }

    @Override public ISpoofaxTestCaseExtractionResult extract(ISpoofaxInputUnit input, IProject project) {
        final FileObject testSuite = input.source();
        if(testSuite == null) {
            return new SpoofaxTestCaseExtractionResult(null, null,
                Iterables2.singleton(MessageBuilder.create()
                    // @formatter:off
                    .asInternal()
                    .asError()
                    .withMessage("Can't extract a test without a source FileObject.")
                    .build()
                    // @formatter:on
                ), Iterables2.<ITestCase>empty());
        }

        final ISpoofaxParseUnit p;
        try {
            p = parseService.parse(input);
            if(!p.valid()) {
                // parse failed and couldn't recover
                return new SpoofaxTestCaseExtractionResult(p, null, Iterables2.<IMessage>empty(),
                    Iterables2.<ITestCase>empty());
            }
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
            return new SpoofaxTestCaseExtractionResult(null, null, Iterables2.singleton(error),
                Iterables2.<ITestCase>empty());
        }

        return extract(p, project);
    }

    @Override public ISpoofaxTestCaseExtractionResult extract(ISpoofaxParseUnit p, final IProject project) {

        final FileObject testSuite = p.input().source();
        if(testSuite == null) {
            return new SpoofaxTestCaseExtractionResult(null, null,
                Iterables2.singleton(MessageBuilder.create()
                    // @formatter:off
                    .asInternal()
                    .asError()
                    .withMessage("Can't extract a test without a source FileObject.")
                    .build()
                    // @formatter:on
                ), Iterables2.<ITestCase>empty());
        }

        final ISpoofaxAnalyzeUnit a;
        try {
            // even if parsing fails we can still analyze
            // the result will just be empty
            IContext ctx = contextService.get(testSuite, project, p.input().langImpl());
            a = analysisService.analyze(p, ctx).result();
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
            return new SpoofaxTestCaseExtractionResult(p, null, Iterables2.singleton(error),
                Iterables2.<ITestCase>empty());
        }

        // Retrieve the AST from the analysis result
        if(a == null || !a.valid() || !a.hasAst()) {
            return new SpoofaxTestCaseExtractionResult(p, a,
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
        final IStrategoTerm ast = a.ast();

        // build each test case and gather messages for missing ITestExpectations
        // for now, we will consider these missing expectations to be an error
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
                        for(ITestExpectation expectation : test.getExpectations()) {
                            // TODO: not a very good way of error reporting, but it works for now
                            // also see SpoofaxTestCaseBuilder.build()
                            if(expectation instanceof NoExpectationError) {
                                ISourceRegion region = ((NoExpectationError) expectation).region();
                                logger.debug("No evaluator found for the expectation at ({}, {});",
                                    region.startOffset(), region.endOffset());
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

        return new SpoofaxTestCaseExtractionResult(p, a, extraMessages, tests);
    }
}
