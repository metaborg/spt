package org.metaborg.spt.core.spoofax;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.ITestCase;
import org.metaborg.spt.core.TestExpectationInput;

public class SpoofaxTestExpectationInput extends TestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit>
    implements ISpoofaxTestExpectationInput {

    public SpoofaxTestExpectationInput(ITestCase testCase, ILanguageImpl languageUnderTest,
        ISpoofaxFragmentResult fragmentResult) {
        super(testCase, languageUnderTest, fragmentResult);
    }

}
