package org.metaborg.spt.testrunner.eclipse.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents running all the tests in a test suite.
 */
public class TestSuiteRun {

    public final MultiTestSuiteRun parent;
    public final String name;
    public final List<TestCaseRun> tests = new ArrayList<>();

    private int numFailed = 0;

    /**
     * Create a TestSuiteRun to represent the running of all tests in the test suite with the given name.
     * 
     * @param name
     *            the name of the test suite.
     */
    public TestSuiteRun(String name) {
        this(null, name);
    }

    /**
     * Create a TestSuiteRun to represent the running of all tests in the test suite with the given name.
     * 
     * @param parent
     *            if you want to run multiple test suites, you can group them in a {@link MultiTestSuiteRun} to keep
     *            track of them.
     * @param name
     *            the name of the test suite.
     */
    public TestSuiteRun(MultiTestSuiteRun parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    /**
     * The number of failed test case runs (so far) in this test suite.
     */
    public int numFailed() {
        return numFailed;
    }

    /**
     * Called by our kids to notify us that they failed.
     */
    protected void fail() {
        numFailed++;
        if(parent != null) {
            parent.fail();
        }
    }

}
