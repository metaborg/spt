package org.metaborg.spt.core;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

import com.google.common.collect.Iterables;

public class TestCaseExtractionResult implements ITestCaseExtractionResult {

    private final boolean success;
    private final ISpoofaxParseUnit p;
    private final ISpoofaxAnalyzeUnit a;
    private final Iterable<IMessage> extraMessages;
    private final List<IMessage> allMessages = new LinkedList<>();
    private final Iterable<ITestCase> tests;

    public TestCaseExtractionResult(ISpoofaxParseUnit parseResult, @Nullable ISpoofaxAnalyzeUnit analysisResult,
        Iterable<IMessage> extraMessages, Iterable<ITestCase> testCases) {
        this.p = parseResult;
        this.a = analysisResult;
        this.extraMessages = extraMessages;
        this.tests = testCases;
        Iterables.addAll(allMessages, extraMessages);
        Iterables.addAll(allMessages, p.messages());
        if(a != null) {
            Iterables.addAll(allMessages, a.messages());
        }
        boolean suc = p.success() && a != null && a.success();
        for(IMessage m : allMessages) {
            // shortcut the loop if we already failed
            if(!suc) {
                break;
            }
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

    @Override public ISpoofaxParseUnit getParseResult() {
        return p;
    }

    @Override public @Nullable ISpoofaxAnalyzeUnit getAnalysisResult() {
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
