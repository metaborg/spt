package org.metaborg.mbt.core.model.expectations;

import javax.annotation.Nullable;

import org.metaborg.core.source.ISourceRegion;

/**
 * A generic expectation for 'resolve #i' and 'resolve #i to #j'.
 */
public class ResolveExpectation extends ATestExpectation {

    private final int fromIndex;
    private final ISourceRegion fromRegion;

    private final int toIndex;
    @Nullable private final ISourceRegion toRegion;

    public ResolveExpectation(ISourceRegion region, int fromIndex, ISourceRegion fromRegion) {
        this(region, fromIndex, fromRegion, -1, null);
    }

    public ResolveExpectation(ISourceRegion region, int fromIndex, ISourceRegion fromRegion, int toIndex,
        @Nullable ISourceRegion toRegion) {
        super(region);
        this.fromIndex = fromIndex;
        this.fromRegion = fromRegion;
        this.toIndex = toIndex;
        this.toRegion = toRegion;
    }

    /**
     * The number (as supplied in the expectation) of the selection where resolution should take place.
     */
    public int from() {
        return fromIndex;
    }

    /**
     * The source region of the first reference to a selection.
     */
    public ISourceRegion fromRegion() {
        return fromRegion;
    }

    /**
     * The number (as supplied in the expectation) of the selection where the resolution should resolve to.
     * 
     * May be -1 if it wasn't present in the expectation.
     */
    public int to() {
        return toIndex;
    }

    /**
     * The source region of the second reference to a selection.
     */
    public @Nullable ISourceRegion toRegion() {
        return toRegion;
    }
}
