package org.metaborg.spt.core;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.syntax.IParseUnit;

/**
 * A service to get an Evaluator for a given ITestExpectation
 */
public interface IExpectationEvaluatorService<P extends IParseUnit, A extends IAnalyzeUnit> {

    public <E extends ITestExpectation> IExpectationEvaluator<P, A, E> lookup(E expectation);
}
