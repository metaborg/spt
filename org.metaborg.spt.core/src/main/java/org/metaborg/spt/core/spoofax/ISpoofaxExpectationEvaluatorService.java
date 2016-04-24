package org.metaborg.spt.core.spoofax;

import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.IExpectationEvaluatorService;
import org.metaborg.spt.core.ITestExpectation;

public interface ISpoofaxExpectationEvaluatorService
    extends IExpectationEvaluatorService<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> {
    @Override <E extends ITestExpectation> ISpoofaxExpectationEvaluator<E> lookup(E expectation);
}
