package org.metaborg.spt.core;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;
import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * A builder for ITestCases.
 */
public interface ITestCaseBuilder {

    /**
     * Use this test fixture for the test case creation.
     * 
     * @param testFixture
     *            the SPT AST term of the test fixture to use.
     * @return the same builder for chaining calls.
     */
    public ITestCaseBuilder withTestFixture(IStrategoTerm testFixture);

    /**
     * Use this resource for the test case creation.
     * 
     * @param resource
     *            the resource containing the test.
     * @return the same builder for chaining calls.
     */
    public ITestCaseBuilder withResource(FileObject resource);

    /**
     * Use this project for the test case creation.
     * 
     * @param project
     *            the project containing the test.
     * @return the same builder for chaining calls.
     */
    public ITestCaseBuilder withProject(IProject project);

    /**
     * Use this test for the test case creation.
     * 
     * Consecutive calls of this method will simply override eachother.
     * 
     * @param test
     *            the SPT AST term of the test case.
     * @return the same builder for chaining calls.
     */
    public ITestCaseBuilder withTest(IStrategoTerm test);

    /**
     * Create the actual test case.
     * 
     * We expect at least one call to withTest prior to calling this method.
     */
    public ITestCase build();
}
