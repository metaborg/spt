package org.metaborg.spt.core.fragments;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.IFragment;
import org.metaborg.spt.core.util.SPTUtil;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;
import org.spoofax.terms.TermVisitor;

public class TracingFragment implements IFragment {

    private final ISpoofaxTracingService traceService;

    private final IStrategoTerm fragment;
    private final FileObject resource;
    private final IProject project;

    private final List<ISourceRegion> selections = new ArrayList<>();
    private final List<FragmentPiece> text = new LinkedList<>();

    public TracingFragment(ISpoofaxTracingService tracingService, IStrategoTerm fragment, FileObject resource,
        IProject project) {
        this.traceService = tracingService;
        this.fragment = fragment;
        this.resource = resource;
        this.project = project;
        new TermVisitor() {

            /*
             * Because we visit left to right, top to bottom, we can collect all selections and fragment text during
             * this visit.
             */
            @Override public void preVisit(IStrategoTerm term) {
                if(Tools.isTermAppl(term)) {
                    ISourceLocation loc = null;
                    String consName = SPTUtil.consName(term);
                    switch(consName) {
                        // collect the selected regions
                        case SPTUtil.SELECTION_CONS:
                            // we want the region of the term inside the selection, not including the selection markers
                            loc = traceService.location(term.getSubterm(1));
                            if(loc == null) {
                                // TODO is this ok? or should we fail more gracefully?
                                throw new IllegalArgumentException("Selection " + term + " has no origin information.");
                            }
                            selections.add(loc.region());
                            break;
                        case SPTUtil.FRAGMENT_CONS:
                            // it's a Fragment("sometext", <TailPart>)
                            IStrategoTerm textTerm = term.getSubterm(0);
                            loc = traceService.location(textTerm);
                            if(loc == null) {
                                // TODO is this ok? or should we fail more gracefully?
                                throw new IllegalArgumentException(
                                    "Fragment text " + textTerm + " has no origin information.");
                            }
                            text.add(new FragmentPiece(loc.region().startOffset(), Term.asJavaString(textTerm)));
                            break;
                        case SPTUtil.TAILPART_MORE_CONS:
                            // it's a More(Selection(<marker>, "sometext", <marker>), "sometext", <Tailpart>)
                            IStrategoTerm selectionTextTerm = term.getSubterm(0).getSubterm(1);
                            loc = traceService.location(selectionTextTerm);
                            if(loc == null) {
                                // TODO is this ok? or should we fail more gracefully?
                                throw new IllegalArgumentException(
                                    "Fragment text " + selectionTextTerm + " has no origin information.");
                            }
                            text.add(
                                new FragmentPiece(loc.region().startOffset(), Term.asJavaString(selectionTextTerm)));
                            IStrategoTerm moreTextTerm = term.getSubterm(1);
                            loc = traceService.location(moreTextTerm);
                            if(loc == null) {
                                // TODO is this ok? or should we fail more gracefully?
                                throw new IllegalArgumentException(
                                    "Fragment text " + moreTextTerm + " has no origin information.");
                            }
                            text.add(new FragmentPiece(loc.region().startOffset(), Term.asJavaString(moreTextTerm)));
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
