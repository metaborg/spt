package org.metaborg.spt.testrunner.eclipse.model;

import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.spt.core.run.ISpoofaxTestResult;

/**
 * Represents a testcase that is being run.
 * 
 * Used by the Eclipse UI part of the runner. It notifies its parent (if any) when it gets the result for the test whose
 * run it represents.
 */
public class TestCaseRun {

    public final TestSuiteRun parent;
    public final ITestCase test;

    private ISpoofaxTestResult res = null;
    // keep track of the duration here
    // maybe at some point we will move that job to mbt.core
    private long start = -1;
    private long duration = -1;

    /**
     * Create a TestCaseRun, representing a run of an ITestCase.
     * 
     * @param parent
     *            the parent test suite (may be null).
     * @param test
     *            the test that you will run.
     */
    public TestCaseRun(TestSuiteRun parent, ITestCase test) {
        this.parent = parent;
        this.test = test;
        this.start = System.currentTimeMillis();
    }

    /**
     * Signals that you want to start the run.
     * 
     * Only required if you care about timing.
     */
    public void start() {
        this.start = System.currentTimeMillis();
    }

    /**
     * Finish this test case run.
     * 
     * We will update our parent (if any) with the result.
     * 
     * @param res
     *            the result of running this test case.
     */
    public void finish(ISpoofaxTestResult res) {
        this.duration = System.currentTimeMillis() - start;
        this.res = res;
        if(parent != null && res != null) {
            if(res.isSuccessful()) {
                parent.pass();
            } else {
                parent.fail();
            }
        }
    }

    /**
     * The result of running this test case.
     * 
     * May be null, if the run wasn't finished yet.
     */
    public ISpoofaxTestResult result() {
        return res;
    }

    /**
     * The time (in ms) that passed between the last call to {@link #start()} and the last call to
     * {@link #finish(ISpoofaxTestResult)}.
     */
    public long duration() {
        return duration;
    }
}
