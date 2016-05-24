package org.metaborg.mbt.core.run;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.mbt.core.model.ITestCase;

public class TestExpectationInput<P extends IParseUnit, A extends IAnalyzeUnit> implements ITestExpectationInput<P, A> {

    private final ITestCase test;
    private final ILanguageImpl lut;
    private final IFragmentResult<P, A> fragmentResult;
    private final IFragmentParserConfig fragmentConfig;

    public TestExpectationInput(ITestCase testCase, ILanguageImpl languageUnderTest,
        IFragmentResult<P, A> fragmentResult, IFragmentParserConfig fragmentConfig) {
        this.test = testCase;
        this.lut = languageUnderTest;
        this.fragmentResult = fragmentResult;
        this.fragmentConfig = fragmentConfig;
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

    @Override public IFragmentParserConfig getFragmentParserConfig() {
        return fragmentConfig;
    }
}
