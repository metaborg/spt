package org.metaborg.mbt.core.model.expectations;

import javax.annotation.Nullable;

import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;

/**
 * An expectation for running Stratego strategies.
 * 
 * As the usage of Stratego as transformation language is kind of Spoofax specific, the expectation is not very generic.
 * But for now it's fine where it is.
 */
public class RunStrategoExpectation extends AToPartExpectation {

    private final String strategy;
    private final ISourceRegion stratRegion;

    public RunStrategoExpectation(ISourceRegion region, String stratName, ISourceRegion stratRegion) {
        this(region, stratName, stratRegion, null, null, null);
    }

    public RunStrategoExpectation(ISourceRegion region, String stratName, ISourceRegion stratRegion,
        IFragment outputFragment, @Nullable String langName, @Nullable ISourceRegion langRegion) {
        super(region, outputFragment, langName, langRegion);
        this.strategy = stratName;
        this.stratRegion = stratRegion;
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
}
