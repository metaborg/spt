package org.metaborg.mbt.core.run;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.mbt.core.model.ITestCase;

/**
 * Input for evaluation of a test expectation.
 *
 * @param <P>
 *            the type of the parse result of the language under test.
 * @param <A>
 *            the type of the analysis result of the language under test.
 * 
 */
public interface ITestExpectationInput<P extends IParseUnit, A extends IAnalyzeUnit> {

    /**
     * The test case for which we should evaluate the expectation.
     */
    ITestCase getTestCase();

    /**
     * The language implementation for the language under test that is used to run this test case.
     */
    ILanguageImpl getLanguageUnderTest();

    /**
     * The result of either parsing, or parsing and analyzing the fragment.
     */
    IFragmentResult<P, A> getFragmentResult();

    /**
     * The configuration parameter to use during this test run for the parsing of fragments.
     */
    IFragmentParserConfig getFragmentParserConfig();

}
