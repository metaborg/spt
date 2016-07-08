package org.metaborg.mbt.core.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceRegion;

public class Fragment implements IFragment {

    private final ISourceRegion region;
    private final FileObject resource;
    private final IProject project;

    private final List<ISourceRegion> selections = new ArrayList<>();
    private final List<FragmentPiece> text = new LinkedList<>();

    public Fragment(ISourceRegion region, List<ISourceRegion> selections, List<FragmentPiece> text, FileObject resource,
        IProject project) {
        this.region = region;
        this.resource = resource;
        this.project = project;
        this.selections.addAll(selections);
        this.text.addAll(text);
    }

    @Override public ISourceRegion getRegion() {
        return region;
    }

    @Override public List<ISourceRegion> getSelections() {
        return selections;
    }

    @Override public FileObject getResource() {
        return resource;
    }

    @Override public IProject getProject() {
        return project;
    }

    @Override public Iterable<FragmentPiece> getText() {
        return text;
    }
}
