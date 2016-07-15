package org.metaborg.mbt.core.extract;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;
import org.metaborg.mbt.core.model.IFragment;

/**
 * A builder for IFragments.
 * 
 * @param <F>
 *            the internal SPT representation of a fragment.
 * @param <TF>
 *            the internal SPT representation of a test fixture.
 */
public interface IFragmentBuilder<F, TF> {

    /**
     * Use this fixture for the fragment creation.
     * 
     * @param fragmentFixture
     *            the SPT AST term of the test fixture to use.
     * @return the same builder for chaining calls.
     */
    public IFragmentBuilder<F, TF> withFixture(TF fragmentFixture);

    /**
     * Use this resource as the source from which this fragment was created.
     * 
     * @param resource
     *            the source of the fragment (usually an SPT test suite file).
     * @return the same builder for chaining calls.
     */
    public IFragmentBuilder<F, TF> withResource(FileObject resource);

    /**
     * Use this project as the project from within which this fragment was created.
     * 
     * @param project
     *            the project.
     * @return the same builder for chaining calls.
     */
    public IFragmentBuilder<F, TF> withProject(IProject project);

    /**
     * Use this fragment term to create an IFragment.
     * 
     * Consecutive calls of this method will simply override each other.
     * 
     * @param fragment
     *            the SPT AST term of the fragment.
     * @return the same builder for chaining calls.
     */
    public IFragmentBuilder<F, TF> withFragment(F fragment);

    /**
     * Create the actual fragment.
     * 
     * We expect at least one call to withFragmet prior to calling this method.
     */
    public IFragment build();
}
