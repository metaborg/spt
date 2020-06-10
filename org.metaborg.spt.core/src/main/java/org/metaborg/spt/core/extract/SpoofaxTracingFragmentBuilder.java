package org.metaborg.spt.core.extract;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.Fragment;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.IFragment.FragmentPiece;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.SPTUtil;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;
import org.spoofax.terms.TermVisitor;

import com.google.inject.Inject;
import org.spoofax.terms.util.TermUtils;

/**
 * A builder for IFragments from the AST nodes of a Spoofax SPT test suite specification.
 * 
 * Uses the ISpoofaxTracingService to construct the regions for the selections of a Fragment.
 */
public class SpoofaxTracingFragmentBuilder implements ISpoofaxFragmentBuilder {
    private final ISpoofaxTracingService traceService;

    private IStrategoTerm fixtureTerm = null;
    private IStrategoTerm fragmentTerm;
    private FileObject resource = null;
    private IProject project = null;

    @Inject public SpoofaxTracingFragmentBuilder(ISpoofaxTracingService traceService) {
        this.traceService = traceService;
    }

    @Override public ISpoofaxFragmentBuilder withFixture(IStrategoTerm fragmentFixture) {
        this.fixtureTerm = fragmentFixture;
        return this;
    }

    @Override public ISpoofaxFragmentBuilder withResource(FileObject resource) {
        this.resource = resource;
        return this;
    }

    @Override public ISpoofaxFragmentBuilder withProject(IProject project) {
        this.project = project;
        return this;
    }

    @Override public ISpoofaxFragmentBuilder withFragment(IStrategoTerm fragment) {
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

        // get the region of the fragment
        ISourceLocation fragmentLocation = traceService.location(fragmentTerm);
        if(fragmentLocation == null) {
            throw new IllegalArgumentException("The given fragment has no origin location.");
        }
        ISourceRegion region = fragmentLocation.region();
        
        // get the text of the fixture before the fragment
        final List<FragmentPiece> text = new LinkedList<>();
        if (fixtureTerm != null) {
            // Fixture(openmarker, text1, openmarker, closemarker, text2, closemarker)
            final IStrategoTerm text1Term = fixtureTerm.getSubterm(1);
            final ISourceLocation loc = traceService.location(text1Term);
            if (loc == null) {
                throw new IllegalArgumentException("No origin info for test fixture text: " + text1Term);
            }
            text.add(new FragmentPiece(loc.region().startOffset(), TermUtils.toJavaString(text1Term)));
        }
        
        // get the text and selections
        final List<ISourceRegion> selections = new ArrayList<>();
        new TermVisitor() {

            /*
             * Because we visit left to right, top to bottom, we can collect all selections and fragment text during
             * this visit.
             */
            @Override public void preVisit(IStrategoTerm term) {
                if(TermUtils.isAppl(term)) {
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
                            text.add(new FragmentPiece(loc.region().startOffset(), TermUtils.toJavaString(textTerm)));
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
                                new FragmentPiece(loc.region().startOffset(), TermUtils.toJavaString(selectionTextTerm)));
                            IStrategoTerm moreTextTerm = term.getSubterm(1);
                            loc = traceService.location(moreTextTerm);
                            if(loc == null) {
                                // TODO is this ok? or should we fail more gracefully?
                                throw new IllegalArgumentException(
                                    "Fragment text " + moreTextTerm + " has no origin information.");
                            }
                            text.add(new FragmentPiece(loc.region().startOffset(), TermUtils.toJavaString(moreTextTerm)));
                            break;
                        default:
                            // nothing to do
                    }
                }
            }
        }.visit(fragmentTerm);

        if (fixtureTerm != null) {
            // Fixture(openmarker, text1, openmarker, closemarker, text2, closemarker)
            final IStrategoTerm text2Term = fixtureTerm.getSubterm(4);
            final ISourceLocation loc = traceService.location(text2Term);
            if (loc == null) {
                throw new IllegalArgumentException("No origin info for test fixture text: " + text2Term);
            }
            text.add(new FragmentPiece(loc.region().startOffset(), TermUtils.toJavaString(text2Term)));
        }
        
        return new Fragment(region, selections, text, resource, project);
    }

}
