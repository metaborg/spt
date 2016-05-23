package org.metaborg.spt.core.run;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.run.TestExpectationInput;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

public class SpoofaxTestExpectationInput extends TestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit>
    implements ISpoofaxTestExpectationInput {

    public SpoofaxTestExpectationInput(ITestCase testCase, ILanguageImpl languageUnderTest,
        ISpoofaxFragmentResult fragmentResult) {
        super(testCase, languageUnderTest, fragmentResult);
    }

}
