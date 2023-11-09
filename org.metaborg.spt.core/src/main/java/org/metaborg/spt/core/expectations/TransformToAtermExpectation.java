package org.metaborg.spt.core.expectations;

import org.metaborg.core.action.ITransformGoal;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.expectations.ATestExpectation;
import org.spoofax.interpreter.terms.IStrategoTerm;

import jakarta.annotation.Nullable;

/**
 * A Spoofax specific transformation for 'transform [goal] to [ATerm]'.
 */
public class TransformToAtermExpectation extends ATestExpectation {

    private final ITransformGoal goal;
    @Nullable private final Integer selection;
    @Nullable private final ISourceRegion selectionRegion;
    private final IStrategoTerm expectedResult;

    public TransformToAtermExpectation(ISourceRegion region, @Nullable Integer selection,
        @Nullable ISourceRegion selectionRegion, ITransformGoal goal, IStrategoTerm expectedResult) {
        super(region);
        this.goal = goal;
        this.selection = selection;
        this.selectionRegion = selectionRegion;
        this.expectedResult = expectedResult;
    }

    /**
     * The transformation goal that we should execute.
     */
    public ITransformGoal goal() {
        return goal;
    }

    @Nullable public Integer selection() {
        return selection;
    }

    @Nullable public ISourceRegion selectionRegion() {
        return selectionRegion;
    }

    public IStrategoTerm expectedResult() {
        return expectedResult;
    }
}
