package org.metaborg.spt.core.run;

import org.metaborg.mbt.core.model.expectations.ITestExpectation;
import org.metaborg.mbt.core.run.IExpectationEvaluatorService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

/**
 * Type interface for an IExpectationEvaluatorService that can look up ITestExpectationEvaluators for Spoofax languages.
 */
public interface ISpoofaxExpectationEvaluatorService
    extends IExpectationEvaluatorService<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> {
    @Override <E extends ITestExpectation> ISpoofaxExpectationEvaluator<E> lookup(E expectation);
}
