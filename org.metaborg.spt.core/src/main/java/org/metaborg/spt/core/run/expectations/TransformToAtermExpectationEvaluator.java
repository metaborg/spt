package org.metaborg.spt.core.run.expectations;

import java.util.Collection;
import java.util.List;

import org.metaborg.core.action.ITransformGoal;
import org.metaborg.core.context.ContextException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.context.ITemporaryContext;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.transform.TransformException;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.TestPhase;
import org.metaborg.mbt.core.run.ITestExpectationInput;
import org.metaborg.spoofax.core.action.ActionFacet;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.core.transform.ISpoofaxTransformService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.expectations.TransformToAtermExpectation;
import org.metaborg.spt.core.run.ISpoofaxExpectationEvaluator;
import org.metaborg.spt.core.run.ISpoofaxFragmentResult;
import org.metaborg.spt.core.run.ISpoofaxTestExpectationOutput;
import org.metaborg.spt.core.run.SpoofaxTestExpectationOutput;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.TermEqualityUtil;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Covers the Spoofax specific TransformationToAtermExpectation.
 * 
 * It requires parsing or analyzing the input fragment, depending on what the transformation requires. Then it compares
 * the resulting AST with the expected ATerm result.
 */
public class TransformToAtermExpectationEvaluator implements ISpoofaxExpectationEvaluator<TransformToAtermExpectation> {

    private static final ILogger logger = LoggerUtils.logger(TransformToAtermExpectationEvaluator.class);

    private final ISpoofaxTransformService transformService;
    private final IContextService contextService;
    private final ITermFactoryService termFactoryService;

    @Inject public TransformToAtermExpectationEvaluator(ISpoofaxTransformService transformService,
        IContextService contextService, ITermFactoryService termFactoryService) {
        this.transformService = transformService;
        this.contextService = contextService;
        this.termFactoryService = termFactoryService;
    }

    @Override public Collection<Integer> usesSelections(IFragment fragment, TransformToAtermExpectation expectation) {
        return Lists.newLinkedList();
    }

    @Override public TestPhase getPhase(IContext languageUnderTestCtx, TransformToAtermExpectation expectation) {
        return transformService.requiresAnalysis(languageUnderTestCtx, expectation.goal()) ? TestPhase.ANALYSIS
            : TestPhase.PARSING;
    }

    @Override public ISpoofaxTestExpectationOutput evaluate(
        ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input, TransformToAtermExpectation expectation) {
        boolean success = false;
        final ITestCase test = input.getTestCase();
        final ILanguageImpl lut = input.getLanguageUnderTest();
        final List<IMessage> messages = Lists.newLinkedList();
        final Iterable<ISpoofaxFragmentResult> fragmentResults = Iterables2.empty();

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
            IStrategoTerm result = TransformExpectationEvaluator.transform(input, expectation.goal(), ctx, test,
                messages, useAnalysis, transformService);
            if(result != null) {
                // do stuff to the output fragment
                final IStrategoTerm out = expectation.expectedResult();
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
            } else {
                messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                    String.format("Transformation %1$s failed.", expectation.goal()), null));
            }
        } catch(TransformException e) {
            logger.debug("An exception occured while trying to transform {}.", e, expectation.goal());
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                String.format("An exception occured while trying to transform %1$s.", expectation.goal()), e));
        }

        if(tempCtx != null) {
            tempCtx.close();
        }
        return new SpoofaxTestExpectationOutput(success, messages, fragmentResults);
    }

}
