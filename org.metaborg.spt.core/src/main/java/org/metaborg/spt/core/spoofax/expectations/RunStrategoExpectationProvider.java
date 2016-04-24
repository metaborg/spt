package org.metaborg.spt.core.spoofax.expectations;

import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.IFragment;
import org.metaborg.spt.core.ITestExpectation;
import org.metaborg.spt.core.expectations.RunStrategoExpectation;
import org.metaborg.spt.core.spoofax.ISpoofaxFragmentBuilder;
import org.metaborg.spt.core.spoofax.ISpoofaxTestExpectationProvider;
import org.metaborg.spt.core.util.SPTUtil;
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
        return Term.isTermString(expectationTerm.getSubterm(0))
            && (RUN.equals(cons) && expectationTerm.getSubtermCount() == 1
                || RUN_TO.equals(cons) && expectationTerm.getSubtermCount() == 2);
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        ISourceLocation loc = traceService.location(expectationTerm);
        ISourceRegion region = loc == null ? inputFragment.getRegion() : loc.region();

        final String cons = SPTUtil.consName(expectationTerm);
        final IStrategoTerm stratTerm = expectationTerm.getSubterm(0);
        final String strategy = Term.asJavaString(stratTerm);
        ISourceLocation stratLoc = traceService.location(stratTerm);
        if(RUN.equals(cons)) {
            return new RunStrategoExpectation(region, strategy, stratLoc.region());
        } else {
            final IStrategoTerm toPart = expectationTerm.getSubterm(1);
            final String langName = FragmentUtil.toPartLangName(toPart);
            final ISourceRegion langRegion = fragmentUtil.toPartLangNameRegion(toPart);
            final IFragment outputFragment = fragmentBuilder.withFragment(FragmentUtil.toPartFragment(toPart))
                .withProject(inputFragment.getProject()).withResource(inputFragment.getResource()).build();
            return new RunStrategoExpectation(region, strategy, loc.region(), outputFragment, langName, langRegion);
        }
    }

}
