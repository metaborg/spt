package org.metaborg.spt.core.extract.expectations;

import javax.annotation.Nullable;

import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;
import org.metaborg.mbt.core.model.expectations.RunStrategoExpectation;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.SPTUtil;
import org.metaborg.spt.core.extract.ISpoofaxFragmentBuilder;
import org.metaborg.spt.core.extract.ISpoofaxTestExpectationProvider;
import org.metaborg.spt.core.run.FragmentUtil;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;

import com.google.inject.Inject;

/**
 * Runs Stratego strategies on selections or the entire test and compares results.
 * 
 * For now, we only run against the AST nodes of the analyzed AST.
 */
public class RunStrategoExpectationProvider implements ISpoofaxTestExpectationProvider {

    private static final String RUN = "Run";
    private static final String RUN_TO = "RunTo";

    private final ISpoofaxFragmentBuilder fragmentBuilder;
    private final ISpoofaxTracingService traceService;

    private final FragmentUtil fragmentUtil;

    @Inject public RunStrategoExpectationProvider(ISpoofaxFragmentBuilder fragmentBuilder,
        ISpoofaxTracingService traceService, FragmentUtil fragmentUtil) {
        this.fragmentBuilder = fragmentBuilder;
        this.traceService = traceService;

        this.fragmentUtil = fragmentUtil;
    }

    @Override public boolean canEvaluate(IFragment inputFragment, IStrategoTerm expectationTerm) {
        String cons = SPTUtil.consName(expectationTerm);
        switch(cons) {
            case RUN:
                return expectationTerm.getSubtermCount() == 2 && Term.isTermString(getStrategyTerm(expectationTerm))
                    && checkOptionalOnPart(getOnPartTerm(expectationTerm));
            case RUN_TO:
                return expectationTerm.getSubtermCount() == 3 && Term.isTermString(getStrategyTerm(expectationTerm))
                    && checkOptionalOnPart(getOnPartTerm(expectationTerm))
                    && FragmentUtil.checkToPart(getToPartTerm(expectationTerm));
            default:
                return false;
        }
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        ISourceLocation loc = traceService.location(expectationTerm);
        ISourceRegion region = loc == null ? inputFragment.getRegion() : loc.region();

        // Run(strat, optional onPart) or RunTo(strat, optOnPart, toPart)
        final String cons = SPTUtil.consName(expectationTerm);
        final IStrategoTerm stratTerm = getStrategyTerm(expectationTerm);
        final String strategy = Term.asJavaString(stratTerm);
        final ISourceLocation stratLoc = traceService.location(stratTerm);
        final @Nullable IStrategoTerm onTerm = SPTUtil.getOptionValue(getOnPartTerm(expectationTerm));
        final Integer selection;
        final ISourceRegion selectionRegion;
        if(onTerm != null) {
            // on #<int> was present
            selection = Term.asJavaInt(onTerm);
            final ISourceLocation selLoc = traceService.location(onTerm);
            if(selLoc == null) {
                selectionRegion = region;
            } else {
                selectionRegion = selLoc.region();
            }
        } else {
            // the onPart was None()
            selection = null;
            selectionRegion = null;
        }

        if(RUN.equals(cons)) {
            // This is a Run term
            return new RunStrategoExpectation(region, strategy, stratLoc.region(), selection, selectionRegion);
        } else {
            // This is a RunTo term
            final IStrategoTerm toPart = getToPartTerm(expectationTerm);
            final String langName = FragmentUtil.toPartLangName(toPart);
            final ISourceRegion langRegion = fragmentUtil.toPartLangNameRegion(toPart);
            final IFragment outputFragment = fragmentBuilder.withFragment(FragmentUtil.toPartFragment(toPart))
                .withProject(inputFragment.getProject()).withResource(inputFragment.getResource()).build();
            return new RunStrategoExpectation(region, strategy, stratLoc.region(), selection, selectionRegion,
                outputFragment, langName, langRegion);
        }
    }

    private IStrategoTerm getStrategyTerm(IStrategoTerm expectation) {
        return expectation.getSubterm(0);
    }

    private IStrategoTerm getOnPartTerm(IStrategoTerm expectation) {
        return expectation.getSubterm(1);
    }

    private IStrategoTerm getToPartTerm(IStrategoTerm expectation) {
        return expectation.getSubterm(2);
    }

    // Check if the given term is an optional OnPart
    // i.e. None() or Some(<int>)
    protected static boolean checkOptionalOnPart(IStrategoTerm term) {
        if(SPTUtil.checkOption(term)) {
            final IStrategoTerm onPart = SPTUtil.getOptionValue(term);
            if(onPart == null) {
                // it's a None()
                return true;
            } else {
                // it's a Some(int)
                return Term.isTermInt(onPart);
            }
        } else {
            return false;
        }
    }

}
