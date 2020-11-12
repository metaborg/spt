package org.metaborg.mbt.core.model.expectations;

import org.metaborg.core.source.ISourceRegion;

/**
 * A special expectation that indicates that no expectation could be found.
 *
 * This is used for error reporting. It's a hacky way for SPT to register that it failed to find
 * a proper ITestExpectation for a certain expectation AST node.
 *
 * DO NOT USE THIS FOR YOUR OWN PROVIDER OR EVALUATOR!!!
 */
public class NoExpectationError extends ATestExpectation {

    /**
     * Initializes a new instance of the {@link NoExpectationError} class.
     *
     * @param region the source location of the expectation term
     */
    public NoExpectationError(ISourceRegion region) {
        super(region);
    }

}
