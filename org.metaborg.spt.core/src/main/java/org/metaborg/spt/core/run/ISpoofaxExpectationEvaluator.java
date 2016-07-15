package org.metaborg.spt.core.run;

import org.metaborg.mbt.core.model.expectations.ITestExpectation;
import org.metaborg.mbt.core.run.ITestExpectationEvaluator;
import org.metaborg.mbt.core.run.ITestExpectationInput;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

/**
 * Type interface for an ITestExpectationEvaluator that can evaluate an input from a test that is being run on a Spoofax
 * language.
 *
 * @param <E>
 *            the type of ITestExpectation that this evaluator can handle.
 */
public interface ISpoofaxExpectationEvaluator<E extends ITestExpectation>
    extends ITestExpectationEvaluator<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, E> {

    @Override ISpoofaxTestExpectationOutput
        evaluate(ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input, E expectation);
}
