package org.metaborg.spt.core.extract.expectations;

import java.util.List;

import org.metaborg.core.action.EndNamedGoal;
import org.metaborg.core.action.ITransformGoal;
import org.metaborg.core.action.NamedGoal;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.SPTUtil;
import org.metaborg.spt.core.expectations.TransformToAtermExpectation;
import org.metaborg.spt.core.extract.ISpoofaxTestExpectationProvider;
import org.spoofax.interpreter.terms.IStrategoTerm;

import org.spoofax.terms.util.TermUtils;

public class TransformToAtermExpectationProvider implements ISpoofaxTestExpectationProvider {

    // TransformToAterm("goal", OnPart(idx), ToAterm(ast))
    private static final String TRANSFORM = "TransformToAterm";

    private final ISpoofaxTracingService traceService;

    @jakarta.inject.Inject @javax.inject.Inject public TransformToAtermExpectationProvider(ISpoofaxTracingService traceService) {
        this.traceService = traceService;
    }

    @Override public boolean canEvaluate(IFragment inputFragment, IStrategoTerm expectationTerm) {
        return checkTransformToAterm(expectationTerm);
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        final ISourceLocation loc = traceService.location(expectationTerm);
        final ISourceRegion region = loc == null ? inputFragment.getRegion() : loc.region();

        // It's a Transform("goal", ToPart(...))
        String unQuotedGoalStr = TermUtils.toJavaString(getGoalTerm(expectationTerm));
        if(unQuotedGoalStr.length() > 2 && unQuotedGoalStr.startsWith("\"") && unQuotedGoalStr.endsWith("\"")) {
            unQuotedGoalStr = unQuotedGoalStr.substring(1, unQuotedGoalStr.length() - 1);
        }
        final List<String> goalNames = TransformExpectationProvider.goalNames(unQuotedGoalStr);

        final IStrategoTerm onPartTerm = getSelectionTerm(expectationTerm);
        final IStrategoTerm selectionTerm = SPTUtil.getOptionValue(onPartTerm);

        final Integer selection;
        final ISourceRegion selectionRegion;

        if(selectionTerm == null) {
            selection = null;
            selectionRegion = null;
        } else {
            selection = TermUtils.toJavaInt(selectionTerm);
            final ISourceLocation selLoc = traceService.location(selectionTerm);
            selectionRegion = selLoc == null ? region : selLoc.region();
        }

        final ITransformGoal goal;
        if(goalNames.size() > 1) {
            goal = new NamedGoal(goalNames);
        } else if(goalNames.size() == 0) {
            throw new IllegalArgumentException(
                "Can't create an evaluator for a transformation expectation without a transformation goal.");
        } else {
            goal = new EndNamedGoal(goalNames.get(0));
        }

        final IStrategoTerm toAtermPart = getToAtermTerm(expectationTerm);
        return new TransformToAtermExpectation(region, selection, selectionRegion, goal, toAtermPart.getSubterm(0));
    }

    private IStrategoTerm getGoalTerm(IStrategoTerm expectationTerm) {
        return expectationTerm.getSubterm(0);
    }

    private IStrategoTerm getSelectionTerm(IStrategoTerm expectationTerm) {
        return expectationTerm.getSubterm(1);
    }

    private IStrategoTerm getToAtermTerm(IStrategoTerm expectationTerm) {
        return expectationTerm.getSubterm(2);
    }

    /**
     * Check if the given term is a TransformToAterm term that we can handle.
     */
    private boolean checkTransformToAterm(IStrategoTerm expectationTerm) {
        // TransformToAterm("goal", OnPart(idx), ToAterm(ast))
        if(!TRANSFORM.equals(SPTUtil.consName(expectationTerm)) || expectationTerm.getSubtermCount() != 3) {
            return false;
        }

        // check the goal term
        final IStrategoTerm goalTerm = getGoalTerm(expectationTerm);
        if(!TermUtils.isString(goalTerm)) {
            return false;
        }
        if(TransformExpectationProvider.goalNames(TermUtils.toJavaString(goalTerm)).size() <= 0) {
            return false;
        }

        // shallow check of the ToAterm term
        if(!RunStrategoToAtermExpectationProvider.checkToAterm(getToAtermTerm(expectationTerm))) {
            return false;
        }

        return true;
    }
}
