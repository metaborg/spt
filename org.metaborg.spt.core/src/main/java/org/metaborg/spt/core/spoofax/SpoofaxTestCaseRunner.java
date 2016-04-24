package org.metaborg.spt.core.spoofax;

import java.util.ArrayList;
import java.util.List;

import org.metaborg.core.context.IContext;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalysisService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnitUpdate;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.ITestCase;
import org.metaborg.spt.core.ITestExpectation;
import org.metaborg.spt.core.ITestExpectationOutput;
import org.metaborg.spt.core.TestCaseRunner;
import org.metaborg.spt.core.TestPhase;
import org.metaborg.spt.core.TestResult;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class SpoofaxTestCaseRunner
    extends TestCaseRunner<ISpoofaxInputUnit, ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, ISpoofaxAnalyzeUnitUpdate> {

    private final ISpoofaxExpectationEvaluatorService evaluatorService;

    @Inject public SpoofaxTestCaseRunner(ISpoofaxAnalysisService analysisService, IContextService contextService,
        ISpoofaxFragmentParser fragmentParser, ISpoofaxExpectationEvaluatorService evaluatorService) {
        super(analysisService, contextService, fragmentParser);
        this.evaluatorService = evaluatorService;
    }

    @Override protected TestResult evaluateExpectations(ITestCase test, ISpoofaxParseUnit parseRes,
        ISpoofaxAnalyzeUnit analysisRes, ILanguageImpl languageUnderTest) {
        boolean success = true;
        List<IMessage> messages = Lists.newLinkedList();

        List<ITestExpectationOutput> expectationOutputs = new ArrayList<>();
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
                    SpoofaxTestExpectationInput input = new SpoofaxTestExpectationInput(test, languageUnderTest,
                        parseRes, analysisRes, analysisRes == null ? null : analysisRes.context());
                    ITestExpectationOutput output = evaluator.evaluate(input, expectation);
                    if(!output.isSuccessful()) {
                        success = false;
                    }
                    expectationOutputs.add(output);
                }
            }
        }
        return new TestResult(success, messages, expectationOutputs);
    }

    @Override protected TestPhase requiredPhase(ITestCase test, IContext languageUnderTestCtx) {
        for(ITestExpectation expectation : test.getExpectations()) {
            ISpoofaxExpectationEvaluator<ITestExpectation> evaluator = evaluatorService.lookup(expectation);
            if(evaluator == null) {
                // the error will be generated during evaluation, so we just ignore it here
            } else {
                // TODO: if we get more parse phases, we can't shortcut like this
                TestPhase phase = evaluator.getPhase(languageUnderTestCtx, expectation);
                if(TestPhase.PARSING.ordinal() < phase.ordinal()) {
                    return phase;
                }
            }
        }
        return TestPhase.PARSING;
    }

}
