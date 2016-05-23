package org.metaborg.spt.core.extract;

import org.metaborg.core.source.ISourceLocation;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.expectations.HasOriginExpectation;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.SPTUtil;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Inject;

/**
 * Provider for the `has origin locations` expectation.
 */
public class HasOriginExpectationProvider implements ISpoofaxTestExpectationProvider {

    private static final String HAS_ORIGIN = "HasOrigin";

    private final ISpoofaxTracingService traceService;

    @Inject public HasOriginExpectationProvider(ISpoofaxTracingService traceService) {
        this.traceService = traceService;
    }

    @Override public boolean canEvaluate(IFragment inputFragment, IStrategoTerm expectationTerm) {
        return HAS_ORIGIN.equals(SPTUtil.consName(expectationTerm)) && expectationTerm.getSubtermCount() == 0;
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        ISourceLocation loc = traceService.location(expectationTerm);
        return new HasOriginExpectation(loc == null ? inputFragment.getRegion() : loc.region());
    }

}
