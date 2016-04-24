package org.metaborg.spt.core;

import javax.annotation.Nullable;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.IParseUnit;

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
    public ITestCase getTestCase();

    /**
     * The language implementation for the language under test that is used to run this test case.
     */
    public ILanguageImpl getLanguageUnderTest();

    /**
     * The result of parsing the fragment with the language under test.
     */
    public P getParseResult();

    /**
     * The result of analyzing the fragment with the language under test.
     * 
     * May be null if the expectation only requires the {@link TestPhase#PARSING} phase, or if the parsing failed.
     */
    public @Nullable A getAnalysisResult();

    /**
     * The context that was used to analyze the input fragment.
     * 
     * May be null.
     */
    public @Nullable IContext getContext();

}
