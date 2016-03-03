package org.metaborg.spt.core;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.source.ISourceRegion;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class TestCase implements ITestCase {

    private final String description;
    private final IStrategoTerm fragment;
    private final FileObject resource;
    private final List<ISourceRegion> selections = new ArrayList<>();
    private final List<IStrategoTerm> expectations = new ArrayList<>();

    public TestCase(String description, IStrategoTerm fragment, @Nullable FileObject resource,
        List<ISourceRegion> selections, List<IStrategoTerm> expectations) {
        this.description = description;
        this.fragment = fragment;
        this.resource = resource;
        this.selections.addAll(selections);
        this.expectations.addAll(expectations);
    }

    @Override public String getDescription() {
        return description;
    }

    @Override public IStrategoTerm getFragment() {
        return fragment;
    }

    @Override public List<ISourceRegion> getSelections() {
        return selections;
    }

    @Override public @Nullable FileObject getResource() {
        return resource;
    }

    @Override public List<IStrategoTerm> getExpectations() {
        return expectations;
    }

}
