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
import org.spoofax.terms.Term;

import com.google.inject.Inject;

public class TransformToAtermExpectationProvider implements ISpoofaxTestExpectationProvider {

    // TransformToAterm("goal", ToAterm(ast))
    private static final String TRANSFORM = "TransformToAterm";

    private final ISpoofaxTracingService traceService;

    @Inject public TransformToAtermExpectationProvider(ISpoofaxTracingService traceService) {
        this.traceService = traceService;
    }

    @Override public boolean canEvaluate(IFragment inputFragment, IStrategoTerm expectationTerm) {
        String cons = SPTUtil.consName(expectationTerm);
        return cons != null && TRANSFORM.equals(cons) && expectationTerm.getSubtermCount() == 2
            && Term.isTermString(expectationTerm.getSubterm(0))
            && TransformExpectationProvider.goalNames(Term.asJavaString(expectationTerm.getSubterm(0))).size() > 0;
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        ISourceLocation loc = traceService.location(expectationTerm);
        ISourceRegion region = loc == null ? inputFragment.getRegion() : loc.region();

        // It's a Transform("goal", ToPart(...))
        String unQuotedGoalStr = Term.asJavaString(expectationTerm.getSubterm(0));
        if(unQuotedGoalStr.length() > 2 && unQuotedGoalStr.startsWith("\"") && unQuotedGoalStr.endsWith("\"")) {
            unQuotedGoalStr = unQuotedGoalStr.substring(1, unQuotedGoalStr.length() - 1);
        }
        final List<String> goalNames = TransformExpectationProvider.goalNames(unQuotedGoalStr);

        final ITransformGoal goal;
        if(goalNames.size() > 1) {
            goal = new NamedGoal(goalNames);
        } else if(goalNames.size() == 0) {
            throw new IllegalArgumentException(
                "Can't create an evaluator for a transformation expectation without a transformation goal.");
        } else {
            goal = new EndNamedGoal(goalNames.get(0));
        }

        final IStrategoTerm toAtermPart = expectationTerm.getSubterm(1);
        return new TransformToAtermExpectation(region, goal, toAtermPart.getSubterm(0));
    }
}
