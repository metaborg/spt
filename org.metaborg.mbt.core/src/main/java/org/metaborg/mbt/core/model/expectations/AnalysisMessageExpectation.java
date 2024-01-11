package org.metaborg.mbt.core.model.expectations;

import jakarta.annotation.Nullable;

import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.source.ISourceRegion;

/**
 * Generic expectation for analysis message test expectations.
 *
 * This covers both the '= 3 errors at #1, #2, #3' expectations, and the 'warning like "some message" at #1'
 * expectations.
 */
public class AnalysisMessageExpectation extends ATestExpectation {

    private final int num;
    private final MessageSeverity severity;
    private final Iterable<Integer> selections;
    private final Operation op;
    // the string contents of the 'like' expectation
    private final String content;

    public AnalysisMessageExpectation(ISourceRegion region, int num, MessageSeverity severity,
        Iterable<Integer> selections, Operation op, @Nullable String content) {
        super(region);
        this.num = num;
        this.severity = severity;
        this.selections = selections;
        this.op = op;
        this.content = content;
    }

    /**
     * The number of messages of this specific severity that were expected.
     */
    public int num() {
        return num;
    }

    /**
     * The severity of the messages we reason about.
     */
    public MessageSeverity severity() {
        return severity;
    }

    /**
     * The references to the selections at which the messages are expected.
     * 
     * May be empty. May be smaller than the number of expected messages.
     */
    public Iterable<Integer> selections() {
        return selections;
    }

    /**
     * The operation that should be used to check against the expected number of messages.
     */
    public Operation operation() {
        return op;
    }

    /**
     * The contents that are supposed to be part of a message.
     */
    public @Nullable String content() {
        return content;
    }

    /**
     * The operation for the amount of expected errors.
     */
    public enum Operation {
        EQUAL, LESS, LESS_OR_EQUAL, MORE, MORE_OR_EQUAL
    }
}
