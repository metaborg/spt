package org.metaborg.mbt.core.run;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.mbt.core.model.ITestCase;

/**
 * The result of running a single ITestCase.
 */
public interface ITestResult<P extends IParseUnit, A extends IAnalyzeUnit> {

    /**
     * The test for which this is the result.
     */
    ITestCase getTest();

    /**
     * True if the test passed.
     */
    boolean isSuccessful();

    /**
     * Any extra messages caused by running the test case, that weren't caused by evaluating expectations.
     */
    Iterable<IMessage> getMessages();

    /**
     * All messages caused by running the test case.
     * 
     * The combination of the extra messages and all messages from all expectation results.
     */
    Iterable<IMessage> getAllMessages();


    /**
     * The result of what happened to the input fragment.
     * 
     * This depends on what the expectations required to happen.
     */
    IFragmentResult<P, A> getFragmentResult();

    /**
     * The results of evaluating the expectations of this test.
     */
    Iterable<? extends ITestExpectationOutput<P, A>> getExpectationResults();
}
