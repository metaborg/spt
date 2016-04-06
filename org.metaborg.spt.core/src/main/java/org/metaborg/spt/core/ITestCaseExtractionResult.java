package org.metaborg.spt.core;

import javax.annotation.Nullable;

import org.metaborg.core.messages.IMessage;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

/**
 * The result of trying to extract test cases from an SPT test suite.
 */
public interface ITestCaseExtractionResult {

    /**
     * True iff there were no errors during parsing and analysis.
     */
    public boolean isSuccessful();

    /**
     * The result of trying to parse the test suite.
     */
    public ISpoofaxParseUnit getParseResult();

    /**
     * The result of trying to analyze the test suite.
     * 
     * May be null.
     */
    public @Nullable ISpoofaxAnalyzeUnit getAnalysisResult();

    /**
     * All messages raised while extracting the test case.
     * 
     * This includes the messages from parsing, analysis, and the extra messages from {@link #getMessages()}.
     */
    public Iterable<IMessage> getAllMessages();

    /**
     * Any extra messages created while extracting the test case.
     * 
     * These can be caused by exceptions thrown by the parsing or analysis, or from errors during the construction of
     * the test case. E.g., messages about missing ITestExpectations that can evaluate this test's expectations.
     */
    public Iterable<IMessage> getMessages();

    /**
     * The test cases that were extracted.
     */
    public Iterable<ITestCase> getTests();
}
