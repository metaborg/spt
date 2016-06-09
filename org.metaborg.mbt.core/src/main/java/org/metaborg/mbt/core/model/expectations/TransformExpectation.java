package org.metaborg.mbt.core.model.expectations;

import javax.annotation.Nullable;

import org.metaborg.core.action.ITransformGoal;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;

/**
 * A generic transformation for 'transform [goal] to [fragment]'.
 */
public class TransformExpectation extends AToPartExpectation {

    private final ITransformGoal goal;

    public TransformExpectation(ISourceRegion region, ITransformGoal goal, IFragment outputFragment) {
        this(region, goal, outputFragment, null, null);
    }

    public TransformExpectation(ISourceRegion region, ITransformGoal goal, IFragment outputFragment,
        @Nullable String langName, @Nullable ISourceRegion langRegion) {
        super(region, outputFragment, langName, langRegion);
        this.goal = goal;
    }

    /**
     * The transformation goal that we should execute.
     */
    public ITransformGoal goal() {
        return goal;
    }
}
