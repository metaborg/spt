package org.metaborg.spt.core.spoofax.expectations;

import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.IFragment;
import org.metaborg.spt.core.ITestExpectation;
import org.metaborg.spt.core.spoofax.ISpoofaxTestExpectationProvider;
import org.metaborg.spt.core.util.SPTUtil;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;

import com.google.inject.Inject;

/**
 * Runs Stratego strategies on selections or the entire test and compares results to an ATerm AST.
 */
public class RunStrategoToAtermExpectationProvider implements ISpoofaxTestExpectationProvider {

    // RunToAterm("strategy", ToAterm(ast))
    private static final String RUN_TO = "RunToAterm";

    private final ISpoofaxTracingService traceService;


    @Inject public RunStrategoToAtermExpectationProvider(ISpoofaxTracingService traceService) {
        this.traceService = traceService;
    }

    @Override public boolean canEvaluate(IFragment inputFragment, IStrategoTerm expectationTerm) {
        String cons = SPTUtil.consName(expectationTerm);
        return Term.isTermString(expectationTerm.getSubterm(0)) && RUN_TO.equals(cons)
            && expectationTerm.getSubtermCount() == 2 && expectationTerm.getSubterm(1).getSubtermCount() == 1;
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        ISourceLocation loc = traceService.location(expectationTerm);
        ISourceRegion region = loc == null ? inputFragment.getRegion() : loc.region();

        final IStrategoTerm stratTerm = expectationTerm.getSubterm(0);
        final String strategy = Term.asJavaString(stratTerm);
        final IStrategoTerm toAtermPart = expectationTerm.getSubterm(1);
        return new RunStrategoToAtermExpectation(region, strategy, loc.region(), toAtermPart.getSubterm(0));
    }

}
