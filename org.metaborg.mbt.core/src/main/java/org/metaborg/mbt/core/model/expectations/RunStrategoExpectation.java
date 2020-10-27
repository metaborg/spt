package org.metaborg.mbt.core.model.expectations;

import java.util.List;

import javax.annotation.Nullable;

import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;
import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * An expectation for running Stratego strategies.
 * 
 * As the usage of Stratego as transformation language is kind of Spoofax
 * specific, the expectation is not very generic. But for now it's fine where it
 * is.
 */
public class RunStrategoExpectation extends AToPartExpectation {

    private final String strategy;
    private final ISourceRegion stratRegion;
    @Nullable
    private final Integer selection;
    @Nullable
    private final ISourceRegion selectionRegion;
    @Nullable
    private final List<IStrategoTerm> termArguments;
    private final boolean expectedToFail;

    public RunStrategoExpectation(ISourceRegion region, String stratName, ISourceRegion stratRegion,
            @Nullable Integer selection, @Nullable ISourceRegion selectionRegion) {
        this(region, stratName, stratRegion, selection, selectionRegion, null, null, null, null, false);
    }

    public RunStrategoExpectation(ISourceRegion region, String stratName, ISourceRegion stratRegion,
            @Nullable Integer selection, @Nullable ISourceRegion selectionRegion, IFragment outputFragment,
            @Nullable String langName, @Nullable ISourceRegion langRegion, List<IStrategoTerm> termArguments,
            boolean expectedToFail) {
        super(region, outputFragment, langName, langRegion);
        this.strategy = stratName;
        this.stratRegion = stratRegion;
        this.selection = selection;
        this.selectionRegion = selectionRegion;
        this.termArguments = termArguments;
        this.expectedToFail = expectedToFail;
    }

    /**
     * The name of the argumentless Stratego strategy that should be executed.
     */
    public String strategy() {
        return strategy;
    }

    /**
     * The region spanned by the name of the strategy.
     */
    public ISourceRegion strategyRegion() {
        return stratRegion;
    }

    /**
     * The number of the selection on which the strategy should be executed.
     * 
     * May be null, if it should be executed on the entire fragment.
     */
    public @Nullable Integer selection() {
        return selection;
    }

    /**
     * The region of the reference to the selection on which the strategy should be
     * executed.
     * 
     * I.e. the region of the '#n' part.
     * 
     * May be null, iff the selection is null.
     */
    public @Nullable ISourceRegion selectionRegion() {
        return selectionRegion;
    }

    /**
     * The term arguments the strategy is called with
     * 
     * Maybe null if the strategy doesn't take any term arguments
     */
    public @Nullable List<IStrategoTerm> getArguments() {
        return termArguments;
    }

    /**
     * Flag if the strategy run is expected to fail
     * 
     * Default is false
     */
    public boolean getExpectedToFail() {
        return expectedToFail;
    }
}