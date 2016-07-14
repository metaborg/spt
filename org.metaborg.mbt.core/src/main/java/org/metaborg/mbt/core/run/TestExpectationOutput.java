package org.metaborg.mbt.core.run;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.syntax.IParseUnit;

public class TestExpectationOutput<P extends IParseUnit, A extends IAnalyzeUnit>
    implements ITestExpectationOutput<P, A> {

    private final boolean success;
    private final Iterable<IMessage> messages;
    private final Iterable<IFragmentResult<P, A>> fs;

    public TestExpectationOutput(boolean success, Iterable<IMessage> messages, Iterable<IFragmentResult<P, A>> fs) {
        this.success = success;
        this.messages = messages;
        this.fs = fs;
    }

    @Override public boolean isSuccessful() {
        return success;
    }

    @Override public Iterable<IMessage> getMessages() {
        return messages;
    }

    @Override public Iterable<? extends IFragmentResult<P, A>> getFragmentResults() {
        return fs;
    }


}
