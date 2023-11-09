package org.metaborg.mbt.core.run;

import java.util.ArrayList;
import java.util.Collection;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.util.iterators.Iterables2;

public class TestResult<P extends IParseUnit, A extends IAnalyzeUnit> implements ITestResult<P, A> {

    private final ITestCase test;
    private final boolean success;
    private final Iterable<IMessage> messages;
    private final Iterable<IMessage> allMessages;
    private final IFragmentResult<P, A> fragmentResult;
    private final Iterable<? extends ITestExpectationOutput<P, A>> results;

    public TestResult(ITestCase test, boolean success, Iterable<IMessage> messages,
        IFragmentResult<P, A> fragmentResult, Iterable<? extends ITestExpectationOutput<P, A>> results) {
        this.test = test;
        this.success = success;
        this.messages = messages;
        this.fragmentResult = fragmentResult;
        this.results = results;

        Collection<IMessage> allM = new ArrayList<>();
        Iterables2.addAll(allM, messages);
        for(ITestExpectationOutput<P, A> res : results) {
            Iterables2.addAll(allM, res.getMessages());
        }
        this.allMessages = allM;
    }

    @Override public ITestCase getTest() {
        return test;
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

    @Override public IFragmentResult<P, A> getFragmentResult() {
        return fragmentResult;
    }

    @Override public Iterable<? extends ITestExpectationOutput<P, A>> getExpectationResults() {
        return results;
    }

}
