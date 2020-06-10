package org.metaborg.spt.core.extract;

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
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;
import org.metaborg.mbt.core.model.expectations.NoExpectationError;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalysisService;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.SPTUtil;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.TermVisitor;

import com.google.inject.Inject;
import org.spoofax.terms.util.TermUtils;

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
            return new SpoofaxTestCaseExtractionResult("", null, null, null,
                Iterables2.singleton(MessageBuilder.create()
                    // @formatter:off
                    .asInternal()
                    .asError()
                    .withMessage("Can't extract a test without a source FileObject.")
                    .build()
                    // @formatter:on
                ), Iterables2.empty());
        }

        final ISpoofaxParseUnit p;
        try {
            p = parseService.parse(input);
            if(!p.valid()) {
                // parse failed and couldn't recover
                return new SpoofaxTestCaseExtractionResult(testSuite.getName().getBaseName(), null, p, null,
                    Iterables2.empty(), Iterables2.empty());
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
            return new SpoofaxTestCaseExtractionResult(testSuite.getName().getBaseName(), null, null, null,
                Iterables2.singleton(error), Iterables2.empty());
        }

        return extract(p, project);
    }

    @Override public ISpoofaxTestCaseExtractionResult extract(ISpoofaxParseUnit p, final IProject project) {

        final FileObject testSuite = p.input().source();
        if(testSuite == null) {
            return new SpoofaxTestCaseExtractionResult("", null, null, null,
                Iterables2.singleton(MessageBuilder.create()
                    // @formatter:off
                    .asInternal()
                    .asError()
                    .withMessage("Can't extract a test without a source FileObject.")
                    .build()
                    // @formatter:on
                ), Iterables2.empty());
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
            return new SpoofaxTestCaseExtractionResult(testSuite.getName().getBaseName(), null, p, null,
                Iterables2.singleton(error), Iterables2.empty());
        }

        // Retrieve the AST from the analysis result
        if(a == null || !a.valid() || !a.hasAst()) {
            return new SpoofaxTestCaseExtractionResult(testSuite.getName().getBaseName(), null, p, a,
                Iterables2.singleton(MessageBuilder.create()
                    // @formatter:off
                    .asInternal()
                    .asError()
                    .withSource(testSuite)
                    .withMessage("The analysis of SPT did not return an AST.")
                    .build()
                    // @formatter:on
                ), Iterables2.empty());
        }
        final IStrategoTerm ast = a.ast();

        // build each test case and gather messages for missing ITestExpectations
        // for now, we will consider these missing expectations to be an error
        final List<IMessage> extraMessages = new LinkedList<>();
        final List<ITestCase> tests = new ArrayList<>();
        final List<String> suiteNameContainer = new ArrayList<>();
        final List<String> langNameContainer = new ArrayList<>();
        final List<String> startSymbolContainer = new ArrayList<>();
        new TermVisitor() {
            IStrategoTerm fixtureTerm = null;

            @Override public void preVisit(IStrategoTerm term) {
                if(TermUtils.isAppl(term)) {
                    final String cons = SPTUtil.consName(term);
                    if(SPTUtil.START_SYMBOL_CONS.equals(cons)) {
                        startSymbolContainer.add(TermUtils.toJavaString(term.getSubterm(0)));
                    } else if(SPTUtil.LANG_CONS.equals(cons)) {
                        langNameContainer.add(TermUtils.toJavaString(term.getSubterm(0)));
                    } else if(SPTUtil.NAME_CONS.equals(cons)) {
                        suiteNameContainer.add(TermUtils.toJavaString(term.getSubterm(0)));
                    } else if(SPTUtil.FIXTURE_CONS.equals(cons)) {
                        fixtureTerm = term;
                        logger.debug("Using test fixture: {}", fixtureTerm);
                    } else if(SPTUtil.TEST_CONS.equals(cons)) {
                        // TODO: this doesn't seem like a proper use of builders
                        // are we allowed to reuse a builder like this?
                        ITestCase test = testBuilder.withProject(project).withResource(testSuite)
                            .withTestFixture(fixtureTerm).withTest(term).build();
                        tests.add(test);
                        for(ITestExpectation expectation : test.getExpectations()) {
                            // TODO: not a very good way of error reporting, but it works for now
                            // also see SpoofaxTestCaseBuilder.build()
                            if(expectation instanceof NoExpectationError) {
                                ISourceRegion region = expectation.region();
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

        if(startSymbolContainer.size() > 1) {
         // @formatter:off
            IMessage m = MessageBuilder.create()
                .asAnalysis()
                .asWarning()
                .withSource(testSuite)
                .withMessage(
                    "Found more than 1 start symbol. We will just use the first one. " + startSymbolContainer)
                .build();
            // @formatter:on
            extraMessages.add(m);
        }
        final String startSymbol = startSymbolContainer.isEmpty() ? null : startSymbolContainer.get(0);

        if(suiteNameContainer.size() > 1) {
            // @formatter:off
            IMessage m = MessageBuilder.create()
                .asAnalysis()
                .asWarning()
                .withSource(testSuite)
                .withMessage(
                    "Found more than 1 module name. We will just use the first one. " + suiteNameContainer)
                .build();
            // @formatter:on
            extraMessages.add(m);
        }
        if(suiteNameContainer.isEmpty()) {
            return new SpoofaxTestCaseExtractionResult(testSuite.getName().getBaseName(), null, p, a,
                Iterables2.singleton(MessageBuilder.create()
                    // @formatter:off
                    .asInternal()
                    .asError()
                    .withSource(testSuite)
                    .withMessage("Found no module name. The test suite should have a name.")
                    .build()
                    // @formatter:on
                ), Iterables2.empty());
        }
        final String suiteName = suiteNameContainer.get(0);

        if(langNameContainer.size() > 1) {
            // @formatter:off
            IMessage m = MessageBuilder.create()
                .asAnalysis()
                .asWarning()
                .withSource(testSuite)
                .withMessage(
                    "Found more than 1 language under test. We will just use the first one. " + langNameContainer)
                .build();
            // @formatter:on
            extraMessages.add(m);
        }
        if(langNameContainer.isEmpty()) {
            return new SpoofaxTestCaseExtractionResult(testSuite.getName().getBaseName(), null, p, a,
                Iterables2.singleton(MessageBuilder.create()
                    // @formatter:off
                    .asInternal()
                    .asError()
                    .withSource(testSuite)
                    .withMessage("Found no language header. The test suite should have a header for the language under test.")
                    .build()
                    // @formatter:on
                ), Iterables2.empty());
        }
        final String langName = langNameContainer.get(0);

        return new SpoofaxTestCaseExtractionResult(suiteName, langName, p, a, extraMessages, tests, startSymbol);
    }
}
