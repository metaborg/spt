package org.metaborg.mbt.core.extract;

import javax.annotation.Nullable;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.mbt.core.model.ITestCase;

/**
 * The result of trying to extract test cases from an SPT test suite.
 */
public interface ITestCaseExtractionResult<P extends IParseUnit, A extends IAnalyzeUnit> {

    /**
     * The name of the test suite.
     */
    String getName();

    /**
     * The name of the language under test.
     */
    @Nullable String getLanguage();

    /**
     * True iff there were no errors during parsing and analysis.
     */
    boolean isSuccessful();

    /**
     * The result of trying to parse the test suite.
     * 
     * May be null, if we couldn't even parse.
     */
    @Nullable P getParseResult();

    /**
     * The result of trying to analyze the test suite.
     * 
     * May be null.
     */
    @Nullable A getAnalysisResult();

    /**
     * All messages raised while extracting the test case.
     * 
     * This includes the messages from parsing, analysis, and the extra messages from {@link #getMessages()}.
     */
    Iterable<IMessage> getAllMessages();

    /**
     * Any extra messages created while extracting the test case.
     * 
     * These can be caused by exceptions thrown by the parsing or analysis, or from errors during the construction of
     * the test case. E.g., messages about missing ITestExpectations that can evaluate this test's expectations.
     */
    Iterable<IMessage> getMessages();

    /**
     * The test cases that were extracted.
     */
    Iterable<ITestCase> getTests();
}
