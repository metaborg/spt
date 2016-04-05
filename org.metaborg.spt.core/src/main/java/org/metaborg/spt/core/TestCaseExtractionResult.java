package org.metaborg.spt.core;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import org.metaborg.core.analysis.AnalysisMessageResult;
import org.metaborg.core.analysis.AnalysisResult;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.syntax.ParseResult;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.Iterables;

public class TestCaseExtractionResult implements ITestCaseExtractionResult {

    private final boolean success;
    private final ParseResult<IStrategoTerm> p;
    private final AnalysisResult<IStrategoTerm, IStrategoTerm> a;
    private final Iterable<IMessage> extraMessages;
    private final List<IMessage> allMessages = new LinkedList<>();
    private final Iterable<ITestCase> tests;

    public TestCaseExtractionResult(ParseResult<IStrategoTerm> parseResult,
        @Nullable AnalysisResult<IStrategoTerm, IStrategoTerm> analysisResult, Iterable<IMessage> extraMessages,
        Iterable<ITestCase> testCases) {
        this.p = parseResult;
        this.a = analysisResult;
        this.extraMessages = extraMessages;
        this.tests = testCases;
        Iterables.addAll(allMessages, extraMessages);
        Iterables.addAll(allMessages, p.messages());
        for(AnalysisMessageResult r : a.messageResults) {
            Iterables.addAll(allMessages, r.messages);
        }
        boolean suc = p.result != null;
        for(IMessage m : allMessages) {
            if(m.severity() == MessageSeverity.ERROR) {
                suc = false;
                break;
            }
        }
        this.success = suc;
    }

    @Override public boolean isSuccessful() {
        return success;
    };

    @Override public ParseResult<IStrategoTerm> getParseResult() {
        return p;
    }

    @Override public @Nullable AnalysisResult<IStrategoTerm, IStrategoTerm> getAnalysisResult() {
        return a;
    }

    @Override public Iterable<IMessage> getAllMessages() {
        return allMessages;
    }

    @Override public Iterable<ITestCase> getTests() {
        return tests;
    }

    @Override public Iterable<IMessage> getMessages() {
        return extraMessages;
    }

}
