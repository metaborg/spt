package org.metaborg.spt.core;

import org.metaborg.core.messages.IMessage;

/**
 * Output after evaluating a test expectation.
 */
public interface ITestExpectationOutput {

    /**
     * True if the test expectation passed.
     */
    public boolean isSuccessful();

    /**
     * Any messages returned by evaluating the test expectation.
     */
    public Iterable<IMessage> getMessages();
}
