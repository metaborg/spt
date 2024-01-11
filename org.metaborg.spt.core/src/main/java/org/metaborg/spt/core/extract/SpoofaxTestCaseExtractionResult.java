package org.metaborg.spt.core.extract;

import jakarta.annotation.Nullable;

import org.metaborg.core.messages.IMessage;
import org.metaborg.mbt.core.extract.TestCaseExtractionResult;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

public class SpoofaxTestCaseExtractionResult extends TestCaseExtractionResult<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit>
    implements ISpoofaxTestCaseExtractionResult {

    private final String startSymbol;

    public SpoofaxTestCaseExtractionResult(String name, @Nullable String language, ISpoofaxParseUnit parseResult,
        ISpoofaxAnalyzeUnit analysisResult, Iterable<IMessage> extraMessages, Iterable<ITestCase> testCases) {
        this(name, language, parseResult, analysisResult, extraMessages, testCases, null);
    }

    public SpoofaxTestCaseExtractionResult(String name, @Nullable String language, ISpoofaxParseUnit parseResult,
        ISpoofaxAnalyzeUnit analysisResult, Iterable<IMessage> extraMessages, Iterable<ITestCase> testCases,
        String startSymbol) {
        super(name, language, parseResult, analysisResult, extraMessages, testCases);
        this.startSymbol = startSymbol;
    }

    @Override public String getStartSymbol() {
        return startSymbol;
    }

}
