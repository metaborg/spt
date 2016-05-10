package org.metaborg.spt.core.run;

import org.metaborg.core.messages.IMessage;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.run.ITestExpectationOutput;
import org.metaborg.mbt.core.run.TestResult;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

import com.google.common.collect.Lists;

public class SpoofaxTestResult extends TestResult<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit>
    implements ISpoofaxTestResult {

    private final Iterable<ISpoofaxTestExpectationOutput> results;
    private final ISpoofaxFragmentResult fragmentResult;

    public SpoofaxTestResult(ITestCase test, boolean success, Iterable<IMessage> messages,
        ISpoofaxFragmentResult fragmentResult, Iterable<ISpoofaxTestExpectationOutput> results) {
        super(test, success, messages, fragmentResult,
            Lists.<ITestExpectationOutput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit>>newLinkedList(results));
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
