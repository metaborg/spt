package org.metaborg.mbt.core.model;

import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;

public interface ITestCase {

    /**
     * The description or name of the test case.
     */
    String getDescription();

    /**
     * The source region covered by the test's description.
     * 
     * Use this to place messages that appear during test runs, but that have no corresponding region in the test
     * fragment.
     */
    ISourceRegion getDescriptionRegion();

    /**
     * The fragment of this test case. I.e., the piece of code written in the language under test that is being tested.
     */
    IFragment getFragment();

    /**
     * The source file (or other resource) of the test suite from which this test case was extracted.
     */
    FileObject getResource();

    /**
     * The project that contains this test. It is required for analysis of fragments.
     */
    IProject getProject();

    /**
     * The test expectations for this test case.
     */
    List<ITestExpectation> getExpectations();

}
