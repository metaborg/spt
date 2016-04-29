package org.metaborg.spt.core.spoofax;

import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.IExpectationEvaluator;
import org.metaborg.spt.core.ITestExpectation;
import org.metaborg.spt.core.ITestExpectationInput;

public interface ISpoofaxExpectationEvaluator<E extends ITestExpectation>
    extends IExpectationEvaluator<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, E> {

    @Override ISpoofaxTestExpectationOutput
        evaluate(ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input, E expectation);
}
