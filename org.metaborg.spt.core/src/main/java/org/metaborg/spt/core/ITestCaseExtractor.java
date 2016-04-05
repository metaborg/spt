package org.metaborg.spt.core;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;

/**
 * Extracts ITestCases from an SPT test suite.
 */
public interface ITestCaseExtractor {

    /**
     * Extract the test cases from the given test suite.
     * 
     * Requires the parsing and analysis of the given test suite.
     * 
     * @param spt
     *            the language implementation of SPT to use to parse the test suite.
     * @param project
     *            the project that contains this test suite.
     * @param testSuite
     *            the file containing the test suite specification.
     */
    public ITestCaseExtractionResult extract(ILanguageImpl spt, IProject project, final FileObject testSuite);
}
