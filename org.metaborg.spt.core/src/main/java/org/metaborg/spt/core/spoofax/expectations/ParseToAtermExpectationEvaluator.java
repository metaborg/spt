package org.metaborg.spt.core.spoofax.expectations;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.metaborg.core.context.IContext;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.IFragment;
import org.metaborg.spt.core.ITestCase;
import org.metaborg.spt.core.ITestExpectationInput;
import org.metaborg.spt.core.ITestExpectationOutput;
import org.metaborg.spt.core.TestExpectationOutput;
import org.metaborg.spt.core.TestPhase;
import org.metaborg.spt.core.spoofax.ISpoofaxExpectationEvaluator;
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

    @Override public ITestExpectationOutput evaluate(
        ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input, ParseToAtermExpectation expectation) {

        ISpoofaxParseUnit p = input.getParseResult();
        ITestCase test = input.getTestCase();
        final boolean success;

        List<IMessage> messages = new LinkedList<>();

        if(p == null || !p.success()) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Expected the input fragment to parse successfully.", null));
            return new TestExpectationOutput(false, messages);
        }

        // compare the parse result
        success = TermEqualityUtil.equalsIgnoreAnnos(p.ast(), expectation.expectedResult(),
            termFactoryService.get(input.getLanguageUnderTest()));
        if(!success) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                String.format(
                    "The fragment did not parse to the expected ATerm.\nParse result was: %1$s\nExpected result was: %2$s",
                    p.ast(), expectation.expectedResult()),
                null));
        }
        return new TestExpectationOutput(success, messages);
    }

}
