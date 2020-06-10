package org.metaborg.mbt.core.extract;

import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;

/**
 * SPT has test expectations represented in the SPT language's internal AST format (IStrategoTerm for Spoofax).
 * 
 * Registered ITestExpectationProviders are asked during test case extraction if they recognize an expectation. If they
 * do, they return an ITestExpectation.
 * 
 * During test execution, we will query the registered IExpectationEvaluators to find one that can evaluate the
 * ITestExpectation.
 * 
 * Registration is done using the Guice MultiBinder.newSetBinder. The first expectation provider that claims to
 * {@link #canEvaluate)} the AST node will be assigned to handle it.
 * 
 * @param <I>
 *            the type of the internal AST representation of the expectation.
 */
public interface ITestExpectationProvider<I> {

    /**
     * Returns true if this implementation can create an evaluator for the given expectation node.
     * 
     * @param inputFragment
     *            the input fragment of the test case that this expectation is on.
     * @param expectationTerm
     *            the AST term of the expectation.
     */
    boolean canEvaluate(IFragment inputFragment, I expectationTerm);

    /**
     * Creates the evaluator that will evaluate the expectation at test runtime.
     * 
     * @param inputFragment
     *            the input fragment of the test case that this expectation is on.
     * @param expectationTerm
     *            the AST term of the expectation.
     */
    ITestExpectation createExpectation(IFragment inputFragment, I expectationTerm);
}
