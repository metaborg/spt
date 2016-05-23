package org.metaborg.mbt.core.extract;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;
import org.metaborg.mbt.core.model.ITestCase;

/**
 * A builder for ITestCases.
 * 
 * @param <T>
 *            the internal SPT representation of a test case.
 * @param <TF>
 *            the internal SPT representation of a test fixture.
 */
public interface ITestCaseBuilder<T, TF> {

    /**
     * Use this test fixture for the test case creation.
     * 
     * @param testFixture
     *            the SPT AST term of the test fixture to use.
     * @return the same builder for chaining calls.
     */
    public ITestCaseBuilder<T, TF> withTestFixture(TF testFixture);

    /**
     * Use this resource for the test case creation.
     * 
     * @param resource
     *            the resource containing the test.
     * @return the same builder for chaining calls.
     */
    public ITestCaseBuilder<T, TF> withResource(FileObject resource);

    /**
     * Use this project for the test case creation.
     * 
     * @param project
     *            the project containing the test.
     * @return the same builder for chaining calls.
     */
    public ITestCaseBuilder<T, TF> withProject(IProject project);

    /**
     * Use this test for the test case creation.
     * 
     * Consecutive calls of this method will simply override eachother.
     * 
     * @param test
     *            the SPT AST term of the test case.
     * @return the same builder for chaining calls.
     */
    public ITestCaseBuilder<T, TF> withTest(T test);

    /**
     * Create the actual test case.
     * 
     * We expect at least one call to withTest prior to calling this method.
     */
    public ITestCase build();
}
