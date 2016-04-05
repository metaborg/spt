package org.metaborg.spt.core;

import java.util.ArrayList;
import java.util.List;

import org.metaborg.core.source.ISourceRegion;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.TermVisitor;

import com.google.inject.Inject;

public abstract class AbstractFragment implements IFragment {

    protected final IStrategoTerm fragment;
    protected final List<ISourceRegion> selections = new ArrayList<>();

    @Inject public AbstractFragment(IStrategoTerm fragment) {
        this.fragment = fragment;
        new TermVisitor() {

            @Override public void preVisit(IStrategoTerm term) {
                if(Tools.isTermAppl(term)) {
                    if(SPTUtil.SELECTION_CONS.equals(SPTUtil.consName(term))) {
                        selections.add(SPTUtil.getRegion(term));
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

}
