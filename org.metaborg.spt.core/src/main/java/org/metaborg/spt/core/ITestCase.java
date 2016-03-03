package org.metaborg.spt.core;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.source.ISourceRegion;
import org.spoofax.interpreter.terms.IStrategoTerm;

public interface ITestCase {

    public String getDescription();

    /**
     * The SPT AST of the Fragment of this test case.
     */
    public IStrategoTerm getFragment();

    public List<ISourceRegion> getSelections();

    public @Nullable FileObject getResource();

    public List<IStrategoTerm> getExpectations();
}
