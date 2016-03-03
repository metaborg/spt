package org.metaborg.spt.core;

import org.metaborg.core.messages.IMessage;

public interface ITestResult {

    public boolean isSuccessful();

    public Iterable<IMessage> getMessages();

    public Iterable<IMessage> getAllMessages();

    public Iterable<ITestExpectationOutput> getExpectationResults();
}
