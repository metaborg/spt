package org.metaborg.spt.core.spoofax;

import org.metaborg.core.messages.IMessage;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.ITestCase;
import org.metaborg.spt.core.TestCaseExtractionResult;

public class SpoofaxTestCaseExtractionResult extends TestCaseExtractionResult<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit>
    implements ISpoofaxTestCaseExtractionResult {

    public SpoofaxTestCaseExtractionResult(ISpoofaxParseUnit parseResult, ISpoofaxAnalyzeUnit analysisResult,
        Iterable<IMessage> extraMessages, Iterable<ITestCase> testCases) {
        super(parseResult, analysisResult, extraMessages, testCases);
    }

}
