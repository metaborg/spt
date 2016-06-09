package org.metaborg.spt.core.extract.expectations;

import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.SPTUtil;
import org.metaborg.spt.core.expectations.RunStrategoToAtermExpectation;
import org.metaborg.spt.core.extract.ISpoofaxTestExpectationProvider;
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
            && expectationTerm.getSubtermCount() == 3 && expectationTerm.getSubterm(2).getSubtermCount() == 1;
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        ISourceLocation loc = traceService.location(expectationTerm);
        ISourceRegion region = loc == null ? inputFragment.getRegion() : loc.region();

        final IStrategoTerm stratTerm = expectationTerm.getSubterm(0);
        final String strategy = Term.asJavaString(stratTerm);
        final IStrategoTerm onTerm = expectationTerm.getSubterm(1);
        final Integer selection;
        final ISourceRegion selectionRegion;
        if(SPTUtil.SOME.equals(SPTUtil.consName(onTerm))) {
            selection = Term.asJavaInt(onTerm.getSubterm(0));
            final ISourceLocation selLoc = traceService.location(onTerm);
            if(selLoc == null) {
                selectionRegion = region;
            } else {
                selectionRegion = selLoc.region();
            }
        } else {
            selection = null;
            selectionRegion = null;
        }
        final IStrategoTerm toAtermPart = expectationTerm.getSubterm(2);
        return new RunStrategoToAtermExpectation(region, strategy, loc.region(), selection, selectionRegion,
            toAtermPart.getSubterm(0));
    }

}
