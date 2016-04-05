package org.metaborg.spt.core;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.source.ISourceRegion;
import org.spoofax.interpreter.terms.IStrategoTerm;

public interface ITestCase {

    /**
     * The description or name of the test case.
     */
    public String getDescription();

    /**
     * The SPT AST of the Fragment of this test case.
     */
    public IStrategoTerm getFragment();

    /**
     * The regions corresponding to the Selection nodes inside the test's fragment.
     */
    public List<ISourceRegion> getSelections();

    /**
     * The source file of the test suite from which this test case was extracted. May be null.
     */
    public @Nullable FileObject getResource();

    /**
     * The SPT AST terms of the test's expectations.
     */
    public List<IStrategoTerm> getExpectations();
}
