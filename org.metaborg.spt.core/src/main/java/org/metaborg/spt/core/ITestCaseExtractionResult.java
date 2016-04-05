package org.metaborg.spt.core;

import javax.annotation.Nullable;

import org.metaborg.core.analysis.AnalysisResult;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.syntax.ParseResult;
import org.spoofax.interpreter.terms.IStrategoTerm;

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
    public ParseResult<IStrategoTerm> getParseResult();

    /**
     * The result of trying to analyze the test suite.
     * 
     * May be null.
     */
    public @Nullable AnalysisResult<IStrategoTerm, IStrategoTerm> getAnalysisResult();

    /**
     * All messages from parsing and analysis of the test suite.
     */
    public Iterable<IMessage> getAllMessages();

    /**
     * The test cases that were extracted.
     */
    public Iterable<ITestCase> getTests();
}
