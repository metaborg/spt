package org.metaborg.spt.core.spoofax.expectations;

import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.IFragment;
import org.metaborg.spt.core.ITestExpectation;
import org.metaborg.spt.core.expectations.ResolveExpectation;
import org.metaborg.spt.core.spoofax.ISpoofaxTestExpectationProvider;
import org.metaborg.spt.core.util.SPTUtil;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;

import com.google.inject.Inject;

/**
 * Runs the ISpoofaxResolverService at the start offset of a selection to see if it resolved properly.
 * 
 * Note that we require the test's analysis context to still be valid and open, as it is reused to run the resolver
 * service.
 */
public class ResolveExpectationProvider implements ISpoofaxTestExpectationProvider {

    private static final String RESOLVE = "Resolve";
    private static final String TO = "ResolveTo";

    private final ISpoofaxTracingService traceService;

    @Inject public ResolveExpectationProvider(ISpoofaxTracingService traceService) {
        this.traceService = traceService;
    }

    @Override public boolean canEvaluate(IFragment inputFragment, IStrategoTerm expectationTerm) {
        String cons = SPTUtil.consName(expectationTerm);
        return RESOLVE.equals(cons) && expectationTerm.getSubtermCount() == 1
            && Term.isTermInt(expectationTerm.getSubterm(0))
            || TO.equals(cons) && expectationTerm.getSubtermCount() == 2
                && Term.isTermInt(expectationTerm.getSubterm(0)) && Term.isTermInt(expectationTerm.getSubterm(1));
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        ISourceLocation loc = traceService.location(expectationTerm);
        ISourceRegion region = loc == null ? inputFragment.getRegion() : loc.region();

        String cons = SPTUtil.consName(expectationTerm);
        int num1 = Term.asJavaInt(expectationTerm.getSubterm(0));
        ISourceLocation loc1 = traceService.location(expectationTerm.getSubterm(0));
        if(RESOLVE.equals(cons)) {
            return new ResolveExpectation(region, num1, loc1.region());
        } else {
            ISourceLocation loc2 = traceService.location(expectationTerm.getSubterm(1));
            return new ResolveExpectation(region, num1, loc1.region(), Term.asJavaInt(expectationTerm.getSubterm(1)),
                loc2.region());
        }
    }

}
