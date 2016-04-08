package org.metaborg.spt.core;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceRegion;
import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * A Fragment represents a piece of code within an SPT test suite, written in another language.
 * 
 * Examples are the fragment of a test case, or the fragment of a 'parse to' test expectation. The former would be
 * written in the language under test, the latter possibly in another language.
 * 
 * The problem with Fragments, is that the text has to be extracted from the original SPT specification, and parsed with
 * another language, while keeping the offsets of the nodes in the returned parse result correct within the larger
 * context of the SPT specification.
 * 
 * The IFragment simply represents the SPT AST node and the selected regions within that node. These selected regions
 * have offsets based on the SPT AST, which is why parsing a fragment should be done using an IFragmentParser, to ensure
 * that the character offsets of the parse result match properly with the selected regions.
 */
public interface IFragment {

    /**
     * The SPT Fragment node representing the fragment.
     */
    public IStrategoTerm getSPTNode();

    /**
     * The selections of this fragment.
     * 
     * Ordered by the order in which they appeared in the fragment.
     */
    public List<ISourceRegion> getSelections();

    /**
     * The source file of the test suite from which this fragment was extracted. May be null.
     */
    public @Nullable FileObject getResource();

    /**
     * The project that contains the test suite that contains this fragment. It is required for analysis.
     */
    public IProject getProject();

    /**
     * The text of this selection. It is returned as tuples of an offset and a piece of text. The offset is the start
     * offset of the piece of text in the rest of the Fragment's surrounding source (usually an SPT test suite).
     * 
     * The text is a consecutive part of program text from the fragment. This text will not contain the SPT specific
     * text.
     * 
     * These tuples should be used by an IFragmentParser to ensure the parse result has the correct offsets.
     */
    public Iterable<FragmentPiece> getText();

    public static class FragmentPiece {
        public final int startOffset;
        public final String text;

        public FragmentPiece(int offset, String txt) {
            this.text = txt;
            this.startOffset = offset;
        }
    }

}
