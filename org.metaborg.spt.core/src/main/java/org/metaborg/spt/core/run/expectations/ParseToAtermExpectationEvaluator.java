package org.metaborg.spt.core.run.expectations;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.metaborg.core.context.IContext;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.TestPhase;
import org.metaborg.mbt.core.run.ITestExpectationInput;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.expectations.ParseToAtermExpectation;
import org.metaborg.spt.core.run.ISpoofaxExpectationEvaluator;
import org.metaborg.spt.core.run.ISpoofaxFragmentResult;
import org.metaborg.spt.core.run.ISpoofaxTestExpectationOutput;
import org.metaborg.spt.core.run.SpoofaxTestExpectationOutput;
import org.metaborg.util.iterators.Iterables2;
import org.strategoxt.lang.TermEqualityUtil;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Check if the input fragment parsed to the expected ATerm AST.
 */
public class ParseToAtermExpectationEvaluator implements ISpoofaxExpectationEvaluator<ParseToAtermExpectation> {

    private final ITermFactoryService termFactoryService;

    @Inject public ParseToAtermExpectationEvaluator(ITermFactoryService termFactoryService) {
        this.termFactoryService = termFactoryService;
    }

    @Override public Collection<Integer> usesSelections(IFragment fragment, ParseToAtermExpectation expectation) {
        return Lists.newLinkedList();
    }

    @Override public TestPhase getPhase(IContext languageUnderTestCtx, ParseToAtermExpectation expectation) {
        return TestPhase.PARSING;
    }

    @Override public ISpoofaxTestExpectationOutput evaluate(
        ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input, ParseToAtermExpectation expectation) {

        ISpoofaxParseUnit p = input.getFragmentResult().getParseResult();
        ITestCase test = input.getTestCase();
        final boolean success;

        List<IMessage> messages = new LinkedList<>();
        Iterable<ISpoofaxFragmentResult> fragmentResults = Iterables2.empty();

        if(p == null || !p.success()) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Expected the input fragment to parse successfully.", null));
            return new SpoofaxTestExpectationOutput(false, messages, fragmentResults);
        }

        // compare the parse result
        success = TermEqualityUtil.equalsIgnoreAnnos(p.ast(), expectation.expectedResult(),
            termFactoryService.get(input.getLanguageUnderTest(), test.getProject(), false));
        if(!success) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                String.format(
                    "The fragment did not parse to the expected ATerm.\nParse result was: %1$s\nExpected result was: %2$s",
                    p.ast(), expectation.expectedResult()),
                null));
        }
        return new SpoofaxTestExpectationOutput(success, messages, fragmentResults);
    }

}
