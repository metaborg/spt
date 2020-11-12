package org.metaborg.spt.core.model.expectations;

import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.expectations.NoExpectationError;
import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * A special expectation that indicates that no expectation could be found.
 *
 * This is used for error reporting.
 */
public final class SpoofaxNoExpectationError extends NoExpectationError {
    private final IStrategoTerm term;

    /**
     * Initializes a new instance of the {@link SpoofaxNoExpectationError} class.
     *
     * @param region the source location of the expectation term
     * @param term the expectation term
     */
    public SpoofaxNoExpectationError(ISourceRegion region, IStrategoTerm term) {
        super(region);
        this.term = term;
    }

    /**
     * Gets the expectation term for which no expectation could be found.
     *
     * @return the expectation term
     */
    public IStrategoTerm getTerm() {
        return this.term;
    }
}
