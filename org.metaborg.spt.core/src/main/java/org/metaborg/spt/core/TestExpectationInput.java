package org.metaborg.spt.core;

import javax.annotation.Nullable;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class TestExpectationInput implements ITestExpectationInput {

    private final ITestCase test;
    private final IStrategoTerm expectation;
    private final ILanguageImpl lut;
    private final ISpoofaxParseUnit pRes;
    private final ISpoofaxAnalyzeUnit aRes;

    public TestExpectationInput(ITestCase testCase, IStrategoTerm expectationTerm, ILanguageImpl languageUnderTest,
        ISpoofaxParseUnit parseResult, @Nullable ISpoofaxAnalyzeUnit analysisResult) {
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

    @Override public ISpoofaxParseUnit getParseResult() {
        return pRes;
    }

    @Override public @Nullable ISpoofaxAnalyzeUnit getAnalysisResult() {
        return aRes;
    }

}
