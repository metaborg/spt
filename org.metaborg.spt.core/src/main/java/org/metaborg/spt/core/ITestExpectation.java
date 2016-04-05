package org.metaborg.spt.core;

import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * An implementation of ITestExpectation can support one or more SPT test expectations.
 * 
 * The expectation should be registered using the Guice MultiBinder.newSetBinder. The first expectation that claims to
 * {@link #canEvaluate(IStrategoTerm)} the expectation will be assigned to evaluate it.
 */
public interface ITestExpectation {

    /**
     * Returns true if this implementation can evaluate the given expectation term.
     * 
     * @param expectationTerm
     *            the AST term of the expectation.
     */
    public boolean canEvaluate(IStrategoTerm expectationTerm);

    /**
     * Returns the phase of test execution that is required to evaluate this expectation.
     * 
     * This method will only be called if {@link #canEvaluate(IStrategoTerm)} returned true.
     * 
     * @param expectationTerm
     *            the AST term of the expectation.
     */
    public TestPhase getPhase(IStrategoTerm expectationTerm);

    /**
     * Evaluate the expectation.
     */
    public ITestExpectationOutput evaluate(ITestExpectationInput<IStrategoTerm, IStrategoTerm> input);
}
