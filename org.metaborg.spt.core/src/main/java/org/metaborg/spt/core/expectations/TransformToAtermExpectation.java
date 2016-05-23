package org.metaborg.spt.core.expectations;

import org.metaborg.core.action.ITransformGoal;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.expectations.ATestExpectation;
import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * A Spoofax specific transformation for 'transform [goal] to [ATerm]'.
 */
public class TransformToAtermExpectation extends ATestExpectation {

    private final ITransformGoal goal;
    private final IStrategoTerm expectedResult;

    public TransformToAtermExpectation(ISourceRegion region, ITransformGoal goal, IStrategoTerm expectedResult) {
        super(region);
        this.goal = goal;
        this.expectedResult = expectedResult;
    }

    /**
     * The transformation goal that we should execute.
     */
    public ITransformGoal goal() {
        return goal;
    }

    public IStrategoTerm expectedResult() {
        return expectedResult;
    }
}
