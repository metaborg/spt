package org.metaborg.spt.core.extract.expectations;

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
                return expectationTerm.getSubtermCount() == 2 && Term.isTermString(expectationTerm.getSubterm(0));
            case RUN_TO:
                return expectationTerm.getSubtermCount() == 3 && Term.isTermString(expectationTerm.getSubterm(0));
            default:
                return false;
        }
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        ISourceLocation loc = traceService.location(expectationTerm);
        ISourceRegion region = loc == null ? inputFragment.getRegion() : loc.region();

        final String cons = SPTUtil.consName(expectationTerm);
        final IStrategoTerm stratTerm = expectationTerm.getSubterm(0);
        final String strategy = Term.asJavaString(stratTerm);
        final ISourceLocation stratLoc = traceService.location(stratTerm);
        final IStrategoTerm onTerm = expectationTerm.getSubterm(1);
        final Integer selection;
        final ISourceRegion selectionRegion;
        if(SPTUtil.SOME_CONS.equals(SPTUtil.consName(onTerm))) {
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

        if(RUN.equals(cons)) {
            return new RunStrategoExpectation(region, strategy, stratLoc.region(), selection, selectionRegion);
        } else {
            final IStrategoTerm toPart = expectationTerm.getSubterm(1);
            final String langName = FragmentUtil.toPartLangName(toPart);
            final ISourceRegion langRegion = fragmentUtil.toPartLangNameRegion(toPart);
            final IFragment outputFragment = fragmentBuilder.withFragment(FragmentUtil.toPartFragment(toPart))
                .withProject(inputFragment.getProject()).withResource(inputFragment.getResource()).build();
            return new RunStrategoExpectation(region, strategy, stratLoc.region(), selection, selectionRegion,
                outputFragment, langName, langRegion);
        }
    }

}
