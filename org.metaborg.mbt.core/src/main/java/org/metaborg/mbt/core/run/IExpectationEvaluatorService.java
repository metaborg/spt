package org.metaborg.mbt.core.run;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;

/**
 * A service to get an ITestExpectationEvaluator for a given ITestExpectation.
 * 
 * As the ITestExpectationEvaluator is parameterized for a certain type of language (e.g. Spoofax languages), this
 * service has to be implemented for the type of language you want to test.
 */
public interface IExpectationEvaluatorService<P extends IParseUnit, A extends IAnalyzeUnit> {

    public <E extends ITestExpectation> ITestExpectationEvaluator<P, A, E> lookup(E expectation);
}
