package org.metaborg.mbt.core.model.expectations;

import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.source.ISourceRegion;

/**
 * Generic expectation for:
 * 
 * <ul>
 * <li>n errors</li>
 * <li>n warnings</li>
 * <li>n notes</li>
 * </ul>
 */
public class AnalysisMessageExpectation extends ATestExpectation {

    private final int num;
    private final MessageSeverity severity;

    public AnalysisMessageExpectation(ISourceRegion region, int num, MessageSeverity severity) {
        super(region);
        this.num = num;
        this.severity = severity;
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
}
