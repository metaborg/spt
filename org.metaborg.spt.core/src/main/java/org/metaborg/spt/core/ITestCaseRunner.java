package org.metaborg.spt.core;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;

/**
 * Runs individual test cases.
 */
public interface ITestCaseRunner {

    /**
     * Run the given test.
     * 
     * @param project
     *            the project containing this test.
     * @param test
     *            the test to run.
     * @param languageUnderTest
     *            the language under test to run this test with.
     * @return
     */
    public ITestResult run(IProject project, ITestCase test, ILanguageImpl languageUnderTest);

}
