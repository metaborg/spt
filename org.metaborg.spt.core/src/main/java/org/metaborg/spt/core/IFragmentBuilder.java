package org.metaborg.spt.core;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;
import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * A builder for IFragments.
 */
public interface IFragmentBuilder {

    /**
     * Use this fixture for the fragment creation.
     * 
     * @param fragmentFixture
     *            the SPT AST term of the test fixture to use.
     * @return the same builder for chaining calls.
     */
    public IFragmentBuilder withFixture(IStrategoTerm fragmentFixture);

    /**
     * Use this resource as the source from which this fragment was created.
     * 
     * @param resource
     *            the source of the fragment (usually an SPT test suite file).
     * @return the same builder for chaining calls.
     */
    public IFragmentBuilder withResource(FileObject resource);

    /**
     * Use this project as the project from within which this fragment was created.
     * 
     * @param project
     *            the project.
     * @return the same builder for chaining calls.
     */
    public IFragmentBuilder withProject(IProject project);

    /**
     * Use this fragment term to create an IFragment.
     * 
     * Consecutive calls of this method will simply override each other.
     * 
     * @param fragment
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
