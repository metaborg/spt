package org.metaborg.spt.core.run.expectations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.metaborg.core.MetaborgException;
import org.metaborg.core.context.ContextException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.messages.MessageType;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.unit.IUnit;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.TestPhase;
import org.metaborg.mbt.core.model.expectations.MessageUtil;
import org.metaborg.mbt.core.run.ITestExpectationInput;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalysisService;
import org.metaborg.spoofax.core.stratego.IStrategoCommon;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.SPTUtil;
import org.metaborg.spt.core.expectations.RunStrategoExpectation;
import org.metaborg.spt.core.run.FragmentUtil;
import org.metaborg.spt.core.run.ISpoofaxExpectationEvaluator;
import org.metaborg.spt.core.run.ISpoofaxFragmentResult;
import org.metaborg.spt.core.run.ISpoofaxTestExpectationOutput;
import org.metaborg.spt.core.run.SpoofaxFragmentResult;
import org.metaborg.spt.core.run.SpoofaxTestExpectationOutput;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.StrategoInt;
import org.spoofax.terms.util.TermUtils;
import org.strategoxt.lang.TermEqualityUtil;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class RunStrategoExpectationEvaluator implements ISpoofaxExpectationEvaluator<RunStrategoExpectation> {
    private static final ILogger logger = LoggerUtils.logger(RunStrategoExpectationEvaluator.class);

    private final IContextService contextService;
    private final ISpoofaxTracingService traceService;
    private final ISpoofaxAnalysisService analysisService;
    private final ITermFactory termFactory;
    private final FragmentUtil fragmentUtil;
    private final IStrategoCommon stratego;


    @Inject public RunStrategoExpectationEvaluator(IContextService contextService, ISpoofaxTracingService traceService,
        ISpoofaxAnalysisService analysisService, ITermFactory termFactory, FragmentUtil fragmentUtil,
        IStrategoCommon stratego) {
        this.contextService = contextService;
        this.traceService = traceService;
        this.analysisService = analysisService;
        this.termFactory = termFactory;
        this.fragmentUtil = fragmentUtil;
        this.stratego = stratego;
    }


    @Override public Collection<Integer> usesSelections(IFragment fragment, RunStrategoExpectation expectation) {
        return expectation.selection() == null ? Lists.newArrayList()
            : Lists.newArrayList(expectation.selection());
    }

    @Override public TestPhase getPhase(ILanguageImpl language, RunStrategoExpectation expectation) {
        if(analysisService.available(language)) {
            return TestPhase.ANALYSIS;
        } else {
            return TestPhase.PARSING;
        }
    }

    @Override public ISpoofaxTestExpectationOutput evaluate(
        ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input, RunStrategoExpectation expectation) {
        logger.debug("Evaluating a RunStrategoExpectation (strat: {}, outputLang: {}, outputFragment: {})",
            expectation.strategy(), expectation.outputLanguage(), expectation.outputFragment());

        List<IMessage> messages = Lists.newLinkedList();
        List<ISpoofaxFragmentResult> fragmentResults = Lists.newLinkedList();

        ITestCase test = input.getTestCase();
        List<ISourceRegion> selections = test.getFragment().getSelections();

        String strategy = expectation.strategy();

        // The result of performing an analyze or parse action on the fragment
        final IUnit actionResult;
        @Nullable IContext context;
        final ISpoofaxAnalyzeUnit analysisResult = input.getFragmentResult().getAnalysisResult();
        if(analysisResult != null) {
            if(!analysisResult.valid() || !analysisResult.hasAst()) {
                messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                    "Analysis did not return a valid AST.", null));
                MessageUtil.propagateMessages(analysisResult.messages(), messages, test.getDescriptionRegion(),
                    test.getFragment().getRegion());
                return new SpoofaxTestExpectationOutput(false, messages, fragmentResults);
            }
            actionResult = analysisResult;
            context = analysisResult.context();
        } else {
            final ISpoofaxParseUnit parseResult = input.getFragmentResult().getParseResult();
            if(!parseResult.valid()) {
                messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                    "Parsing did not return a valid AST.", null));
                MessageUtil.propagateMessages(parseResult.messages(), messages, test.getDescriptionRegion(),
                    test.getFragment().getRegion());
                return new SpoofaxTestExpectationOutput(false, messages, fragmentResults);
            }
            actionResult = parseResult;
            try {
                context = contextService.get(test.getResource(), test.getProject(), input.getLanguageUnderTest());
            } catch(ContextException e) {
                // Ignore error, just set context to null
                context = null;
            }
        }

        // Obtain the AST nodes to try to run on.
        final List<IStrategoTerm> terms = runOnTerms(test, expectation, actionResult, selections, messages);

        // before we try to run anything, make sure we have something to execute on
        if(terms.isEmpty()) {
            messages.add(MessageFactory.newMessage(test.getResource(), test.getDescriptionRegion(),
                "Could not select fragment(s) to run strategy on.", MessageSeverity.ERROR, MessageType.TRANSFORMATION,
                null));
            return new SpoofaxTestExpectationOutput(false, messages, fragmentResults);
        }
        
        List<IStrategoTerm> arguments = parseArguments(test, expectation, actionResult, selections, messages);
        if (!messages.isEmpty() ) {
        	return new SpoofaxTestExpectationOutput(false, messages, fragmentResults);
        }

        // run the strategy until we are done
        boolean success = false;
        IMessage lastMessage = null;
        for(IStrategoTerm term : terms) {
            // logger.debug("About to try to run the strategy {} on {}", expectation.strategy(), term);
            // reset the last message
            lastMessage = null;
            try {
                final IStrategoTerm result;
                if (context == null) {
                    if (arguments == null) {
                        result = stratego.invoke(input.getLanguageUnderTest(), test.getResource(), term, strategy);
                    } else {
                        result = stratego.invoke(input.getLanguageUnderTest(), test.getResource(), term, strategy,
                                arguments);
                    }
                } else {
                    if (arguments == null) {
                        result = stratego.invoke(input.getLanguageUnderTest(), analysisResult.context(), term,
                                strategy);
                    } else {
                        result = stratego.invoke(input.getLanguageUnderTest(), analysisResult.context(), term, strategy,
                                arguments);
                    }
                }

                if (expectation.getExpectedToFail()) {
                    if (result == null) {
                        success = true;
                    } else {
                        lastMessage = MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                                String.format("The given strategy %1$s is expected to fail but succeeded.",
                                        expectation.strategy()),
                                null);
                    }
                    continue;
                } else {
                    if (result == null) {
                        lastMessage = MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                                String.format("The given strategy %1$s failed during execution.",
                                        expectation.strategy()),
                                null);
                        continue;
                    }
                }
                
                // the strategy was successful
                if(expectation.outputFragment() == null) {
                    // a successful invocation is all we need
                    success = true;
                } else {
                    // it's a RunTo(strategyName, ToPart(languageName, openMarker, fragment, closeMarker))
                    // we need to analyze the fragment, at least until we support running on raw parsed terms
                    final ISpoofaxAnalyzeUnit analyzedFragment;
                    if(expectation.outputLanguage() == null) {
                        // default to the language under test if no language was given
                        analyzedFragment = fragmentUtil.analyzeFragment(expectation.outputFragment(),
                            input.getLanguageUnderTest(), messages, test, input.getFragmentParserConfig());
                    } else {
                        analyzedFragment = fragmentUtil.analyzeFragment(expectation.outputFragment(),
                            expectation.outputLanguage(), messages, test, input.getFragmentParserConfig());
                    }
                    // compare the ASTs
                    if(analyzedFragment != null && TermEqualityUtil.equalsIgnoreAnnos(analyzedFragment.ast(), result,
                        termFactory)) {
                        success = true;
                    } else {
                        lastMessage = MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                            String.format(
                                "The result of running %1$s did not match the expected result.\nExpected: %2$s\nGot: %3$s",
                                strategy, analyzedFragment == null ? "null" : analyzedFragment.ast(), result),
                            null);
                    }
                    if(analyzedFragment != null) {
                        fragmentResults.add(new SpoofaxFragmentResult(expectation.outputFragment(),
                            analyzedFragment.input(), analyzedFragment, null));
                    }
                }
                if(success) {
                    break;
                }
            } catch(MetaborgException e) {
                // who knows what caused this, but we will just keep trying on the other terms
                logger.warn("Encountered an error while executing the given strategy", e);
                lastMessage = MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                    "Encountered an error while executing the given strategy.", e);
            }
        }
        if(lastMessage != null) {
            messages.add(lastMessage);
        }

        return new SpoofaxTestExpectationOutput(success, messages, fragmentResults);
    }


    private List<IStrategoTerm> parseArguments(ITestCase test, RunStrategoExpectation expectation, IUnit parsedFragment,
            List<ISourceRegion> selections, List<IMessage> messages) {
        if (test == null || expectation == null || expectation.getArguments() == null) {
            return null;
        }

        List<IStrategoTerm> parsedArgs = new ArrayList<>();

        for (IStrategoTerm arg : expectation.getArguments()) {
            if (SPTUtil.isStringLiteral(arg)) {
                IStrategoString stringTerm = TermUtils.toString(arg.getSubterm(0));
                parsedArgs.add(stringTerm);
            } else if (SPTUtil.isIntLiteral(arg)) {
                String stringValue = TermUtils.toString(arg.getSubterm(0)).stringValue();
                StrategoInt intTerm = new StrategoInt(Integer.parseInt(stringValue));
                parsedArgs.add(intTerm);
            } else if (SPTUtil.isSelectionRef(arg)) {
                int index = Integer.parseInt(TermUtils.toJavaStringAt(arg, 0));
                if (index > selections.size()) {
                    messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                            "Not enough selections to resolve #" + index, null));
                    return null;
                }
                ISourceRegion selectedRegion = selections.get(index - 1);

                Iterable<IStrategoTerm> selectedTerms;
                if (parsedFragment instanceof ISpoofaxParseUnit) {
                    selectedTerms = traceService.fragmentsWithin((ISpoofaxParseUnit) parsedFragment, selectedRegion);
                } else {
                    selectedTerms = traceService.fragmentsWithin((ISpoofaxAnalyzeUnit) parsedFragment, selectedRegion);
                }

                Collection<IStrategoTerm> selectedTermsCollection = (Collection<IStrategoTerm>) selectedTerms;
                if (selectedTermsCollection.isEmpty()) {
                    messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                            "Could not resolve this selection to an AST node.", null));
                    return null;
                } else if (selectedTermsCollection.size() == 1) {
                    parsedArgs.addAll(selectedTermsCollection);
                } else {
                    IStrategoList strategoList = termFactory.makeList(selectedTermsCollection);
                    parsedArgs.add(strategoList);
                }
                
            }
        }

        return parsedArgs;
    }

    /*
     * Obtain the AST nodes to try to run on.
     * 
     * We collect all terms with the exact right offsets, and try to execute the strategy on each of these terms,
     * starting on the outermost term, until we processed them all or one of them passed successfully.
     */
    private List<IStrategoTerm> runOnTerms(ITestCase test, RunStrategoExpectation expectation, final IUnit result,
        List<ISourceRegion> selections, List<IMessage> outMessages) {
        final List<IStrategoTerm> terms = Lists.newLinkedList();
        if(expectation.selection() == null) {
            // no selections, so we run on the entire ast
            // but only on the part that is inside the actual fragment, not the fixture
            final Iterable<IStrategoTerm> fragments;
            if(result instanceof ISpoofaxParseUnit) {
                fragments = traceService.fragmentsWithin((ISpoofaxParseUnit) result, test.getFragment().getRegion());
            } else {
                fragments = traceService.fragmentsWithin((ISpoofaxAnalyzeUnit) result, test.getFragment().getRegion());
            }
            for(IStrategoTerm term : fragments) {
                terms.add(term);
            }
        } else if(expectation.selection() > selections.size()) {
            // not enough selections to resolve it
            outMessages.add(MessageFactory.newAnalysisError(test.getResource(), expectation.selectionRegion(),
                "Not enough selections to resolve #" + expectation.selection(), null));
        } else {
            // the input should be the selected term
            ISourceRegion selection = selections.get(expectation.selection() - 1);
            final Iterable<IStrategoTerm> fragments;
            if(result instanceof ISpoofaxParseUnit) {
                fragments = traceService.fragmentsWithin((ISpoofaxParseUnit) result, selection);
            } else {
                fragments = traceService.fragmentsWithin((ISpoofaxAnalyzeUnit) result, selection);
            }
            for(IStrategoTerm possibleSelection : fragments) {
                terms.add(possibleSelection);
            }
            if(terms.isEmpty()) {
                outMessages.add(MessageFactory.newAnalysisError(test.getResource(), selection,
                    "Could not resolve this selection to an AST node.", null));
            }
        }
        return Lists.reverse(terms);
    }
}
