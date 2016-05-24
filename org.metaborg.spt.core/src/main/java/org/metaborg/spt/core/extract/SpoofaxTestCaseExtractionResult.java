package org.metaborg.spt.core.extract;

import org.metaborg.core.messages.IMessage;
import org.metaborg.mbt.core.extract.TestCaseExtractionResult;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

public class SpoofaxTestCaseExtractionResult extends TestCaseExtractionResult<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit>
    implements ISpoofaxTestCaseExtractionResult {

    private final String startSymbol;

    public SpoofaxTestCaseExtractionResult(ISpoofaxParseUnit parseResult, ISpoofaxAnalyzeUnit analysisResult,
        Iterable<IMessage> extraMessages, Iterable<ITestCase> testCases) {
        this(parseResult, analysisResult, extraMessages, testCases, null);
    }

    public SpoofaxTestCaseExtractionResult(ISpoofaxParseUnit parseResult, ISpoofaxAnalyzeUnit analysisResult,
        Iterable<IMessage> extraMessages, Iterable<ITestCase> testCases, String startSymbol) {
        super(parseResult, analysisResult, extraMessages, testCases);
        this.startSymbol = startSymbol;
    }

    @Override public String getStartSymbol() {
        return startSymbol;
    }

}
