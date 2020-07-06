package org.metaborg.mbt.core.run;

import java.util.Collection;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.TestPhase;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;

/**
 * An IExpectationEvaluator evaluates a specific ITestExpectation, of class E.
 * 
 * @param <P>
 *            the type of the parse unit for the language under test.
 * @param <A>
 *            the type of the analyze unit for the language under test.
 * @param <E>
 *            the specific subclass of ITestExpectation, that we know how to handle.
 */
public interface ITestExpectationEvaluator<P extends IParseUnit, A extends IAnalyzeUnit, E extends ITestExpectation> {

    /**
     * Returns the indexes of the selections that will be used to evaluate the expectation.
     * 
     * @param fragment
     *            the input fragment.
     * @return the indexes (starting at 0) of the selections we utilize.
     */
    Collection<Integer> usesSelections(IFragment fragment, E expectation);

    /**
     * The phase that is required to evaluate the expectation.
     * 
     * It is used to determine what we should do with the input fragment (e.g., parse or analyze it).
     * 
     * @param language
     *            the context of the language under test for which you want to evaluate this expectation.
     */
    TestPhase getPhase(ILanguageImpl language, E expectation);

    /**
     * Evaluate the test expectation for the given input.
     */
    ITestExpectationOutput<P, A> evaluate(ITestExpectationInput<P, A> input, E expectation);
}
