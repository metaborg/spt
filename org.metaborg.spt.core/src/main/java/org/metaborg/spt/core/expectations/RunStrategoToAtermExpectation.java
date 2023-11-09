package org.metaborg.spt.core.expectations;

import jakarta.annotation.Nullable;

import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.expectations.ATestExpectation;
import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * An expectation for running Stratego strategies and comparing the result to an ATerm AST.
 */
public class RunStrategoToAtermExpectation extends ATestExpectation {

    private final String strategy;
    private final ISourceRegion stratRegion;
    @Nullable private final Integer selection;
    @Nullable private final ISourceRegion selectionRegion;
    private final IStrategoTerm expectedResult;

    public RunStrategoToAtermExpectation(ISourceRegion region, String stratName, ISourceRegion stratRegion,
        @Nullable Integer selection, @Nullable ISourceRegion selectionRegion, IStrategoTerm expectedResult) {
        super(region);
        this.strategy = stratName;
        this.stratRegion = stratRegion;
        this.selection = selection;
        this.selectionRegion = selectionRegion;
        this.expectedResult = expectedResult;
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
     * The reference to the selection on which the strategy should be executed.
     * 
     * May be null, iff the strategy should be executed on the entire fragment.
     */
    public @Nullable Integer selection() {
        return selection;
    }

    /**
     * The region of the reference to the selection on which the strategy should be executed.
     * 
     * May be null, iff the selection is null.
     */
    public @Nullable ISourceRegion selectionRegion() {
        return selectionRegion;
    }

    public IStrategoTerm expectedResult() {
        return expectedResult;
    }
}
