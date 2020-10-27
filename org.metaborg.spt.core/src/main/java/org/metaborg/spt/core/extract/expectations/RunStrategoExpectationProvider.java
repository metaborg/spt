package org.metaborg.spt.core.extract.expectations;

import java.util.List;

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
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.util.TermUtils;

import com.google.inject.Inject;

/**
 * Runs Stratego strategies on selections or the entire test and compares
 * results.
 * 
 * For now, we only run against the AST nodes of the analyzed AST.
 */
public class RunStrategoExpectationProvider implements ISpoofaxTestExpectationProvider {

    private static final ILogger logger = LoggerUtils.logger(RunStrategoExpectationProvider.class);

    private static final String RUN = "Run";
    private static final String FAILS = "Fails";

    private final ISpoofaxFragmentBuilder fragmentBuilder;
    private final ISpoofaxTracingService traceService;

    private final FragmentUtil fragmentUtil;

    @Inject
    public RunStrategoExpectationProvider(ISpoofaxFragmentBuilder fragmentBuilder, ISpoofaxTracingService traceService,
            FragmentUtil fragmentUtil) {
        this.fragmentBuilder = fragmentBuilder;
        this.traceService = traceService;
        this.fragmentUtil = fragmentUtil;
    }

    @Override
    public boolean canEvaluate(IFragment inputFragment, IStrategoTerm expectationTerm) {
        String constructor = SPTUtil.consName(expectationTerm);

        logger.warn("expectation: " + expectationTerm);

        if (!RUN.equals(constructor)) {
            return false;
        }

        if (expectationTerm.getSubtermCount() != 4) {
            return false;
        }

        if (!TermUtils.isString(getStrategyName(expectationTerm))) {
            return false;
        }

        if (!checkOptionalTermArgs(getTermArguments(expectationTerm))) {
            return false;
        }

        if (!FragmentUtil.checkOptionalOnPart(getOnPart(expectationTerm))) {
            return false;
        }

        if (!checkResult(getResultPart(expectationTerm))) {
            return false;
        }

        return true;
    }

    @Override
    public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        ISourceLocation loc = traceService.location(expectationTerm);
        ISourceRegion region = loc == null ? inputFragment.getRegion() : loc.region();

        final IStrategoTerm strategyNameTerm = getStrategyName(expectationTerm);
        final String strategyName = TermUtils.toJavaString(strategyNameTerm);
        final ISourceLocation stratLoc = traceService.location(strategyNameTerm);

        final @Nullable IStrategoTerm onTerm = SPTUtil.getOptionValue(getOnPart(expectationTerm));
        Integer selection = null;
        ISourceRegion selectionRegion = null;
        if (onTerm != null) {
            selection = TermUtils.toJavaInt(onTerm);
            final ISourceLocation selLoc = traceService.location(onTerm);
            if (selLoc == null) {
                selectionRegion = region;
            } else {
                selectionRegion = selLoc.region();
            }
        }

        final @Nullable IStrategoTerm toPart = getToPart(expectationTerm);
        String langName = null;
        ISourceRegion langRegion = null;
        IFragment outputFragment = null;

        if (toPart != null) {
            langName = FragmentUtil.toPartLangName(toPart);
            langRegion = fragmentUtil.toPartLangNameRegion(toPart);
            outputFragment = fragmentBuilder.withFragment(FragmentUtil.toPartFragment(toPart))
                    .withProject(inputFragment.getProject()).withResource(inputFragment.getResource()).build();
        }

        boolean expectedToFail = getExpectedToFail(expectationTerm);

        IStrategoTerm termArgumentsTerm = SPTUtil.getOptionValue(getTermArguments(expectationTerm));
        IStrategoTerm termArguments = termArgumentsTerm != null ? termArgumentsTerm.getSubterm(0) : null;
        final List<IStrategoTerm> termArgumentList = TermUtils.asJavaList(termArguments).orElse(null);

        return new RunStrategoExpectation(region, strategyName, stratLoc.region(), selection, selectionRegion,
                outputFragment, langName, langRegion, termArgumentList, expectedToFail);
    }

    private IStrategoTerm getStrategyName(IStrategoTerm expectation) {
        return expectation.getSubterm(0);
    }

    private IStrategoTerm getTermArguments(IStrategoTerm expectation) {
        return expectation.getSubterm(1);
    }

    private IStrategoTerm getOnPart(IStrategoTerm expectation) {
        return expectation.getSubterm(2);
    }

    private IStrategoTerm getResultPart(IStrategoTerm expectation) {
        return expectation.getSubterm(3);
    }

    private IStrategoTerm getToPart(IStrategoTerm expectation) {
        IStrategoTerm result = SPTUtil.getOptionValue(getResultPart(expectation));
        if (FragmentUtil.checkToPart(result)) {
            return result;
        } else {
            return null;
        }

    }

    private boolean getExpectedToFail(IStrategoTerm expectation) {
        IStrategoTerm result = SPTUtil.getOptionValue(getResultPart(expectation));
        String constructor = SPTUtil.consName(result);
        if (FAILS.equals(constructor)) {
            return true;
        } else {
            return false;
        }

    }

    private boolean checkOptionalTermArgs(IStrategoTerm term) {
        if (!SPTUtil.checkOption(term)) {
            return false;
        }

        final IStrategoTerm args = SPTUtil.getOptionValue(term);
        if (args == null) {
            logger.warn("true");
            return true;
        } else {
            return TermUtils.isList(args.getSubterm(0));
        }
    }

    private boolean checkResult(IStrategoTerm resultPart) {
        if (!SPTUtil.checkOption(resultPart)) {
            return false;
        }
        final IStrategoTerm result = SPTUtil.getOptionValue(resultPart);
        if (result == null) {
            return true;
        }
        String constructor = SPTUtil.consName(result);
        if (FAILS.equals(constructor)) {
            return true;
        } else {
            return FragmentUtil.checkToPart(result);
        }
    }

}
