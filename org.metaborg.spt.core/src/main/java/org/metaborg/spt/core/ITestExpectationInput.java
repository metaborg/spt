package org.metaborg.spt.core;

import javax.annotation.Nullable;

import org.metaborg.core.analysis.AnalysisResult;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.ParseResult;
import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * Input for evaluation of a test expectation.
 *
 * @param
 *            <P>
 *            the type of the parse result of the language under test.
 * @param <A>
 *            the type of the analysis result of the language under test.
 */
public interface ITestExpectationInput<P, A> {

    /**
     * The test case for which we should evaluate the expectation.
     */
    public ITestCase getTestCase();

    /**
     * The AST of the expectation we should evaluate.
     */
    public IStrategoTerm getExpectation();

    /**
     * The language implementation for the language under test that is used to run this test case.
     */
    public ILanguageImpl getLanguageUnderTest();

    /**
     * The result of parsing the fragment with the language under test.
     */
    public ParseResult<P> getParseResult();

    /**
     * The result of analyzing the fragment with the language under test.
     * 
     * May be null if the expectation only requires the {@link TestPhase#PARSING} phase, or if the parsing failed.
     */
    public @Nullable AnalysisResult<P, A> getAnalysisResult();

}
