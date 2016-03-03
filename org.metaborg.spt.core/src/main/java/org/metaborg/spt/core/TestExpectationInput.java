package org.metaborg.spt.core;

import javax.annotation.Nullable;

import org.metaborg.core.analysis.AnalysisResult;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.ParseResult;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class TestExpectationInput implements ITestExpectationInput<IStrategoTerm, IStrategoTerm> {

    private final ITestCase test;
    private final IStrategoTerm expectation;
    private final ILanguageImpl lut;
    private final ParseResult<IStrategoTerm> pRes;
    private final AnalysisResult<IStrategoTerm, IStrategoTerm> aRes;

    public TestExpectationInput(ITestCase testCase, IStrategoTerm expectationTerm, ILanguageImpl languageUnderTest,
        ParseResult<IStrategoTerm> parseResult, @Nullable AnalysisResult<IStrategoTerm, IStrategoTerm> analysisResult) {
        this.test = testCase;
        this.expectation = expectationTerm;
        this.lut = languageUnderTest;
        this.pRes = parseResult;
        this.aRes = analysisResult;
    }

    @Override public ITestCase getTestCase() {
        return test;
    }

    @Override public IStrategoTerm getExpectation() {
        return expectation;
    }

    @Override public ILanguageImpl getLanguageUnderTest() {
        return lut;
    }

    @Override public ParseResult<IStrategoTerm> getParseResult() {
        return pRes;
    }

    @Override public @Nullable AnalysisResult<IStrategoTerm, IStrategoTerm> getAnalysisResult() {
        return aRes;
    }

}
