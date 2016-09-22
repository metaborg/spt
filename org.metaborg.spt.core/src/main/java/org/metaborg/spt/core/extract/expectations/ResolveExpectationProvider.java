package org.metaborg.spt.core.extract.expectations;

import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;
import org.metaborg.mbt.core.model.expectations.ResolveExpectation;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.SPTUtil;
import org.metaborg.spt.core.extract.ISpoofaxTestExpectationProvider;
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
        return checkResolve(expectationTerm) || checkResolveTo(expectationTerm);
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        final ISourceLocation loc = traceService.location(expectationTerm);
        final ISourceRegion region = loc == null ? inputFragment.getRegion() : loc.region();

        final String cons = SPTUtil.consName(expectationTerm);
        final IStrategoTerm refTerm = getReferenceTerm(expectationTerm);
        final ISourceLocation refLoc = traceService.location(refTerm);
        if(RESOLVE.equals(cons)) {
            return new ResolveExpectation(region, Term.asJavaInt(refTerm), refLoc.region());
        } else {
            final IStrategoTerm defTerm = getDefinitionTerm(expectationTerm);
            final ISourceLocation defLoc = traceService.location(defTerm);
            return new ResolveExpectation(region, Term.asJavaInt(refTerm), refLoc.region(), Term.asJavaInt(defTerm),
                defLoc.region());
        }
    }

    // Resolve(int) or ResolveTo(int, int)
    private IStrategoTerm getReferenceTerm(IStrategoTerm expectationTerm) {
        return expectationTerm.getSubterm(0);
    }

    // ResolveTo(int, int)
    private IStrategoTerm getDefinitionTerm(IStrategoTerm expectationTerm) {
        return expectationTerm.getSubterm(1);
    }

    // Resolve(int)
    private boolean checkResolve(IStrategoTerm expectationTerm) {
        if(!RESOLVE.equals(SPTUtil.consName(expectationTerm)) || expectationTerm.getSubtermCount() != 1) {
            return false;
        }
        if(!Term.isTermInt(getReferenceTerm(expectationTerm))) {
            return false;
        }
        return true;
    }

    // ResolveTo(int, int)
    private boolean checkResolveTo(IStrategoTerm expectationTerm) {
        if(!TO.equals(SPTUtil.consName(expectationTerm)) || expectationTerm.getSubtermCount() != 2) {
            return false;
        }
        if(!Term.isTermInt(getReferenceTerm(expectationTerm))) {
            return false;
        }
        if(!Term.isTermInt(getDefinitionTerm(expectationTerm))) {
            return false;
        }
        return true;
    }
}
