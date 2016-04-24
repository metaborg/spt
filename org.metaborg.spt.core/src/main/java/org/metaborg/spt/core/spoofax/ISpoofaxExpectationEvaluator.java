package org.metaborg.spt.core.spoofax;

import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.IExpectationEvaluator;
import org.metaborg.spt.core.ITestExpectation;

public interface ISpoofaxExpectationEvaluator<E extends ITestExpectation>
    extends IExpectationEvaluator<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, E> {

}
