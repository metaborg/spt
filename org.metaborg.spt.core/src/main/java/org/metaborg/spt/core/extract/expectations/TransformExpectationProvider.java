package org.metaborg.spt.core.extract.expectations;

import java.util.List;

import org.metaborg.core.action.EndNamedGoal;
import org.metaborg.core.action.ITransformGoal;
import org.metaborg.core.action.NamedGoal;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;
import org.metaborg.mbt.core.model.expectations.TransformExpectation;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.SPTUtil;
import org.metaborg.spt.core.extract.ISpoofaxFragmentBuilder;
import org.metaborg.spt.core.extract.ISpoofaxTestExpectationProvider;
import org.metaborg.spt.core.run.FragmentUtil;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.spoofax.terms.util.TermUtils;

public class TransformExpectationProvider implements ISpoofaxTestExpectationProvider {

    private static final String TRANSFORM = "Transform";

    private final ISpoofaxTracingService traceService;
    private final ISpoofaxFragmentBuilder fragmentBuilder;

    private final FragmentUtil fragmentUtil;

    @Inject public TransformExpectationProvider(ISpoofaxTracingService traceService,
        ISpoofaxFragmentBuilder fragmentBuilder, FragmentUtil fragmentUtil) {
        this.traceService = traceService;
        this.fragmentBuilder = fragmentBuilder;

        this.fragmentUtil = fragmentUtil;
    }

    protected static List<String> goalNames(String str) {
        String[] goals = str.split(" -> ");
        List<String> goalNames = Lists.newLinkedList();
        for(String goalName : goals) {
            goalNames.add(goalName.trim());
        }
        return goalNames;
    }

    @Override public boolean canEvaluate(IFragment inputFragment, IStrategoTerm expectationTerm) {
        String cons = SPTUtil.consName(expectationTerm);
        if(!TRANSFORM.equals(cons)) {
            return false;
        }
        if(expectationTerm.getSubtermCount() == 1) {
            return TermUtils.isString(expectationTerm.getSubterm(0));
        } else if(expectationTerm.getSubtermCount() == 2) {
            return TermUtils.isString(expectationTerm.getSubterm(0))
                && goalNames(TermUtils.toJavaString(expectationTerm.getSubterm(0))).size() > 0;
        }
        return false;
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        ISourceLocation loc = traceService.location(expectationTerm);
        ISourceRegion region = loc == null ? inputFragment.getRegion() : loc.region();

        // It's a Transform("goal") or a Transform("goal", ToPart(...))
        String unQuotedGoalStr = TermUtils.toJavaString(expectationTerm.getSubterm(0));
        if(unQuotedGoalStr.length() > 2 && unQuotedGoalStr.startsWith("\"") && unQuotedGoalStr.endsWith("\"")) {
            unQuotedGoalStr = unQuotedGoalStr.substring(1, unQuotedGoalStr.length() - 1);
        }
        final List<String> goalNames = goalNames(unQuotedGoalStr);

        final ITransformGoal goal;
        if(goalNames.size() > 1) {
            goal = new NamedGoal(goalNames);
        } else if(goalNames.size() == 0) {
            throw new IllegalArgumentException(
                "Can't create an evaluator for a transformation expectation without a transformation goal.");
        } else {
            goal = new EndNamedGoal(goalNames.get(0));
        }

        if(expectationTerm.getSubtermCount() == 2) {
            final IStrategoTerm toPart = expectationTerm.getSubterm(1);
            final String lang = FragmentUtil.toPartLangName(toPart);
            final ISourceRegion langRegion = fragmentUtil.toPartLangNameRegion(toPart);
            final IFragment fragment = fragmentBuilder.withFragment(FragmentUtil.toPartFragment(toPart))
                .withProject(inputFragment.getProject()).withResource(inputFragment.getResource()).build();
            return new TransformExpectation(region, goal, fragment, lang, langRegion);
        } else {
            return new TransformExpectation(region, goal, null, null, null);
        }

    }
}
