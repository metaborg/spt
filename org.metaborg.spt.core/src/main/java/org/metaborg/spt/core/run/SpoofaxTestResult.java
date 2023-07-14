package org.metaborg.spt.core.run;

import java.util.Collection;
import java.util.LinkedList;

import org.metaborg.core.messages.IMessage;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.run.TestResult;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

public class SpoofaxTestResult extends TestResult<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit>
    implements ISpoofaxTestResult {

    private final Iterable<ISpoofaxTestExpectationOutput> results;
    private final ISpoofaxFragmentResult fragmentResult;

    public SpoofaxTestResult(ITestCase test, boolean success, Iterable<IMessage> messages,
        ISpoofaxFragmentResult fragmentResult, Collection<ISpoofaxTestExpectationOutput> results) {
        super(test, success, messages, fragmentResult,
            new LinkedList<>(results));
        this.results = results;
        this.fragmentResult = fragmentResult;
    }

    @Override public Iterable<ISpoofaxTestExpectationOutput> getExpectationResults() {
        return results;
    }

    @Override public ISpoofaxFragmentResult getFragmentResult() {
        return fragmentResult;
    }
}
