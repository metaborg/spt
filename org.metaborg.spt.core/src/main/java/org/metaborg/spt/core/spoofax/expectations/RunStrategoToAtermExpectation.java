package org.metaborg.spt.core.spoofax.expectations;

import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spt.core.expectations.ATestExpectation;
import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * An expectation for running Stratego strategies and comparing the result to an ATerm AST.
 */
public class RunStrategoToAtermExpectation extends ATestExpectation {

    private final String strategy;
    private final ISourceRegion stratRegion;
    private final IStrategoTerm expectedResult;

    public RunStrategoToAtermExpectation(ISourceRegion region, String stratName, ISourceRegion stratRegion,
        IStrategoTerm expectedResult) {
        super(region);
        this.strategy = stratName;
        this.stratRegion = stratRegion;
        this.expectedResult = expectedResult;
    }

    /**
     * The name of the argumentless Stratego strategy that should be executed.
     */
    public String strategy() {
        return strategy;
    }

    /**
     * The region spanned by the name of the strategy.
     */
    public ISourceRegion strategyRegion() {
        return stratRegion;
    }

    public IStrategoTerm expectedResult() {
        return expectedResult;
    }
}
