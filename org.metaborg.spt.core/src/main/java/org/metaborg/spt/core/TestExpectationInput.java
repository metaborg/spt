package org.metaborg.spt.core;

import javax.annotation.Nullable;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.IParseUnit;

public class TestExpectationInput<P extends IParseUnit, A extends IAnalyzeUnit> implements ITestExpectationInput<P, A> {

    private final ITestCase test;
    private final ILanguageImpl lut;
    private final P p;
    private final A a;
    private final IContext ctx;

    public TestExpectationInput(ITestCase testCase, ILanguageImpl languageUnderTest, P parseResult,
        @Nullable A analysisResult, @Nullable IContext ctx) {
        this.test = testCase;
        this.lut = languageUnderTest;
        this.p = parseResult;
        this.a = analysisResult;
        this.ctx = ctx;
    }

    @Override public ITestCase getTestCase() {
        return test;
    }

    @Override public ILanguageImpl getLanguageUnderTest() {
        return lut;
    }

    @Override public P getParseResult() {
        return p;
    }

    @Override public @Nullable A getAnalysisResult() {
        return a;
    }

    @Override public IContext getContext() {
        return ctx;
    }
}
