package org.metaborg.spt.core.extract.expectations;

import javax.annotation.Nullable;

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

    // RunToAterm("strategy", optional onPart(int), ToAterm(ast))
    private static final String RUN_TO = "RunToAterm";
    private static final String TO_ATERM = "ToAterm";

    private final ISpoofaxTracingService traceService;


    @Inject public RunStrategoToAtermExpectationProvider(ISpoofaxTracingService traceService) {
        this.traceService = traceService;
    }

    @Override public boolean canEvaluate(IFragment inputFragment, IStrategoTerm expectationTerm) {
        return checkRunToAterm(expectationTerm);
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        ISourceLocation loc = traceService.location(expectationTerm);
        ISourceRegion region = loc == null ? inputFragment.getRegion() : loc.region();

        final IStrategoTerm stratTerm = getStrategyTerm(expectationTerm);
        final String strategy = Term.asJavaString(stratTerm);

        final @Nullable IStrategoTerm onTerm = SPTUtil.getOptionValue(getOptionalOnPartTerm(expectationTerm));
        final Integer selection;
        final ISourceRegion selectionRegion;
        if(onTerm == null) {
            // the optional onPart was None()
            selection = null;
            selectionRegion = null;
        } else {
            // the optional onPart was Some(onTerm)
            selection = Term.asJavaInt(onTerm);
            final ISourceLocation selLoc = traceService.location(onTerm);
            if(selLoc == null) {
                selectionRegion = region;
            } else {
                selectionRegion = selLoc.region();
            }
        }
        final IStrategoTerm toAtermPart = getToAtermTerm(expectationTerm);
        return new RunStrategoToAtermExpectation(region, strategy, loc.region(), selection, selectionRegion,
            toAtermPart.getSubterm(0));
    }

    private IStrategoTerm getStrategyTerm(IStrategoTerm expectationTerm) {
        return expectationTerm.getSubterm(0);
    }

    private IStrategoTerm getOptionalOnPartTerm(IStrategoTerm expectationTerm) {
        return expectationTerm.getSubterm(1);
    }

    private IStrategoTerm getToAtermTerm(IStrategoTerm expectationTerm) {
        return expectationTerm.getSubterm(2);
    }

    /**
     * Check if the given term is a RunToAterm term that we can handle.
     */
    private boolean checkRunToAterm(IStrategoTerm expectationTerm) {
        // RunToAterm("strategy", optional onPart(int), ToAterm(ast))
        if(!RUN_TO.equals(SPTUtil.consName(expectationTerm)) || expectationTerm.getSubtermCount() != 3) {
            return false;
        }

        // check strategy name
        if(!Term.isTermString(getStrategyTerm(expectationTerm))) {
            return false;
        }

        // check optional OnPart
        if(!RunStrategoExpectationProvider.checkOptionalOnPart(getOptionalOnPartTerm(expectationTerm))) {
            return false;
        }

        if(!checkToAterm(getToAtermTerm(expectationTerm))) {
            return false;
        }

        return true;
    }

    /**
     * Check if the given term is a ToAterm term.
     * 
     * For now we do not check the entire term, only the constructor.
     */
    protected static boolean checkToAterm(IStrategoTerm toAterm) {
        // I don't feel like checking the entire ATerm AST.
        if(!TO_ATERM.equals(SPTUtil.consName(toAterm)) || toAterm.getSubtermCount() != 1) {
            return false;
        }
        return true;
    }

}
