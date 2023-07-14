package org.metaborg.mbt.core.extract;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.util.iterators.Iterables2;

public class TestCaseExtractionResult<P extends IParseUnit, A extends IAnalyzeUnit>
    implements ITestCaseExtractionResult<P, A> {

    private final String name;
    private final String language;
    private final boolean success;
    private final P p;
    private final A a;
    private final Iterable<IMessage> extraMessages;
    private final List<IMessage> allMessages = new LinkedList<>();
    private final Iterable<ITestCase> tests;

    public TestCaseExtractionResult(String name, @Nullable String language, P parseResult, @Nullable A analysisResult,
        Iterable<IMessage> extraMessages, Iterable<ITestCase> testCases) {
        this.name = name;
        this.language = language;
        this.p = parseResult;
        this.a = analysisResult;
        this.extraMessages = extraMessages;
        this.tests = testCases;
        Iterables2.addAll(allMessages, extraMessages);
        Iterables2.addAll(allMessages, p.messages());
        if(a != null) {
            Iterables2.addAll(allMessages, a.messages());
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

    @Override public String getName() {
        return name;
    }

    @Override public @Nullable String getLanguage() {
        return language;
    }

    @Override public boolean isSuccessful() {
        return success;
    }

    @Override public P getParseResult() {
        return p;
    }

    @Override public @Nullable A getAnalysisResult() {
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
