package org.metaborg.mbt.core.model.expectations;

import javax.annotation.Nullable;

import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;

/**
 * An expectation for running Stratego strategies.
 * 
 * As the usage of Stratego as transformation language is kind of Spoofax specific, the expectation is not very generic.
 * But for now it's fine where it is.
 */
public class RunStrategoExpectation extends AToPartExpectation {

    private final String strategy;
    private final ISourceRegion stratRegion;
    @Nullable private final Integer selection;
    @Nullable private final ISourceRegion selectionRegion;

    public RunStrategoExpectation(ISourceRegion region, String stratName, ISourceRegion stratRegion,
        @Nullable Integer selection, @Nullable ISourceRegion selectionRegion) {
        this(region, stratName, stratRegion, selection, selectionRegion, null, null, null);
    }

    public RunStrategoExpectation(ISourceRegion region, String stratName, ISourceRegion stratRegion,
        @Nullable Integer selection, @Nullable ISourceRegion selectionRegion, IFragment outputFragment,
        @Nullable String langName, @Nullable ISourceRegion langRegion) {
        super(region, outputFragment, langName, langRegion);
        this.strategy = stratName;
        this.stratRegion = stratRegion;
        this.selection = selection;
        this.selectionRegion = selectionRegion;
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
     * The region of the reference to the selection on which the strategy should be executed.
     * 
     * I.e. the region of the '#n' part.
     * 
     * May be null, iff the selection is null.
     */
    public @Nullable ISourceRegion selectionRegion() {
        return selectionRegion;
    }
}
