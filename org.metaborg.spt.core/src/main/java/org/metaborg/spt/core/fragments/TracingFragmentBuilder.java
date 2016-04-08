package org.metaborg.spt.core.fragments;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.IFragment;
import org.metaborg.spt.core.IFragmentBuilder;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Inject;

public class TracingFragmentBuilder implements IFragmentBuilder {

    private final ISpoofaxTracingService traceService;

    private IStrategoTerm fragmentTerm;
    private FileObject resource = null;
    private IProject project = null;

    @Inject public TracingFragmentBuilder(ISpoofaxTracingService traceService) {
        this.traceService = traceService;
    }

    @Override public IFragmentBuilder withFixture(IStrategoTerm fragmentFixture) {
        throw new UnsupportedOperationException("We don't support test fixtures yet.");
    }

    @Override public IFragmentBuilder withResource(FileObject resource) {
        this.resource = resource;
        return this;
    }

    @Override public IFragmentBuilder withProject(IProject project) {
        this.project = project;
        return this;
    }

    @Override public IFragmentBuilder withFragment(IStrategoTerm fragment) {
        this.fragmentTerm = fragment;
        return this;
    }

    @Override public IFragment build() {
        if(resource == null) {
            throw new IllegalStateException("Can't construct a fragment without a resource.");
        }
        if(project == null) {
            throw new IllegalStateException("Can't construct a fragment without a project.");
        }
        if(fragmentTerm == null) {
            throw new IllegalStateException("Can't construct a fragment without the AST node.");
        }
        return new TracingFragment(traceService, fragmentTerm, resource, project);
    }

}
