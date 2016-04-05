package org.metaborg.spt.core;

import org.metaborg.core.messages.IMessage;

/**
 * The result of running a single ITestCase.
 */
public interface ITestResult {

    /**
     * True if the test passed.
     */
    public boolean isSuccessful();

    /**
     * Any extra messages caused by running the test case, that weren't caused by evaluating expectations.
     */
    public Iterable<IMessage> getMessages();

    /**
     * All messages caused by running the test case.
     * 
     * The combination of the extra messages and all messages from all expectation results.
     */
    public Iterable<IMessage> getAllMessages();

    /**
     * The results of evaluating the expectations of this test.
     */
    public Iterable<ITestExpectationOutput> getExpectationResults();
}
