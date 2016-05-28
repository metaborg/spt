package org.metaborg.spt.core.run.expectations;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.metaborg.core.action.ITransformGoal;
import org.metaborg.core.context.ContextException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.context.ITemporaryContext;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.transform.TransformConfig;
import org.metaborg.core.transform.TransformException;
import org.metaborg.core.unit.IUnit;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.TestPhase;
import org.metaborg.mbt.core.model.expectations.MessageUtil;
import org.metaborg.mbt.core.model.expectations.TransformExpectation;
import org.metaborg.mbt.core.run.IFragmentParserConfig;
import org.metaborg.mbt.core.run.ITestExpectationInput;
import org.metaborg.spoofax.core.action.ActionFacet;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.core.transform.ISpoofaxTransformService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxTransformUnit;
import org.metaborg.spt.core.run.FragmentUtil;
import org.metaborg.spt.core.run.ISpoofaxExpectationEvaluator;
import org.metaborg.spt.core.run.ISpoofaxFragmentResult;
import org.metaborg.spt.core.run.ISpoofaxTestExpectationOutput;
import org.metaborg.spt.core.run.SpoofaxFragmentResult;
import org.metaborg.spt.core.run.SpoofaxTestExpectationOutput;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.TermEqualityUtil;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Covers the TransformationExpectation.
 * 
 * It parses or analyzes the input fragment and output fragment, depending on what the transformation requires. Then it
 * compares the resulting ASTs.
 */
public class TransformExpectationEvaluator implements ISpoofaxExpectationEvaluator<TransformExpectation> {

    private static final ILogger logger = LoggerUtils.logger(TransformExpectationEvaluator.class);

    private final ISpoofaxTransformService transformService;
    private final IContextService contextService;
    private final ITermFactoryService termFactoryService;

    private final FragmentUtil fragmentUtil;

    @Inject public TransformExpectationEvaluator(ISpoofaxTransformService transformService,
        IContextService contextService, ITermFactoryService termFactoryService, FragmentUtil fragmentUtil) {
        this.transformService = transformService;
        this.contextService = contextService;
        this.termFactoryService = termFactoryService;

        this.fragmentUtil = fragmentUtil;
    }

    @Override public Collection<Integer> usesSelections(IFragment fragment, TransformExpectation expectation) {
        return Lists.newLinkedList();
    }

    @Override public TestPhase getPhase(IContext languageUnderTestCtx, TransformExpectation expectation) {
        return transformService.requiresAnalysis(languageUnderTestCtx, expectation.goal()) ? TestPhase.ANALYSIS
            : TestPhase.PARSING;
    }

    @Override public ISpoofaxTestExpectationOutput evaluate(
        ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input, TransformExpectation expectation) {
        boolean success = false;
        final ITestCase test = input.getTestCase();
        final ILanguageImpl lut = input.getLanguageUnderTest();
        final List<IMessage> messages = Lists.newLinkedList();
        final List<ISpoofaxFragmentResult> fragmentResults = Lists.newLinkedList();

        // obtain a context
        ITemporaryContext tempCtx = null;
        IContext ctx = input.getFragmentResult().getContext();
        if(ctx == null) {
            // we have to create a new context
            try {
                tempCtx = contextService.getTemporary(test.getResource(), test.getProject(), lut);
                ctx = tempCtx;
            } catch(ContextException e) {
                messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                    "Failed to create a context for the language under test.", e));
                return new SpoofaxTestExpectationOutput(success, messages, fragmentResults);
            }
        }

        // check if the transformation exists for this language
        if(!transformService.available(ctx, expectation.goal())) {
            if(logger.debugEnabled()) {
                Iterable<ActionFacet> facets = lut.facets(ActionFacet.class);
                for(ActionFacet facet : facets) {
                    for(ITransformGoal availableGoal : facet.actions.keySet()) {
                        logger.debug("Available transformation: {}", availableGoal);
                    }
                }
            }
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                String.format("The transformation %1$s is unavailable for the language %2$s.", expectation.goal(),
                    input.getLanguageUnderTest().id()),
                null));
            if(tempCtx != null) {
                tempCtx.close();
            }
            return new SpoofaxTestExpectationOutput(success, messages, fragmentResults);
        }

        boolean useAnalysis = transformService.requiresAnalysis(ctx, expectation.goal());

        try {
            // transform the input fragment
            IStrategoTerm result =
                transform(input, expectation.goal(), ctx, test, messages, useAnalysis, transformService);
            if(result != null) {
                // do stuff to the output fragment
                final IStrategoTerm out = doFragment(expectation.outputFragment(), expectation.outputLanguage(), test,
                    messages, fragmentResults, useAnalysis, input.getFragmentParserConfig());
                if(out != null) {
                    // check the equality
                    if(TermEqualityUtil.equalsIgnoreAnnos(result, out,
                        termFactoryService.get(lut, test.getProject(), false))) {
                        success = true;
                    } else {
                        messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                            String.format(
                                "The result of transformation %1$s did not match the expected result.\nExpected: %2$s\nGot: %3$s",
                                expectation.goal(), out, result),
                            null));
                    }
                }
            } else {
                messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                    String.format("Transformation %1$s failed.", expectation.goal()), null));
            }
        } catch(TransformException e) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                String.format("An exception occured while trying to transform %1$s.", expectation.goal()), e));
        }

        if(tempCtx != null) {
            tempCtx.close();
        }
        return new SpoofaxTestExpectationOutput(success, messages, fragmentResults);
    }

    // parse or analyze the output fragment
    private @Nullable IStrategoTerm doFragment(IFragment fragment, String langName, ITestCase test,
        Collection<IMessage> messages, List<ISpoofaxFragmentResult> fragmentResults, boolean useAnalysis,
        @Nullable IFragmentParserConfig fragmentConfig) {
        if(useAnalysis) {
            ISpoofaxAnalyzeUnit a = fragmentUtil.analyzeFragment(fragment, langName, messages, test, fragmentConfig);
            fragmentResults.add(new SpoofaxFragmentResult(fragment, a.input(), a, null));
            if(a != null && a.success() && a.hasAst()) {
                return a.ast();
            }
        } else {
            ISpoofaxParseUnit p = fragmentUtil.parseFragment(fragment, langName, messages, test, fragmentConfig);
            fragmentResults.add(new SpoofaxFragmentResult(fragment, p, null, null));
            if(p == null || !p.valid()) {
                messages.add(MessageFactory.newAnalysisError(test.getResource(), fragment.getRegion(),
                    "Parsing of the fragment did not return an AST.", null));
            } else {
                return p.ast();
            }
        }
        return null;
    }

    // run the transformation on either the analysis or parse result
    protected static @Nullable IStrategoTerm transform(
        ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input, ITransformGoal goal, IContext ctx,
        ITestCase test, Collection<IMessage> messages, boolean useAnalysis, ISpoofaxTransformService transformService)
        throws TransformException {

        ISpoofaxTransformUnit<? extends IUnit> result;
        if(useAnalysis) {
            ISpoofaxAnalyzeUnit a = input.getFragmentResult().getAnalysisResult();
            if(a == null || !a.success()) {
                result = null;
                messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                    "Expected analysis to succeed before transforming.", null));
            } else {
                Collection<ISpoofaxTransformUnit<ISpoofaxAnalyzeUnit>> results =
                    transformService.transform(a, ctx, goal, new TransformConfig(true));
                result = getTransformResult(goal, results, test, messages);
            }
        } else {
            Collection<ISpoofaxTransformUnit<ISpoofaxParseUnit>> results = transformService
                .transform(input.getFragmentResult().getParseResult(), ctx, goal, new TransformConfig(true));
            result = getTransformResult(goal, results, test, messages);
        }
        if(result != null && result.success()) {
            return result.ast();
        } else if(result != null) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                String.format("Transformation %1$s failed", goal), null));
            MessageUtil.propagateMessages(result.messages(), messages, test.getDescriptionRegion(),
                test.getFragment().getRegion());
        }
        return null;
    }

    // get the transform unit from the results
    protected static @Nullable <T> T getTransformResult(ITransformGoal goal, Collection<T> results, ITestCase test,
        Collection<IMessage> messages) {
        if(results.isEmpty()) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                String.format("Transformation %1$s gave no results.", goal), null));
        } else if(results.size() > 1) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                String.format("Transformation %1$s gave more than 1 result.", goal), null));
        } else {
            return results.iterator().next();
        }
        return null;
    }
}
