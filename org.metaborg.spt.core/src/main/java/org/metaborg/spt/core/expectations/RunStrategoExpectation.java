package org.metaborg.spt.core.expectations;

import java.util.List;

import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.FacetContribution;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spoofax.core.stratego.IStrategoRuntimeService;
import org.metaborg.spoofax.core.stratego.StrategoRuntimeFacet;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spt.core.ITestCase;
import org.metaborg.spt.core.ITestExpectation;
import org.metaborg.spt.core.ITestExpectationInput;
import org.metaborg.spt.core.ITestExpectationOutput;
import org.metaborg.spt.core.TestExpectationOutput;
import org.metaborg.spt.core.TestPhase;
import org.metaborg.spt.core.util.SPTUtil;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.core.UndefinedStrategyException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;
import org.strategoxt.HybridInterpreter;
import org.strategoxt.lang.TermEqualityUtil;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Runs Stratego strategies on selections or the entire test and compares results.
 * 
 * For now, we only run against the AST nodes of the analyzed AST.
 */
public class RunStrategoExpectation implements ITestExpectation {

    private static final String RUN = "Run";
    private static final String RUN_TO = "RunTo";

    private final IStrategoRuntimeService runtimeService;
    private final ISpoofaxTracingService traceService;
    private final ITermFactoryService termFactoryService;
    private final FragmentUtil fragmentUtil;

    @Inject public RunStrategoExpectation(IStrategoRuntimeService runtimeService, ISpoofaxTracingService traceService,
        ITermFactoryService termFactoryService, FragmentUtil fragmentUtil) {
        this.runtimeService = runtimeService;
        this.traceService = traceService;
        this.termFactoryService = termFactoryService;
        this.fragmentUtil = fragmentUtil;
    }

    @Override public boolean canEvaluate(IStrategoTerm expectationTerm) {
        String cons = SPTUtil.consName(expectationTerm);
        return RUN.equals(cons) || RUN_TO.equals(cons);
    }

    @Override public TestPhase getPhase(IStrategoTerm expectationTerm) {
        // until we support running on raw ASTs
        return TestPhase.TRANSFORMATION;
    }

    @Override public ITestExpectationOutput evaluate(ITestExpectationInput input) {
        List<IMessage> messages = Lists.newLinkedList();

        ITestCase test = input.getTestCase();
        IStrategoTerm expectation = input.getExpectation();
        List<ISourceRegion> selections = test.getFragment().getSelections();

        // obtain the strategy to run from Run(strategyName) or RunTo(strategyName, ToPart(languageName, openMarker,
        // fragment, closeMarker))
        IStrategoTerm strategyTerm = expectation.getSubterm(0);
        String strategy = Term.asJavaString(strategyTerm);


        // we need an analysis result with an AST (until we allow running on raw ASTs)
        ISpoofaxAnalyzeUnit analysisResult = input.getAnalysisResult();
        if(analysisResult == null) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Expected analysis to succeed", null));
            return new TestExpectationOutput(false, messages);
        }
        if(!analysisResult.valid() || !analysisResult.hasAst()) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Analysis did not return a valid AST.", null));
            MessageUtil.propagateMessages(analysisResult.messages(), messages, test.getDescriptionRegion());
            return new TestExpectationOutput(false, messages);
        }

        // Create the runtime for stratego
        HybridInterpreter runtime = null;
        FacetContribution<StrategoRuntimeFacet> facetContrib =
            input.getLanguageUnderTest().facetContribution(StrategoRuntimeFacet.class);
        if(facetContrib == null) {
            ISourceLocation loc = traceService.location(expectation);
            ISourceRegion region = loc == null ? test.getDescriptionRegion() : loc.region();
            messages.add(MessageFactory.newAnalysisError(test.getResource(), region,
                "Unable to load the StrategoRuntimeFacet for the language under test.", null));
        } else {
            try {
                runtime = runtimeService.runtime(facetContrib.contributor, analysisResult.context());
            } catch(MetaborgException e) {
                ISourceLocation loc = traceService.location(expectation);
                ISourceRegion region = loc == null ? test.getDescriptionRegion() : loc.region();
                messages.add(MessageFactory.newAnalysisError(test.getResource(), region,
                    "Unable to load required files for the Stratego runtime.", e));
            }
        }

        /*
         * Obtain the AST nodes to try to run on.
         * 
         * We collect all terms with the exact right offsets, and try to execute the strategy on each of these terms,
         * starting on the outermost term, until we processed them all or one of them passed successfully.
         */
        List<IStrategoTerm> terms = Lists.newLinkedList();
        if(selections.isEmpty()) {
            // no selections, so we run on the entire ast
            terms.add(analysisResult.ast());
        } else if(selections.size() > 1) {
            // too many selections, we don't know which to select as input
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Too many selections in this test case, we don't know which selection you want to use as input.",
                null));
        } else {
            // the input should be the selected term
            ISourceRegion selection = selections.get(0);
            for(IStrategoTerm possibleSelection : traceService.fragments(analysisResult, selection)) {
                ISourceLocation loc = traceService.location(possibleSelection);
                // the region should match exactly
                if(loc != null && loc.region().startOffset() == selection.startOffset()
                    && loc.region().endOffset() == selection.endOffset()) {
                    terms.add(possibleSelection);
                }
            }
            if(terms.isEmpty()) {
                messages.add(MessageFactory.newAnalysisError(test.getResource(), selection,
                    "Could not resolve this selection to an AST node.", null));
            }
        }
        terms = Lists.reverse(terms);

        // before we try to run anything, make sure we have a runtime and something to execute on
        if(runtime == null || terms.isEmpty()) {
            return new TestExpectationOutput(false, messages);
        }

        // run the strategy until we are done
        boolean success = false;
        IMessage lastMessage = null;
        for(IStrategoTerm term : terms) {
            // reset the last message
            lastMessage = null;
            runtime.setCurrent(term);
            try {
                // if the strategy failed, try the next input term
                if(!runtime.invoke(strategy)) {
                    continue;
                }
                // the strategy wa successfull
                switch(SPTUtil.consName(expectation)) {
                    case RUN:
                        // a successful invocation is all we need
                        success = true;
                        break;
                    case RUN_TO:
                        // it's a RunTo(strategyName, ToPart(languageName, openMarker, fragment, closeMarker))
                        // we need to analyze the fragment, at least until we support running on raw parsed terms
                        IStrategoTerm toPart = expectation.getSubterm(1);
                        ISpoofaxAnalyzeUnit analyzedFragment = fragmentUtil.analyzeToPart(toPart, messages, test);
                        // compare the ASTs
                        if(analyzedFragment != null
                            && TermEqualityUtil.equalsIgnoreAnnos(analyzedFragment.ast(), runtime.current(),
                                termFactoryService.get(fragmentUtil.langFromToPart(toPart).activeImpl()))) {
                            success = true;
                        }
                        break;
                    default:
                        // TODO: is this ok? Or should we fail gracefully?
                        throw new IllegalArgumentException("Can't handle expectation " + expectation);
                }
                if(success) {
                    break;
                }
            } catch(UndefinedStrategyException e) {
                ISourceLocation loc = traceService.location(strategyTerm);
                ISourceRegion region = loc == null ? test.getDescriptionRegion() : loc.region();
                lastMessage = MessageFactory.newAnalysisError(test.getResource(), region,
                    "No such strategy found: " + strategy, e);
                // this exception does not depend on the input so we can stop trying
                break;
            } catch(InterpreterException e) {
                // who knows what caused this, but we will just keep trying on the other terms
                ISourceLocation loc = traceService.location(strategyTerm);
                ISourceRegion region = loc == null ? test.getDescriptionRegion() : loc.region();
                lastMessage = MessageFactory.newAnalysisError(test.getResource(), region,
                    "Encountered an error while executing the given strategy.", e);
            }
        }
        if(lastMessage != null) {
            messages.add(lastMessage);
        }

        return new TestExpectationOutput(success, messages);
    }

}
