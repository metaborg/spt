package org.metaborg.spt.testrunner.eclipse.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.FileObject;

/**
 * Represents running all the tests in a test suite.
 */
public class TestSuiteRun {

    public final MultiTestSuiteRun parent;
    public final String name;
    public final List<TestCaseRun> tests = new ArrayList<>();
    public final FileObject file;

    private int numFailed = 0;

    /**
     * Create a TestSuiteRun to represent the running of all tests in the test suite with the given name.
     * 
     * @param file
     *            the file containing the module (required so double clicking opens the module)
     * @param name
     *            the name of the test suite.
     */
    public TestSuiteRun(FileObject file, String name) {
        this(file, null, name);
    }

    /**
     * Create a TestSuiteRun to represent the running of all tests in the test suite with the given name.
     * 
     * @param file
     *            the file containing the module (required so double clicking opens the module)
     * @param parent
     *            if you want to run multiple test suites, you can group them in a {@link MultiTestSuiteRun} to keep
     *            track of them.
     * @param name
     *            the name of the test suite.
     */
    public TestSuiteRun(FileObject file, MultiTestSuiteRun parent, String name) {
        this.file = file;
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
