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
import org.metaborg.spt.core.run.expectations.RunStrategoExpectationEvaluator;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.util.TermUtils;

import com.google.inject.Inject;

/**
 * Runs Stratego strategies on selections or the entire test and compares results.
 * 
 * For now, we only run against the AST nodes of the analyzed AST.
 */
public class RunStrategoExpectationProvider implements ISpoofaxTestExpectationProvider {
	
    private static final ILogger logger = LoggerUtils.logger(RunStrategoExpectationProvider.class);


    private static final String RUN = "Run";
    private static final String RUN_TO = "RunTo";
    private static final String RUN_TO_WITH_ARGS = "RunToWithArgs";

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
        String constructor = SPTUtil.consName(expectationTerm);
        logger.warn("constructor: " + constructor);
        logger.warn("expectation: " + expectationTerm );
        
        if(!TermUtils.isString(getStrategyTerm(expectationTerm))) {
        	return false;
        }
        
        if (!checkOptionalOnPart(getOnPartTerm(expectationTerm, constructor))) {
        	return false;
        }
        
        int subTermCount = expectationTerm.getSubtermCount();
		switch(constructor) {
            case RUN:
                return subTermCount == 2;
            case RUN_TO:
                return subTermCount == 3 
                    && FragmentUtil.checkToPart(getToPartTerm(expectationTerm, constructor));
            case RUN_TO_WITH_ARGS:
            	return subTermCount == 4 
                && FragmentUtil.checkToPart(getToPartTerm(expectationTerm, constructor))
                && checkOptionalTermArgs(getTermArguments(expectationTerm));
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
        final String strategy = TermUtils.toJavaString(stratTerm);
        final ISourceLocation stratLoc = traceService.location(stratTerm);
        final @Nullable IStrategoTerm onTerm = SPTUtil.getOptionValue(getOnPartTerm(expectationTerm, cons));
        final Integer selection;
        final ISourceRegion selectionRegion;
        if(onTerm != null) {
            // on #<int> was present
            selection = TermUtils.toJavaInt(onTerm);
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
            final IStrategoTerm toPart = getToPartTerm(expectationTerm, cons);
            final String langName = FragmentUtil.toPartLangName(toPart);
            final ISourceRegion langRegion = fragmentUtil.toPartLangNameRegion(toPart);
            final IFragment outputFragment = fragmentBuilder.withFragment(FragmentUtil.toPartFragment(toPart))
                .withProject(inputFragment.getProject()).withResource(inputFragment.getResource()).build();
            
            final List<IStrategoTerm> termArgs;
            if(RUN_TO_WITH_ARGS.equals(cons)) {
            	termArgs = TermUtils.toJavaList(getTermArguments(expectationTerm));
            } else {
            	termArgs = null;
            }
            
            return new RunStrategoExpectation(region, strategy, stratLoc.region(), selection, selectionRegion,
                outputFragment, langName, langRegion, termArgs);
        }
    }

    private IStrategoTerm getStrategyTerm(IStrategoTerm expectation) {
        return expectation.getSubterm(0);
    }
    
    private IStrategoTerm getTermArguments(IStrategoTerm expectation) {
    	return expectation.getSubterm(1);
    }

    private IStrategoTerm getOnPartTerm(IStrategoTerm expectation, String constructor) {
    	if(RUN_TO_WITH_ARGS.equals(constructor)) {
    		return expectation.getSubterm(2);
    	} else {
    		return expectation.getSubterm(1);
    	}
    }

    private IStrategoTerm getToPartTerm(IStrategoTerm expectation, String constructor) {
    	if(RUN_TO_WITH_ARGS.equals(constructor)) {
    		return expectation.getSubterm(3);
    	} else {
    		return expectation.getSubterm(2);
    	}
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
                return TermUtils.isInt(onPart);
            }
        } else {
            return false;
        }
    }
    
    // Check if the given term is an optional TermArgs part
    // i.e. None() or Some(<List>)
    protected static boolean checkOptionalTermArgs(IStrategoTerm term) {
        if(SPTUtil.checkOption(term)) {
            final IStrategoTerm args = SPTUtil.getOptionValue(term);
            if(args == null) {
                // it's a None()
                return true;
            } else {
                // it's a Some(List)
                return TermUtils.isList(args);
            }
        } else {
            return false;
        }
    }

}
