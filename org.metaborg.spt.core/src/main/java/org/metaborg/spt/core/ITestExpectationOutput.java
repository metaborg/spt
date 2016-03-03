package org.metaborg.spt.core;

import org.metaborg.core.messages.IMessage;

public interface ITestExpectationOutput {

    public boolean isSuccessful();

    public Iterable<IMessage> getMessages();
}
