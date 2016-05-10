package org.metaborg.mbt.core.model.expectations;

import org.metaborg.core.source.ISourceRegion;

public class ATestExpectation implements ITestExpectation {

    private final ISourceRegion region;

    public ATestExpectation(ISourceRegion region) {
        this.region = region;
    }

    @Override public ISourceRegion region() {
        return region;
    }
}
