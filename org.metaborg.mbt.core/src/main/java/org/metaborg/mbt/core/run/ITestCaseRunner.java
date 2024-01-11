package org.metaborg.mbt.core.run;

import jakarta.annotation.Nullable;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.mbt.core.model.ITestCase;

/**
 * Runs individual test cases.
 */
public interface ITestCaseRunner<P extends IParseUnit, A extends IAnalyzeUnit> {

    /**
     * Run the given test.
     * 
     * @param project
     *            the project containing this test.
     * @param test
     *            the test to run.
     * @param languageUnderTest
     *            the language under test to run this test with.
     * @param dialectUnderTest
     *            TODO: I don't know what this is, but it will be used to parse the fragment of the test case. If you
     *            also don't know what it is, just pass null.
     * @param fragmentParseConfig
     *            a configuration parameter for the {@link IFragmentParser} that will be used throughout the test run.
     * @return the test result
     */
    ITestResult<P, A> run(IProject project, ITestCase test, ILanguageImpl languageUnderTest,
                          @Nullable ILanguageImpl dialectUnderTest, @Nullable IFragmentParserConfig fragmentParseConfig);

}
