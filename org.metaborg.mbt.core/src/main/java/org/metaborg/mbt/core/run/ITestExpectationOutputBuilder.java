package org.metaborg.mbt.core.run;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.messages.MessageType;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.syntax.IParseUnit;

import jakarta.annotation.Nullable;

/**
 * Builds the test expectation output.
 *
 * @param <P> the type of parse unit
 * @param <A> the type of analysis unit
 */
public interface ITestExpectationOutputBuilder<P extends IParseUnit, A extends IAnalyzeUnit> {

    /**
     * Builds a test expectation output.
     *
     * @param success whether the test expectation was met
     * @return the built test expectation output
     */
    ITestExpectationOutput<P, A> build(boolean success);


    /**
     * Returns an output builder whose messages have the specified resource.
     *
     * Any operations on the returned output builder affect this output builder too.
     *
     * @param resource the new resource; or {@code null}
     * @return the output builder
     */
    ITestExpectationOutputBuilder<P, A> withResource(@Nullable FileObject resource);

    /**
     * Returns an output builder whose messages have the specified region.
     *
     * Any operations on the returned output builder affect this output builder too.
     *
     * @param region the new region; or {@code null}
     * @return the output builder
     */
    ITestExpectationOutputBuilder<P, A> withRegion(@Nullable ISourceRegion region);


    // Fragment results
    /**
     * Adds a fragment result.
     *
     * @param result the fragment result
     */
    void addFragmentResult(IFragmentResult<P, A> result);



    // Has Messages
    /**
     * Whether any messages of the specified type and severity where added.
     *
     * @param type the type of messages; or {@code null} for any type
     * @param severity the severity of messages; or {@code null} for any severity
     * @return {@code true} when any messages of the specified type and severity where added;
     * otherwise, {@code false}
     */
    boolean hasMessages(@Nullable MessageType type, @Nullable MessageSeverity severity);

    /**
     * Whether any error messages where registered.
     *
     * @return {@code true} when any error messages where registered; otherwise, {@code false}
     */
    default boolean hasErrorMessages() {
        return hasMessages(null, MessageSeverity.ERROR);
    }


    // Propagating Messages
    /**
     * Adds the given messages to the given collection.
     *
     * @param messages the messages to propagate and add
     * @param bounds the region to which messages are limited
     */
    void propagateMessages(Iterable<IMessage> messages, ISourceRegion bounds);


    // Generic Messages
    /**
     * Adds a message.
     *
     * @param message the message to add
     */
    void addMessage(IMessage message);

    /**
     * Adds a message.
     *
     * @param type the message type
     * @param severity the message severity
     * @param message the message text
     * @param cause the throwable that caused this message; or {@code null}
     * @return the message that was added
     */
    IMessage addMessage(MessageType type, MessageSeverity severity, String message, @Nullable Throwable cause);

    /**
     * Adds a message.
     *
     * @param type the message type
     * @param severity the message severity
     * @param message the message text
     * @return the message that was added
     */
    default IMessage addMessage(MessageType type, MessageSeverity severity, String message) {
        return addMessage(type, severity, message, null);
    }



    // Analysis Messages
    /**
     * Adds an analysis error message.
     *
     * @param message the message text
     * @param cause the throwable that caused this message; or {@code null}
     * @return the message that was added
     */
    default IMessage addAnalysisError(String message, @Nullable Throwable cause) {
        return addMessage(MessageType.ANALYSIS, MessageSeverity.ERROR, message, cause);
    }

    /**
     * Adds an analysis error message.
     *
     * @param message the message text
     * @return the message that was added
     */
    default IMessage addAnalysisError(String message) {
        return addAnalysisError(message, null);
    }

    /**
     * Adds an analysis warning message.
     *
     * @param message the message text
     * @param cause the throwable that caused this message; or {@code null}
     * @return the message that was added
     */
    default IMessage addAnalysisWarning(String message, @Nullable Throwable cause) {
        return addMessage(MessageType.ANALYSIS, MessageSeverity.WARNING, message, cause);
    }

    /**
     * Adds an analysis warning message.
     *
     * @param message the message text
     * @return the message that was added
     */
    default IMessage addAnalysisWarning(String message) {
        return addAnalysisWarning(message, null);
    }

    /**
     * Adds an analysis note message.
     *
     * @param message the message text
     * @param cause the throwable that caused this message; or {@code null}
     * @return the message that was added
     */
    default IMessage addAnalysisNote(String message, @Nullable Throwable cause) {
        return addMessage(MessageType.ANALYSIS, MessageSeverity.NOTE, message, cause);
    }

    /**
     * Adds an analysis note message.
     *
     * @param message the message text
     * @return the message that was added
     */
    default IMessage addAnalysisNote(String message) {
        return addAnalysisWarning(message, null);
    }



    // Parser Messages
    /**
     * Adds a parser error message.
     *
     * @param message the message text
     * @param cause the throwable that caused this message; or {@code null}
     * @return the message that was added
     */
    default IMessage addParserError(String message, @Nullable Throwable cause) {
        return addMessage(MessageType.PARSER, MessageSeverity.ERROR, message, cause);
    }

    /**
     * Adds a parser error message.
     *
     * @param message the message text
     * @return the message that was added
     */
    default IMessage addParserError(String message) {
        return addParserError(message, null);
    }

    /**
     * Adds a parser warning message.
     *
     * @param message the message text
     * @param cause the throwable that caused this message; or {@code null}
     * @return the message that was added
     */
    default IMessage addParserWarning(String message, @Nullable Throwable cause) {
        return addMessage(MessageType.PARSER, MessageSeverity.WARNING, message, cause);
    }

    /**
     * Adds a parser warning message.
     *
     * @param message the message text
     * @return the message that was added
     */
    default IMessage addParserWarning(String message) {
        return addParserWarning(message, null);
    }

    /**
     * Adds a parser note message.
     *
     * @param message the message text
     * @param cause the throwable that caused this message; or {@code null}
     * @return the message that was added
     */
    default IMessage addParserNote(String message, @Nullable Throwable cause) {
        return addMessage(MessageType.PARSER, MessageSeverity.NOTE, message, cause);
    }

    /**
     * Adds a parser note message.
     *
     * @param message the message text
     * @return the message that was added
     */
    default IMessage addParserNote(String message) {
        return addParserNote(message, null);
    }



    // Builder Messages
    /**
     * Adds a builder error message.
     *
     * @param message the message text
     * @param cause the throwable that caused this message; or {@code null}
     * @return the message that was added
     */
    default IMessage addBuilderError(String message, @Nullable Throwable cause) {
        return addMessage(MessageType.TRANSFORMATION, MessageSeverity.ERROR, message, cause);
    }

    /**
     * Adds a builder error message.
     *
     * @param message the message text
     * @return the message that was added
     */
    default IMessage addBuilderError(String message) {
        return addParserError(message, null);
    }

    /**
     * Adds a builder warning message.
     *
     * @param message the message text
     * @param cause the throwable that caused this message; or {@code null}
     * @return the message that was added
     */
    default IMessage addBuilderWarning(String message, @Nullable Throwable cause) {
        return addMessage(MessageType.TRANSFORMATION, MessageSeverity.WARNING, message, cause);
    }

    /**
     * Adds a builder warning message.
     *
     * @param message the message text
     * @return the message that was added
     */
    default IMessage addBuilderWarning(String message) {
        return addParserWarning(message, null);
    }

    /**
     * Adds a builder note message.
     *
     * @param message the message text
     * @param cause the throwable that caused this message; or {@code null}
     * @return the message that was added
     */
    default IMessage addBuilderNote(String message, @Nullable Throwable cause) {
        return addMessage(MessageType.TRANSFORMATION, MessageSeverity.NOTE, message, cause);
    }

    /**
     * Adds a builder note message.
     *
     * @param message the message text
     * @return the message that was added
     */
    default IMessage addBuilderNote(String message) {
        return addParserNote(message, null);
    }

}
