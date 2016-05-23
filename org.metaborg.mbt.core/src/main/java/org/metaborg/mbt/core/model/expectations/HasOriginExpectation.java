package org.metaborg.mbt.core.model.expectations;

import org.metaborg.core.source.ISourceRegion;

/**
 * Expectation that checks if all nodes in the fragment have origin information.
 */
public class HasOriginExpectation extends ATestExpectation {

    public HasOriginExpectation(ISourceRegion region) {
        super(region);
    }

}
