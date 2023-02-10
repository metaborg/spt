package org.metaborg.mbt.core.model.expectations;

import org.metaborg.core.action.ITransformGoal;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;

import javax.annotation.Nullable;

/**
 * A generic transformation for 'transform [goal] on [int] to [fragment]'.
 */
public class TransformExpectation extends AToPartExpectation {

    private final ITransformGoal goal;
    @Nullable private final Integer selection;
    @Nullable private final ISourceRegion selectionRegion;

    public TransformExpectation(ISourceRegion region, ITransformGoal goal, @Nullable Integer selection,
        @Nullable ISourceRegion selectionRegion, IFragment outputFragment) {
        this(region, goal, selection, selectionRegion, outputFragment, null, null);
    }

    public TransformExpectation(ISourceRegion region, ITransformGoal goal, @Nullable Integer selection,
        @Nullable ISourceRegion selectionRegion, @Nullable IFragment outputFragment, @Nullable String langName,
        @Nullable ISourceRegion langRegion) {
        super(region, outputFragment, langName, langRegion);
        this.goal = goal;
        this.selection = selection;
        this.selectionRegion = selectionRegion;
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
}
