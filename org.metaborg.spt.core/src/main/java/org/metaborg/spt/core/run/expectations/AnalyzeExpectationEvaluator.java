package org.metaborg.spt.core.run.expectations;

import java.util.Collection;
import java.util.List;

import org.metaborg.core.context.IContext;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.TestPhase;
import org.metaborg.mbt.core.model.expectations.AnalysisMessageExpectation;
import org.metaborg.mbt.core.model.expectations.MessageUtil;
import org.metaborg.mbt.core.run.ITestExpectationInput;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.run.ISpoofaxExpectationEvaluator;
import org.metaborg.spt.core.run.ISpoofaxFragmentResult;
import org.metaborg.spt.core.run.ISpoofaxTestExpectationOutput;
import org.metaborg.spt.core.run.SpoofaxTestExpectationOutput;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Lists;

public class AnalyzeExpectationEvaluator implements ISpoofaxExpectationEvaluator<AnalysisMessageExpectation> {

    // private static final ILogger logger = LoggerUtils.logger(AnalyzeExpectationEvaluator.class);

    @Override public Collection<Integer> usesSelections(IFragment fragment, AnalysisMessageExpectation expectation) {
        return Lists.newArrayList(expectation.selections());
    }

    @Override public TestPhase getPhase(IContext unused, AnalysisMessageExpectation expectation) {
        return TestPhase.ANALYSIS;
    }

    @Override public ISpoofaxTestExpectationOutput evaluate(
        ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input, AnalysisMessageExpectation expectation) {
        List<IMessage> messages = Lists.newLinkedList();
        // analysis expectations don't have output fragments (not at the moment anyway)
        final Iterable<ISpoofaxFragmentResult> fragmentResults = Iterables2.empty();

        ITestCase test = input.getTestCase();

        /*
         * TODO: should we check this before invoking every ANALYSIS/TRANSFORMATION phase test expectation? Now we have
         * to manually check this in each expectation, but maybe at some point we want to write a test expectation that
         * is ok with analysis failing. Will that ever happen? Do we want to disallow it just to save us from copying
         * over this code block to each analysis phase expectation?
         */
        ISpoofaxAnalyzeUnit analysisResult = input.getFragmentResult().getAnalysisResult();
        if(analysisResult == null) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Expected analysis to succeed", null));
            return new SpoofaxTestExpectationOutput(false, messages, fragmentResults);
        }

        Iterable<IMessage> analysisMessages = input.getFragmentResult().getAnalysisResult().messages();

        final boolean success = checkMessages(test, analysisMessages, expectation.severity(), expectation.num(),
            expectation.selections(), messages);

        return new SpoofaxTestExpectationOutput(success, messages, fragmentResults);
    }

    /**
     * Check if the number of messages in the given analysisMessages of the given severity matches the given expected
     * number of messages of this severity.
     * 
     * Only considers messages that are within the bounds of the fragment (so it ignores any messages that are on the
     * test fixture).
     * 
     * Also make sure the locations of the messages are correct if any selections were given (e.g. '2 errors at #1,
     * #2').
     */
    private boolean checkMessages(ITestCase test, Iterable<IMessage> analysisMessages, MessageSeverity severity,
        int expectedNumMessages, Iterable<Integer> selectionRefs, Collection<IMessage> messages) {
        // collect the messages of the given severity and proper location
        List<IMessage> interestingMessages = Lists.newLinkedList();
        for(IMessage message : analysisMessages) {
            if(severity == message.severity()
                && (message.region() == null || test.getFragment().getRegion().contains(message.region()))) {
                interestingMessages.add(message);
            }
        }

        // check the number of messages
        if(interestingMessages.size() != expectedNumMessages) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Expected " + expectedNumMessages + " " + severity + "s, but got " + interestingMessages.size(), null));
        }

        // Check message locations
        final List<ISourceRegion> selections = test.getFragment().getSelections();
        final List<Integer> processedSelections = Lists.newArrayList();
        for(int i : selectionRefs) {
            if(i > selections.size()) {
                messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                    "Not enough selections in the fragment to resolve #" + i, null));
            } else {
                ISourceRegion selection = selections.get(i - 1);
                // we have to make sure that `2 errors at #1,#1` will look for BOTH errors at location #1
                int countRequired = 1;
                for(int j : processedSelections) {
                    if(i == j) {
                        countRequired++;
                    }
                }
                int found = 0;
                for(IMessage error : interestingMessages) {
                    if(error.region() != null && selection.contains(error.region())) {
                        found++;
                        if(found >= countRequired) {
                            break;
                        }
                    }
                }
                if(found < countRequired) {
                    messages.add(MessageFactory.newAnalysisError(test.getResource(), selection,
                        String.format("Expected %s %s%s at this selection, but found %s.", countRequired, severity,
                            countRequired == 1 ? "" : "s", found),
                        null));
                }
                processedSelections.add(i);
            }
        }

        if(messages.isEmpty()) {
            return true;
        } else {
            MessageUtil.propagateMessages(analysisMessages, messages, test.getDescriptionRegion(),
                test.getFragment().getRegion());
            return false;
        }
    }
}
