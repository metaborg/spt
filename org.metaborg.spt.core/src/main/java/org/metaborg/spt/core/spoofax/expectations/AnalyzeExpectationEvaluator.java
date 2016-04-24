package org.metaborg.spt.core.spoofax.expectations;

import java.util.Collection;
import java.util.List;

import org.metaborg.core.context.IContext;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.IFragment;
import org.metaborg.spt.core.ITestCase;
import org.metaborg.spt.core.ITestExpectationInput;
import org.metaborg.spt.core.ITestExpectationOutput;
import org.metaborg.spt.core.TestExpectationOutput;
import org.metaborg.spt.core.TestPhase;
import org.metaborg.spt.core.expectations.AnalysisMessageExpectation;
import org.metaborg.spt.core.expectations.MessageUtil;
import org.metaborg.spt.core.spoofax.ISpoofaxExpectationEvaluator;

import com.google.common.collect.Lists;

public class AnalyzeExpectationEvaluator implements ISpoofaxExpectationEvaluator<AnalysisMessageExpectation> {

    @Override public Collection<Integer> usesSelections(IFragment fragment, AnalysisMessageExpectation expectation) {
        // we claim the first n selections for an 'n errors' expectation
        Collection<Integer> used = Lists.newLinkedList();
        // but only if there are enough selections
        if(fragment.getSelections().size() >= expectation.num()) {
            for(int i = 0; i < expectation.num(); i++) {
                used.add(i);
            }
        }
        return used;
    }

    @Override public TestPhase getPhase(IContext unused, AnalysisMessageExpectation expectation) {
        return TestPhase.ANALYSIS;
    }

    @Override public ITestExpectationOutput evaluate(
        ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input, AnalysisMessageExpectation expectation) {
        List<IMessage> messages = Lists.newLinkedList();

        ITestCase test = input.getTestCase();

        /*
         * TODO: should we check this before invoking every ANALYSIS/TRANSFORMATION phase test expectation? Now we have
         * to manually check this in each expectation, but maybe at some point we want to write a test expectation that
         * is ok with analysis failing. Will that ever happen? Do we want to disallow it just to save us from copying
         * over this code block to each analysis phase expectation?
         */
        ISpoofaxAnalyzeUnit analysisResult = input.getAnalysisResult();
        if(analysisResult == null) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Expected analysis to succeed", null));
            return new TestExpectationOutput(false, messages);
        }

        Iterable<IMessage> analysisMessages = input.getAnalysisResult().messages();

        final boolean success =
            checkMessages(test, analysisMessages, expectation.severity(), expectation.num(), messages);

        return new TestExpectationOutput(success, messages);
    }

    /**
     * Check if the number of messages in the given analysisMessages of the given severity matches the given expected
     * number of messages of this severity.
     * 
     * Also checks if all selections of the test case capture a message of the given severity. It is allowed to have
     * uncaptured messages, but it's not allowed to have selection that don't capture a message. A selection captures a
     * message if the selection's region contains the message's region. Note that this means that selection are allowed
     * to be wider than the actual message!
     */
    private boolean checkMessages(ITestCase test, Iterable<IMessage> analysisMessages, MessageSeverity severity,
        int expectedNumMessages, Collection<IMessage> messages) {
        // collect the messages of the given severity
        List<IMessage> interestingMessages = Lists.newLinkedList();
        for(IMessage message : analysisMessages) {
            if(severity == message.severity()) {
                interestingMessages.add(message);
            }
        }

        // check the number of messages
        if(interestingMessages.size() != expectedNumMessages) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Expected " + expectedNumMessages + " " + severity + "s, but got " + interestingMessages.size(), null));
        }

        /*
         * Check if all selections capture a message we are interested in. Not all messages have to be captured by a
         * selection, but all selections have to capture an interesting message.
         */
        for(ISourceRegion selection : test.getFragment().getSelections()) {
            boolean found = false;
            for(IMessage error : interestingMessages) {
                if(error.region() != null && selection.contains(error.region())) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                messages.add(MessageFactory.newAnalysisError(test.getResource(), selection,
                    "Expected an " + severity + " at this selection, but didn't find one.", null));
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
