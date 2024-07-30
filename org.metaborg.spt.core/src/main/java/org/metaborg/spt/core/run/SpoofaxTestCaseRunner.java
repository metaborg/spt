package org.metaborg.spt.core.run;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;

import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.project.IProject;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.TestPhase;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;
import org.metaborg.mbt.core.run.IFragmentParserConfig;
import org.metaborg.mbt.core.run.ITestResult;
import org.metaborg.mbt.core.run.TestCaseRunner;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalysisService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnitUpdate;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;


public class SpoofaxTestCaseRunner
    extends TestCaseRunner<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, ISpoofaxAnalyzeUnitUpdate>
    implements ISpoofaxTestCaseRunner {

    private static final ILogger logger = LoggerUtils.logger(SpoofaxTestCaseRunner.class);

    private final ISpoofaxExpectationEvaluatorService evaluatorService;

    @jakarta.inject.Inject public SpoofaxTestCaseRunner(ISpoofaxAnalysisService analysisService, IContextService contextService,
        ISpoofaxFragmentParser fragmentParser, ISpoofaxExpectationEvaluatorService evaluatorService) {
        super(analysisService, contextService, fragmentParser);
        this.evaluatorService = evaluatorService;
    }

    @Override public ISpoofaxTestResult run(IProject project, ITestCase test, ILanguageImpl languageUnderTest,
        ILanguageImpl dialectUnderTest, IFragmentParserConfig fragmentParseConfig) {
        try {
            // Evaluating Test: statix.test/../.. - unit type ascription
            final String projectName = project.location().getName().getBaseName();
            final String testSuite = test.getResource().getName().getRelativeName(project.location().getName());
            final String message = logger.format("[INFO]  - t.core.run.SpoofaxTestCaseRunner | Evaluating Test: {}/{} - {}",
                    projectName, testSuite, test.getDescription());

            // Ugh - Repeat '*' `message.length()` times
            System.out.println(new String(new char[message.length()]).replace('\0', '*'));
            System.out.println(message);
        } catch(FileSystemException e) {
        }
        ITestResult<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> res =
            super.run(project, test, languageUnderTest, dialectUnderTest, fragmentParseConfig);
        // safe as long as the guarantee of TestCaseRunner.run holds (see the JavaDoc of that method)
        return (ISpoofaxTestResult) res;
    }

    @Override protected ISpoofaxTestResult evaluateExpectations(ITestCase test, ISpoofaxParseUnit parseRes,
        ISpoofaxAnalyzeUnit analysisRes, Iterable<IMessage> analysisMessages, ILanguageImpl languageUnderTest,
        List<IMessage> messages, @Nullable IFragmentParserConfig fragmentParseConfig) {
        boolean success = true;

        List<ISpoofaxTestExpectationOutput> expectationOutputs = new ArrayList<>();
        if(test.getExpectations().isEmpty()) {
            // handle the 'no expectation means parsing must succeed' thing
            success = parseRes.success();
        } else {
            for(ITestExpectation expectation : test.getExpectations()) {
                ISpoofaxExpectationEvaluator<ITestExpectation> evaluator = evaluatorService.lookup(expectation);
                if(evaluator == null) {
                    messages.add(MessageFactory.newAnalysisError(test.getResource(), expectation.region(),
                            "Could not evaluate this expectation. No suitable evaluator was found.", null));
                } else {
                    // TODO: should we really reuse the analysis context for expectation evaluation?
                    // an example is that we reuse it when running a transformation.
                    SpoofaxTestExpectationInput input = new SpoofaxTestExpectationInput(
                            test, languageUnderTest, new SpoofaxFragmentResult(test.getFragment(), parseRes,
                                    analysisRes, analysisMessages, analysisRes == null ? null : analysisRes.context()),
                            fragmentParseConfig);
                    ISpoofaxTestExpectationOutput output = evaluator.evaluate(input, expectation);
                    if(!output.isSuccessful()) {
                        success = false;
                    }
                    expectationOutputs.add(output);
                }
            }
        }

        // ensure there is an error on the test description if the test failed
        if(!success) {
            boolean errorOnName = false;
            for(ISpoofaxTestExpectationOutput output : expectationOutputs) {
                for(IMessage message : output.getMessages()) {
                    if(message.severity() == MessageSeverity.ERROR && message.region() != null
                        && message.region().contains(test.getDescriptionRegion())) {
                        errorOnName = true;
                        break;
                    }
                }
                if(errorOnName) {
                    break;
                }
            }
            if(!errorOnName) {
                messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                    "Test failed, see errors in the fragment.", null));
            }
        }

        return new SpoofaxTestResult(test, success, messages,
            new SpoofaxFragmentResult(test.getFragment(), parseRes, analysisRes, analysisMessages, null), expectationOutputs);
    }

    @Override protected TestPhase requiredPhase(ITestCase test, IContext languageUnderTestCtx) {
        for(ITestExpectation expectation : test.getExpectations()) {
            ISpoofaxExpectationEvaluator<ITestExpectation> evaluator = evaluatorService.lookup(expectation);
            if(evaluator == null) {
                // the error will be generated during evaluation, so we just ignore it here
            } else {
                // TODO: if we get more parse phases, we can't shortcut like this
                TestPhase phase = evaluator.getPhase(languageUnderTestCtx.language(), expectation);
                if(TestPhase.PARSING.ordinal() < phase.ordinal()) {
                    return phase;
                }
            }
        }
        return TestPhase.PARSING;
    }

}
