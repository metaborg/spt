package org.metaborg.mbt.core.model.expectations;

import org.metaborg.core.source.ISourceRegion;

public class NotExpectation extends ATestExpectation {
    public final ITestExpectation subExpectation;


    public NotExpectation(ISourceRegion region, ITestExpectation subExpectation) {
        super(region);
        this.subExpectation = subExpectation;
    }
}
