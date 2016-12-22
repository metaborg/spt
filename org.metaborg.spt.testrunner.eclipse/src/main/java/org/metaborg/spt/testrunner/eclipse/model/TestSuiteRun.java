package org.metaborg.spt.testrunner.eclipse.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;
import org.metaborg.spt.core.extract.ISpoofaxTestCaseExtractionResult;

/**
 * Represents running all the tests in a test suite.
 */
public class TestSuiteRun {

    public final MultiTestSuiteRun parent;
    public final String name;
    public final List<TestCaseRun> tests = new ArrayList<>();
    public final FileObject file;
    public final IProject project;
    public final ISpoofaxTestCaseExtractionResult ext;

    private int numFailed = 0;
    private int numPassed = 0;

    /**
     * Create a TestSuiteRun to represent the running of all tests in the test suite with the given name.
     * 
     * @param ext
     *            the result of extracting this test suite (a failed extraction is ok).
     * @param project
     *            the project containing this test suite.
     * @param file
     *            the file containing the module (required so double clicking opens the module)
     * @param name
     *            the name of the test suite.
     */
    public TestSuiteRun(ISpoofaxTestCaseExtractionResult ext, IProject project, FileObject file, String name) {
        this(ext, project, file, null, name);
    }

    /**
     * Create a TestSuiteRun to represent the running of all tests in the test suite with the given name.
     * 
     * @param ext
     *            the result of extracting this test suite (a failed extraction is ok).
     * @param project
     *            the project containing this test suite.
     * @param file
     *            the file containing the module (required so double clicking opens the module)
     * @param parent
     *            if you want to run multiple test suites, you can group them in a {@link MultiTestSuiteRun} to keep
     *            track of them.
     * @param name
     *            the name of the test suite.
     */
    public TestSuiteRun(ISpoofaxTestCaseExtractionResult ext, IProject project, FileObject file,
        MultiTestSuiteRun parent, String name) {
        this.ext = ext;
        this.project = project;
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
     * The number of successfully completed test case runs (so far) in this test suite.
     */
    public int numPassed() {
        return numPassed;
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

    /**
     * Called by our kids to notify us that they passed.
     */
    protected void pass() {
        numPassed++;
        if(parent != null) {
            parent.pass();
        }
    }

}
