package org.metaborg.spt.core;

import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * A builder for IFragments.
 */
public interface IFragmentBuilder {

    /**
     * Use this test fixture for the fragment creation.
     * 
     * @param testFixture
     *            the SPT AST term of the test fixture to use.
     * @return the same builder for chaining calls.
     */
    public IFragmentBuilder withTestFixture(IStrategoTerm fragmentFixture);

    /**
     * Use this fragment term to create an IFragment.
     * 
     * Consecutive calls of this method will simply override each other.
     * 
     * @param test
     *            the SPT AST term of the fragment.
     * @return the same builder for chaining calls.
     */
    public IFragmentBuilder withFragment(IStrategoTerm fragment);

    /**
     * Create the actual fragment.
     * 
     * We expect at least one call to withFragmet prior to calling this method.
     */
    public IFragment build();
}
