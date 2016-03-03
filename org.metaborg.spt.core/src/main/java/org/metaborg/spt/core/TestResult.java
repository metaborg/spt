package org.metaborg.spt.core;

import java.util.ArrayList;
import java.util.Collection;

import org.metaborg.core.messages.IMessage;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Iterables;

public class TestResult implements ITestResult {

    private final boolean success;
    private final Iterable<IMessage> messages;
    private final Iterable<IMessage> allMessages;
    private final Iterable<ITestExpectationOutput> results;

    public TestResult(boolean success, Iterable<IMessage> messages, Iterable<ITestExpectationOutput> results) {
        this.success = success;
        this.messages = messages;
        this.results = Iterables2.from(results);

        Collection<IMessage> allM = new ArrayList<>();
        Iterables.addAll(allM, messages);
        for(ITestExpectationOutput res : results) {
            Iterables.addAll(allM, res.getMessages());
        }
        this.allMessages = allM;
    }

    @Override public boolean isSuccessful() {
        return success;
    }

    @Override public Iterable<IMessage> getMessages() {
        return messages;
    }

    @Override public Iterable<IMessage> getAllMessages() {
        return allMessages;
    }

    @Override public Iterable<ITestExpectationOutput> getExpectationResults() {
        return results;
    }

}
