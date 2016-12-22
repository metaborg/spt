package org.metaborg.mbt.core.run;

import java.util.List;

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
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.TestPhase;
import org.metaborg.util.concurrent.IClosableLock;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

public abstract class TestCaseRunner<P extends IParseUnit, A extends IAnalyzeUnit, AU extends IAnalyzeUnitUpdate>
    implements ITestCaseRunner<P, A> {

    private static final ILogger logger = LoggerUtils.logger(TestCaseRunner.class);

    private final IAnalysisService<P, A, AU> analysisService;
    private final IContextService contextService;
    private final IFragmentParser<P> fragmentParser;


    @Inject public TestCaseRunner(IAnalysisService<P, A, AU> analysisService, IContextService contextService,
        IFragmentParser<P> fragmentParser) {
        this.analysisService = analysisService;
        this.contextService = contextService;
        this.fragmentParser = fragmentParser;
    }

    /**
     * Guaranteed to return the result provided by the subclass' implementation of
     * {@link #evaluateExpectations(ITestCase, IParseUnit, IAnalyzeUnit, ILanguageImpl)}.
     */
    @Override public ITestResult<P, A> run(IProject project, ITestCase test, ILanguageImpl languageUnderTest,
        @Nullable ILanguageImpl dialectUnderTest, @Nullable IFragmentParserConfig fragmentParseConfig) {
        logger.debug("About to run test case '{}' with language {}", test.getDescription(), languageUnderTest.id());

        List<IMessage> messages = Lists.newLinkedList();

        // parse the fragment
        final P parseRes;
        try {
            parseRes =
                fragmentParser.parse(test.getFragment(), languageUnderTest, dialectUnderTest, fragmentParseConfig);
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
                try(IClosableLock lock = context.read()) {
                    analysisRes = analysisService.analyze(parseRes, context).result();
                }
            }
        } catch(ContextException | AnalysisException e) {
            if(context != null) {
                context.close();
            }
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Failed to analyze the input fragment, which is required to evaluate some of the test expectations.",
                e));
        }

        // evaluate the test expectations
        final ITestResult<P, A> result =
            evaluateExpectations(test, parseRes, analysisRes, languageUnderTest, messages, fragmentParseConfig);

        // close the analysis context for this test run
        if(context != null) {
            context.close();
        }

        return result;
    }

    /**
     * Evaluate the expectations of the test.
     */
    protected abstract ITestResult<P, A> evaluateExpectations(ITestCase test, P parseRes, A analysisRes,
        ILanguageImpl languageUnderTest, List<IMessage> messages, @Nullable IFragmentParserConfig fragmentParseConfig);

    /**
     * The maximum required phase for this input fragment.
     * 
     * Determines what we do with the input fragment. i.e. whether we just parse it, or also analyze it.
     */
    protected abstract TestPhase requiredPhase(ITestCase test, IContext languageUnderTestCtx);
}
