package org.metaborg.spt.core;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;

public class TestCase implements ITestCase {

    private final String description;
    private final IFragment fragment;
    private final FileObject resource;
    private final List<ExpectationPair> expectations = new LinkedList<>();

    public TestCase(String description, IFragment fragment, @Nullable FileObject resource,
        List<ExpectationPair> expectations) {
        this.description = description;
        this.fragment = fragment;
        this.resource = resource;
        this.expectations.addAll(expectations);
    }

    @Override public String getDescription() {
        return description;
    }

    @Override public IFragment getFragment() {
        return fragment;
    }

    @Override public @Nullable FileObject getResource() {
        return resource;
    }

    @Override public List<ExpectationPair> getExpectations() {
        return expectations;
    }

}
