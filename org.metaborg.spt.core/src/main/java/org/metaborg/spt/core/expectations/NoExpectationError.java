package org.metaborg.spt.core.expectations;

import org.metaborg.core.source.ISourceRegion;

/**
 * DO NOT USE THIS FOR YOUR OWN PROVIDER OR EVALUATOR!!!
 * 
 * It's a hacky way for SPT to register that it failed to find a proper ITestExpectation for a certain expectation AST
 * node.
 */
public class NoExpectationError extends ATestExpectation {

    public NoExpectationError(ISourceRegion region) {
        super(region);
    }

}
