package org.metaborg.spt.core.fragments;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spt.core.IFragment;
import org.metaborg.spt.core.util.SPTUtil;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.terms.Term;
import org.spoofax.terms.TermVisitor;

public class ImploderFragment implements IFragment {

    private final IStrategoTerm fragment;
    private final FileObject resource;

    private final List<ISourceRegion> selections = new ArrayList<>();
    private final List<FragmentPiece> text = new LinkedList<>();

    public ImploderFragment(IStrategoTerm fragment, FileObject resource) {
        this.fragment = fragment;
        this.resource = resource;
        new TermVisitor() {

            /*
             * Because we visit left to right, top to bottom, we can collect all selections and fragment text during
             * this visit.
             */
            @Override public void preVisit(IStrategoTerm term) {
                if(Tools.isTermAppl(term)) {
                    String consName = SPTUtil.consName(term);
                    switch(consName) {
                        // collect the selected regions
                        case SPTUtil.SELECTION_CONS:
                            selections.add(SPTUtil.getRegion(term));
                            break;
                        case SPTUtil.FRAGMENT_CONS:
                            // it's a Fragment("sometext", <TailPart>)
                            IStrategoTerm textTerm = term.getSubterm(0);
                            text.add(new FragmentPiece(ImploderAttachment.getLeftToken(textTerm).getStartOffset(),
                                Term.asJavaString(textTerm)));
                            break;
                        case SPTUtil.TAILPART_MORE_CONS:
                            // it's a More(Selection(<marker>, "sometext", <marker>), "sometext", <Tailpart>)
                            IStrategoTerm selectionTextTerm = term.getSubterm(0).getSubterm(1);
                            text.add(
                                new FragmentPiece(ImploderAttachment.getLeftToken(selectionTextTerm).getStartOffset(),
                                    Term.asJavaString(selectionTextTerm)));
                            IStrategoTerm moreTextTerm = term.getSubterm(1);
                            text.add(new FragmentPiece(ImploderAttachment.getLeftToken(moreTextTerm).getStartOffset(),
                                Term.asJavaString(moreTextTerm)));
                            break;
                        default:
                            // nothing to do
                    }
                }
            }
        }.visit(fragment);
    }

    @Override public IStrategoTerm getSPTNode() {
        return fragment;
    }

    @Override public Iterable<ISourceRegion> getSelections() {
        return selections;
    }

    @Override public FileObject getResource() {
        return resource;
    }

    @Override public Iterable<FragmentPiece> getText() {
        return text;
    }

}
