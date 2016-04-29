package org.metaborg.spt.core;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.syntax.IParseUnit;

/**
 * Output after evaluating a test expectation.
 */
public interface ITestExpectationOutput<P extends IParseUnit, A extends IAnalyzeUnit> {

    /**
     * True if the test expectation passed.
     */
    public boolean isSuccessful();

    /**
     * Any messages returned by evaluating the test expectation.
     */
    public Iterable<IMessage> getMessages();

    /**
     * Any (output) fragments that were part of the expectation and the result of whatever the execution did to them.
     * 
     * May be empty.
     */
    public Iterable<? extends IFragmentResult<P, A>> getFragmentResults();
}
