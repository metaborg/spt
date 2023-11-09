package org.metaborg.mbt.core.model;

import java.util.LinkedList;
import java.util.List;

import jakarta.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;

public class TestCase implements ITestCase {

    private final String description;
    private final ISourceRegion descriptionRegion;
    private final IFragment fragment;
    private final FileObject resource;
    private final IProject project;
    private final List<ITestExpectation> expectations = new LinkedList<>();

    public TestCase(String description, ISourceRegion descriptionRegion, IFragment fragment, FileObject resource,
        IProject project, List<ITestExpectation> expectations) {
        this.description = description;
        this.descriptionRegion = descriptionRegion;
        this.fragment = fragment;
        this.resource = resource;
        this.project = project;
        this.expectations.addAll(expectations);
    }

    @Override public String getDescription() {
        return description;
    }

    @Override public ISourceRegion getDescriptionRegion() {
        return descriptionRegion;
    }

    @Override public IFragment getFragment() {
        return fragment;
    }

    @Override public @Nullable FileObject getResource() {
        return resource;
    }

    @Override public @Nullable IProject getProject() {
        return project;
    }

    @Override public List<ITestExpectation> getExpectations() {
        return expectations;
    }

}
