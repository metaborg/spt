package org.metaborg.spt.core.run.expectations;

import org.metaborg.mbt.core.model.expectations.NotExpectation;
import org.metaborg.mbt.core.run.IExpectationEvaluatorService;
import org.metaborg.mbt.core.run.ITestExpectationInput;
import org.metaborg.mbt.core.run.ITestExpectationOutput;
import org.metaborg.mbt.core.run.expectations.NotExpectationEvaluator;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.run.ISpoofaxExpectationEvaluator;
import org.metaborg.spt.core.run.ISpoofaxTestExpectationOutput;
import org.metaborg.spt.core.run.SpoofaxTestExpectationOutput;

import com.google.inject.Inject;

/**
 * Typedef class for {@link NotExpectationEvaluator} with {@link ISpoofaxExpectationEvaluator}.
 */
public class SpoofaxNotExpectationEvaluator extends NotExpectationEvaluator<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit>
    implements ISpoofaxExpectationEvaluator<NotExpectation> {
    @Inject public SpoofaxNotExpectationEvaluator(
        IExpectationEvaluatorService<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> evaluatorService) {
        super(evaluatorService);
    }


    @Override public ISpoofaxTestExpectationOutput
        evaluate(ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input, NotExpectation expectation) {
        final ITestExpectationOutput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> result =
            super.evaluate(input, expectation);
        return new SpoofaxTestExpectationOutput(result);
    }
}
