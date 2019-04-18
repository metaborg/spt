package org.metaborg.spt.core.run.expectations;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.TestPhase;
import org.metaborg.mbt.core.run.ITestExpectationInput;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.SPTUtil;
import org.metaborg.spt.core.expectations.ParseToAtermExpectation;
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

/**
 * Check if the input fragment parsed to the expected ATerm AST.
 */
public class ParseToAtermExpectationEvaluator implements ISpoofaxExpectationEvaluator<ParseToAtermExpectation> {

    private static final ILogger logger = LoggerUtils.logger(ParseToAtermExpectationEvaluator.class);

    private final ITermFactoryService termFactoryService;
    private final ISpoofaxTracingService traceService;

    @Inject public ParseToAtermExpectationEvaluator(ITermFactoryService termFactoryService,
        ISpoofaxTracingService traceService) {
        this.termFactoryService = termFactoryService;
        this.traceService = traceService;
    }

    @Override public Collection<Integer> usesSelections(IFragment fragment, ParseToAtermExpectation expectation) {
        return Lists.newLinkedList();
    }

    @Override public TestPhase getPhase(ILanguageImpl language, ParseToAtermExpectation expectation) {
        return TestPhase.PARSING;
    }

    @Override public ISpoofaxTestExpectationOutput evaluate(
        ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input, ParseToAtermExpectation expectation) {

        ISpoofaxParseUnit p = input.getFragmentResult().getParseResult();
        ITestCase test = input.getTestCase();

        List<IMessage> messages = new LinkedList<>();
        Iterable<ISpoofaxFragmentResult> fragmentResults = Iterables2.empty();

        if(p == null || !p.success()) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Expected the input fragment to parse successfully.", null));
            return new SpoofaxTestExpectationOutput(false, messages, fragmentResults);
        }

        // compare the parse result
        // but only the part of the test's fragment, not the parts of the test fixture
        ISourceRegion fragmentRegion = test.getFragment().getRegion();
        Iterable<IStrategoTerm> terms = traceService.fragmentsWithin(p, fragmentRegion);
        logger.debug("Fragment region: {}\nFragment terms: {}", fragmentRegion, terms);
        boolean success = false;
        String latestMessage = "The fragment was empty.";
        for(IStrategoTerm term : terms) {
            if(SPTUtil.checkATermMatch(term, expectation.expectedResult(),
                termFactoryService.get(input.getLanguageUnderTest(), test.getProject(), false))) {
                success = true;
                break;
            } else {
                latestMessage = String.format(
                    "The fragment did not parse to the expected ATerm.\nParse result was: %1$s\nExpected result was: %2$s",
                    term, SPTUtil.prettyPrintMatch(expectation.expectedResult()));
            }
        }
        if(!success) {
            messages.add(
                MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(), latestMessage, null));
        }
        return new SpoofaxTestExpectationOutput(success, messages, fragmentResults);
    }

}
