package org.metaborg.spt.core;

import org.metaborg.core.messages.IMessage;
import org.metaborg.util.iterators.Iterables2;

public class TestExpectationOutput implements ITestExpectationOutput {

    private final boolean success;
    private final Iterable<IMessage> messages;

    public TestExpectationOutput(boolean success, Iterable<IMessage> messages) {
        this.success = success;
        this.messages = Iterables2.from(messages);
    }

    @Override public boolean isSuccessful() {
        return success;
    }

    @Override public Iterable<IMessage> getMessages() {
        return messages;
    }


}
