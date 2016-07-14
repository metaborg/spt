package org.metaborg.spt.core.run.expectations;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.metaborg.core.MetaborgException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.TestPhase;
import org.metaborg.mbt.core.model.expectations.MessageUtil;
import org.metaborg.mbt.core.run.ITestExpectationInput;
import org.metaborg.spoofax.core.stratego.IStrategoCommon;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.SPTUtil;
import org.metaborg.spt.core.expectations.RunStrategoToAtermExpectation;
import org.metaborg.spt.core.run.ISpoofaxExpectationEvaluator;
import org.metaborg.spt.core.run.ISpoofaxFragmentResult;
import org.metaborg.spt.core.run.ISpoofaxTestExpectationOutput;
import org.metaborg.spt.core.run.SpoofaxTestExpectationOutput;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class RunStrategoToAtermExpectationEvaluator
    implements ISpoofaxExpectationEvaluator<RunStrategoToAtermExpectation> {

    private static final ILogger logger = LoggerUtils.logger(RunStrategoToAtermExpectationEvaluator.class);

    private final IStrategoCommon stratego;
    private final ISpoofaxTracingService traceService;
    private final ITermFactoryService termFactoryService;

    @Inject public RunStrategoToAtermExpectationEvaluator(IStrategoCommon stratego, ISpoofaxTracingService traceService,
        ITermFactoryService termFactoryService) {
        this.stratego = stratego;
        this.traceService = traceService;
        this.termFactoryService = termFactoryService;
    }

    @SuppressWarnings("unchecked") @Override public Collection<Integer> usesSelections(IFragment fragment,
        RunStrategoToAtermExpectation expectation) {
        return expectation.selection() == null ? Collections.EMPTY_LIST : Lists.newArrayList(expectation.selection());
    }

    @Override public TestPhase getPhase(IContext languageUnderTestCtx, RunStrategoToAtermExpectation expectation) {
        // until we support running on raw ASTs
        return TestPhase.ANALYSIS;
    }

    @Override public ISpoofaxTestExpectationOutput evaluate(
        ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input,
        RunStrategoToAtermExpectation expectation) {

        List<IMessage> messages = Lists.newLinkedList();
        // the 'to ATerm' variant of this expectation doesn't have a fragment
        Iterable<ISpoofaxFragmentResult> fragmentResults = Iterables2.empty();

        ITestCase test = input.getTestCase();
        List<ISourceRegion> selections = test.getFragment().getSelections();

        String strategy = expectation.strategy();

        // we need an analysis result with an AST (until we allow running on raw ASTs)
        ISpoofaxAnalyzeUnit analysisResult = input.getFragmentResult().getAnalysisResult();
        if(analysisResult == null) {
            logger.debug("Expected analysis to succeed");
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Expected analysis to succeed", null));
            return new SpoofaxTestExpectationOutput(false, messages, fragmentResults);
        }
        if(!analysisResult.valid() || !analysisResult.hasAst()) {
            logger.debug("Analysis did not return a valid AST.");
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Analysis did not return a valid AST.", null));
            MessageUtil.propagateMessages(analysisResult.messages(), messages, test.getDescriptionRegion(),
                test.getFragment().getRegion());
            return new SpoofaxTestExpectationOutput(false, messages, fragmentResults);
        }

        /*
         * Obtain the AST nodes to try to run on.
         * 
         * We collect all terms with the exact right offsets, and try to execute the strategy on each of these terms,
         * starting on the outermost term, until we processed them all or one of them passed successfully.
         */
        List<IStrategoTerm> terms = Lists.newLinkedList();
        if(expectation.selection() == null) {
            // no selections, so we run on the entire ast
            // but only on the part that is inside the actual fragment, not the fixture
            for(IStrategoTerm term : traceService.fragmentsWithin(analysisResult, test.getFragment().getRegion())) {
                terms.add(term);
            }
        } else if(expectation.selection() > selections.size()) {
            // not enough selections to resolve this selection
            messages.add(MessageFactory.newAnalysisError(test.getResource(), expectation.selectionRegion(),
                "Not enough selections to resolve #" + expectation.selection(), null));
        } else {
            // the input should be the selected term
            ISourceRegion selection = selections.get(expectation.selection() - 1);
            for(IStrategoTerm possibleSelection : traceService.fragmentsWithin(analysisResult, selection)) {
                terms.add(possibleSelection);
            }
            if(terms.isEmpty()) {
                logger.debug("Could not resolve this selection to an AST node.");
                messages.add(MessageFactory.newAnalysisError(test.getResource(), selection,
                    "Could not resolve this selection to an AST node.", null));
            }
        }
        terms = Lists.reverse(terms);

        // before we try to run anything, make sure we have a runtime and something to execute on
        if(terms.isEmpty()) {
            logger.debug("Returning early, as there is either no runtime or nothing to run on.");
            return new SpoofaxTestExpectationOutput(false, messages, fragmentResults);
        }

        // run the strategy until we are done
        boolean success = false;
        IMessage lastMessage = null;
        for(IStrategoTerm term : terms) {
            // reset the last message
            lastMessage = null;
            try {
                final IStrategoTerm result =
                    stratego.invoke(input.getLanguageUnderTest(), analysisResult.context(), term, strategy);
                // if the strategy failed, try the next input term
                if(result == null) {
                    lastMessage = MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                        String.format("The given strategy %1$s failed during execution.", expectation.strategy()),
                        null);
                    continue;
                }
                // the strategy was successfull
                // compare the ASTs
                if(SPTUtil.checkATermMatch(result, expectation.expectedResult(),
                    termFactoryService.get(input.getLanguageUnderTest(), test.getProject(), false))) {
                    success = true;
                } else {
                    lastMessage = MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                        String.format(
                            "The result of running %1$s did not match the expected result.\nExpected: %2$s\nGot: %3$s",
                            strategy, SPTUtil.prettyPrintMatch(expectation.expectedResult()), result),
                        null);
                }
                if(success) {
                    break;
                }
            } catch(MetaborgException e) {
                // who knows what caused this, but we will just keep trying on the other terms
                lastMessage = MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                    "Encountered an error while executing the given strategy.", e);
            }
        }
        if(lastMessage != null) {
            messages.add(lastMessage);
        }

        return new SpoofaxTestExpectationOutput(success, messages, fragmentResults);
    }

}
