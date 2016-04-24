package org.metaborg.spt.core;

import javax.annotation.Nullable;

import org.metaborg.core.analysis.AnalysisException;
import org.metaborg.core.analysis.IAnalysisService;
import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.analysis.IAnalyzeUnitUpdate;
import org.metaborg.core.context.ContextException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.context.ITemporaryContext;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.project.IProject;
import org.metaborg.core.syntax.IInputUnit;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.Inject;

public abstract class TestCaseRunner<I extends IInputUnit, P extends IParseUnit, A extends IAnalyzeUnit, AU extends IAnalyzeUnitUpdate>
    implements ITestCaseRunner {

    private static final ILogger logger = LoggerUtils.logger(TestCaseRunner.class);

    private final IAnalysisService<P, A, AU> analysisService;
    private final IContextService contextService;
    private final IFragmentParser<I, P> fragmentParser;


    @Inject public TestCaseRunner(IAnalysisService<P, A, AU> analysisService, IContextService contextService,
        IFragmentParser<I, P> fragmentParser) {
        this.analysisService = analysisService;
        this.contextService = contextService;
        this.fragmentParser = fragmentParser;
    }

    @Override public ITestResult run(IProject project, ITestCase test, ILanguageImpl languageUnderTest,
        @Nullable ILanguageImpl dialectUnderTest) {
        logger.debug("About to run test case '{}' with language {}", test.getDescription(), languageUnderTest.id());

        // parse the fragment
        final P parseRes;
        try {
            parseRes = fragmentParser.parse(test.getFragment(), languageUnderTest, dialectUnderTest);
        } catch(ParseException e) {
            // TODO: is this ok? or should we fail the test and gracefully return a message?
            throw new RuntimeException(e);
        }

        // analyze the fragment if any expectation requires analysis
        A analysisRes = null;
        ITemporaryContext context = null;
        try {
            context = contextService.getTemporary(test.getResource(), project, languageUnderTest);
            TestPhase phase = requiredPhase(test, context);
            if(phase.ordinal() > TestPhase.PARSING.ordinal()) {
                analysisRes = analysisService.analyze(parseRes, context).result();
            }
        } catch(ContextException | AnalysisException e) {
            if(context != null) {
                context.close();
            }
            return new TestResult(false,
                Iterables2.<IMessage>singleton(
                    MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                        "Failed to analyze the input fragment, which is required to evaluate some of the test expectations.",
                        e)),
                Iterables2.<ITestExpectationOutput>empty());
        }

        // evaluate the test expectations
        final ITestResult result = evaluateExpectations(test, parseRes, analysisRes, languageUnderTest);

        // close the analysis context for this test run
        if(context != null) {
            context.close();
        }

        return result;
    }

    /**
     * Evaluate the expectations of the test.
     */
    protected abstract ITestResult evaluateExpectations(ITestCase test, P parseRes, A analysisRes,
        ILanguageImpl languageUnderTest);

    /**
     * The maximum required phase for this input fragment.
     * 
     * Determines what we do with the input fragment. i.e. whether we just parse it, or also analyze it.
     */
    protected abstract TestPhase requiredPhase(ITestCase test, IContext languageUnderTestCtx);
}
