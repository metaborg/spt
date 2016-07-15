package org.metaborg.spt.core.expectations;

import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.expectations.ATestExpectation;
import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * Testing if something parses to a given ATerm AST is specific to Spoofax.
 */
public class ParseToAtermExpectation extends ATestExpectation {

    private final IStrategoTerm expectedResult;

    public ParseToAtermExpectation(ISourceRegion region, IStrategoTerm expectedResult) {
        super(region);
        this.expectedResult = expectedResult;
    }

    public IStrategoTerm expectedResult() {
        return expectedResult;
    }
}
