package org.metaborg.mbt.core.extract;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.project.IProject;
import org.metaborg.core.syntax.IInputUnit;
import org.metaborg.core.syntax.IParseUnit;

/**
 * Extracts ITestCases from an SPT test suite.
 */
public interface ITestCaseExtractor<I extends IInputUnit, P extends IParseUnit, A extends IAnalyzeUnit> {

    /**
     * Extract the test cases from the given test suite.
     * 
     * Will parse and analyze the given test suite.
     * 
     * @param spt
     *            the language implementation of SPT to use to parse the test suite.
     * @param project
     *            the project that contains this test suite.
     * @param testSuite
     *            the file containing the test suite specification.
     */
    public ITestCaseExtractionResult<P, A> extract(I input, IProject project);

    /**
     * Extract the test cases from the given test suite.
     * 
     * Will analyze the given test suite.
     * 
     * @param spt
     *            the language implementation of SPT to use to parse the test suite.
     * @param project
     *            the project that contains this test suite.
     * @param testSuite
     *            the file containing the test suite specification.
     */
    public ITestCaseExtractionResult<P, A> extract(P input, IProject project);
}
