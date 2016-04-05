package org.metaborg.spt.core.fragments;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.spt.core.IFragment;
import org.metaborg.spt.core.IFragmentBuilder;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class ImploderFragmentBuilder implements IFragmentBuilder {

    private IStrategoTerm fragmentTerm;
    private FileObject resource = null;

    @Override public IFragmentBuilder withFixture(IStrategoTerm fragmentFixture) {
        throw new UnsupportedOperationException("We don't support test fixtures yet.");
    }

    @Override public IFragmentBuilder withResource(FileObject resource) {
        this.resource = resource;
        return this;
    }

    @Override public IFragmentBuilder withFragment(IStrategoTerm fragment) {
        this.fragmentTerm = fragment;
        return this;
    }

    @Override public IFragment build() {
        return new ImploderFragment(fragmentTerm, resource);
    }

}
