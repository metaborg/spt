package org.metaborg.mbt.core.run;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.syntax.IParseUnit;

import java.util.List;

/**
 * Output after evaluating a test expectation.
 */
public interface ITestExpectationOutput<P extends IParseUnit, A extends IAnalyzeUnit> {

    /**
     * Gets whether the test expectation was met.
     *
     * @return {@code true} when the test expectation was met;
     * otherwise, {@code false}
     */
    boolean isSuccessful();

    /**
     * Gets any messages returned by evaluating the test expectation.
     *
     * @return the messages
     */
    List<IMessage> getMessages();

    /**
     * Gets any (output) fragments that were part of the expectation
     * and the result of whatever the execution did to them.
     * 
     * @return the fragment results; or an empty list
     */
    List<? extends IFragmentResult<P, A>> getFragmentResults();

}
