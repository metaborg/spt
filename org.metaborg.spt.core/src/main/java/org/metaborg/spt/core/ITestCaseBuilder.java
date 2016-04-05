package org.metaborg.spt.core;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;
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
     * Use this test for the test case creation.
     * 
     * Consecutive calls of this method will simply override eachother.
     * 
     * @param test
     *            the SPT AST term of the test case.
     * @param suiteFile
     *            the source file to which this test belongs.
     * @return the same builder for chaining calls.
     */
    public ITestCaseBuilder withTest(IStrategoTerm test, @Nullable FileObject suiteFile);

    /**
     * Create the actual test case.
     * 
     * We expect at least one call to withTest prior to calling this method.
     */
    public ITestCase build();
}
