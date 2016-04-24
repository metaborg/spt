package org.metaborg.spt.core.spoofax;

import javax.annotation.Nullable;

import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.ITestCase;
import org.metaborg.spt.core.TestExpectationInput;

public class SpoofaxTestExpectationInput extends TestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit>
    implements ISpoofaxTestExpectationInput {

    public SpoofaxTestExpectationInput(ITestCase testCase, ILanguageImpl languageUnderTest,
        ISpoofaxParseUnit parseResult, @Nullable ISpoofaxAnalyzeUnit analysisResult, @Nullable IContext ctx) {
        super(testCase, languageUnderTest, parseResult, analysisResult, ctx);
    }

}
