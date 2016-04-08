package org.metaborg.spt.core.expectations;

import java.util.Collection;
import java.util.List;

import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spt.core.ITestCase;
import org.metaborg.spt.core.ITestExpectation;
import org.metaborg.spt.core.ITestExpectationInput;
import org.metaborg.spt.core.ITestExpectationOutput;
import org.metaborg.spt.core.TestExpectationOutput;
import org.metaborg.spt.core.TestPhase;
import org.metaborg.spt.core.util.SPTUtil;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class AnalyzeExpectation implements ITestExpectation {

    private static final String ERR = "Errors";
    private static final String WARN = "Warnings";
    private static final String NOTE = "Notes";

    private final ISpoofaxTracingService traceService;

    @Inject public AnalyzeExpectation(ISpoofaxTracingService traceService) {
        this.traceService = traceService;
    }

    @Override public boolean canEvaluate(IStrategoTerm expectationTerm) {
        String cons = SPTUtil.consName(expectationTerm);
        return ERR.equals(cons) || WARN.equals(cons) || NOTE.equals(cons);
    }

    @Override public TestPhase getPhase(IStrategoTerm expectationTerm) {
        return TestPhase.ANALYSIS;
    }

    @Override public ITestExpectationOutput evaluate(ITestExpectationInput input) {
        List<IMessage> messages = Lists.newLinkedList();
        final boolean success;

        ITestCase test = input.getTestCase();
        IStrategoTerm expectation = input.getExpectation();

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

        switch(SPTUtil.consName(expectation)) {
            case ERR:
                // it's an Errors(n) term
                success = checkMessages(test, expectation, analysisMessages, MessageSeverity.ERROR,
                    Term.asJavaInt(expectation.getSubterm(0)), messages);
                break;
            case WARN:
                // it's a Warnings(n) term
                success = checkMessages(test, expectation, analysisMessages, MessageSeverity.WARNING,
                    Term.asJavaInt(expectation.getSubterm(0)), messages);
                break;
            case NOTE:
                // it's a Notes(n) term
                success = checkMessages(test, expectation, analysisMessages, MessageSeverity.NOTE,
                    Term.asJavaInt(expectation.getSubterm(0)), messages);
                break;
            default:
                throw new IllegalArgumentException("This test expectation can't evaluate " + expectation);
        }

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
    private boolean checkMessages(ITestCase test, IStrategoTerm expectation, Iterable<IMessage> analysisMessages,
        MessageSeverity severity, int expectedNumMessages, Collection<IMessage> messages) {
        // collect the messages of the given severity
        List<IMessage> interestingMessages = Lists.newLinkedList();
        for(IMessage message : analysisMessages) {
            if(severity == message.severity()) {
                interestingMessages.add(message);
            }
        }

        // check the number of messages
        if(interestingMessages.size() != expectedNumMessages) {
            ISourceLocation loc = traceService.location(expectation);
            ISourceRegion region = loc == null ? test.getDescriptionRegion() : loc.region();
            messages.add(MessageFactory.newAnalysisError(test.getResource(), region,
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
            MessageUtil.propagateMessages(analysisMessages, messages, test.getDescriptionRegion());
            return false;
        }
    }
}
