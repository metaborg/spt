package org.metaborg.spt.core.expectations;

import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spt.core.ITestExpectation;

public class ATestExpectation implements ITestExpectation {

    private final ISourceRegion region;

    public ATestExpectation(ISourceRegion region) {
        this.region = region;
    }

    @Override public ISourceRegion region() {
        return region;
    }
}
