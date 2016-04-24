package org.metaborg.spt.core;

import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceRegion;

public interface ITestCase {

    /**
     * The description or name of the test case.
     */
    public String getDescription();

    /**
     * The source region covered by the test's description.
     * 
     * Use this to place messages that appear during test runs, but that have no corresponding region in the test
     * fragment.
     */
    public ISourceRegion getDescriptionRegion();

    /**
     * The fragment of this test case. I.e., the piece of code written in the language under test that is being tested.
     */
    public IFragment getFragment();

    /**
     * The source file (or other resource) of the test suite from which this test case was extracted.
     */
    public FileObject getResource();

    /**
     * The project that contains this test. It is required for analysis of fragments.
     */
    public IProject getProject();

    /**
     * The test expectations for this test case.
     */
    public List<ITestExpectation> getExpectations();

}
