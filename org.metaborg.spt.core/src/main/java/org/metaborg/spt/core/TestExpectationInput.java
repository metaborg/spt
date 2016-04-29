package org.metaborg.spt.core;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.IParseUnit;

public class TestExpectationInput<P extends IParseUnit, A extends IAnalyzeUnit> implements ITestExpectationInput<P, A> {

    private final ITestCase test;
    private final ILanguageImpl lut;
    private final IFragmentResult<P, A> fragmentResult;

    public TestExpectationInput(ITestCase testCase, ILanguageImpl languageUnderTest,
        IFragmentResult<P, A> fragmentResult) {
        this.test = testCase;
        this.lut = languageUnderTest;
        this.fragmentResult = fragmentResult;
    }

    @Override public ITestCase getTestCase() {
        return test;
    }

    @Override public ILanguageImpl getLanguageUnderTest() {
        return lut;
    }

    @Override public IFragmentResult<P, A> getFragmentResult() {
        return fragmentResult;
    }
}
