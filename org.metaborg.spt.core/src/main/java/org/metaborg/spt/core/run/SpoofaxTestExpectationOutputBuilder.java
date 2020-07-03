package org.metaborg.spt.core.run;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.Message;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.messages.MessageType;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.expectations.MessageUtil;
import org.metaborg.mbt.core.run.IFragmentResult;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

/**
 * A builder for {@link ISpoofaxTestExpectationOutput} objects.
 */
public final class SpoofaxTestExpectationOutputBuilder implements ISpoofaxTestExpectationOutputBuilder {

    private final ITestCase testCase;
    private final List<IMessage> messages;
    private final List<ISpoofaxFragmentResult> fragmentResults;
    @Nullable private final FileObject resource;
    @Nullable private final ISourceRegion region;

    /**
     * Initializes a new instance of the {@link SpoofaxTestExpectationOutputBuilder} class.
     *
     * The default resource for messages is the resource of the test case;
     * the default region for messages is the description region of the test case.
     *
     * @param testCase the test case
     */
    public SpoofaxTestExpectationOutputBuilder(ITestCase testCase) {
        this(testCase, new LinkedList<>(), new LinkedList<>(), testCase.getResource(), testCase.getDescriptionRegion());
    }

    /**
     * Initializes a new instance of the {@link SpoofaxTestExpectationOutputBuilder} class.
     *
     * @param testCase the test case
     * @param messages the messages list
     * @param fragmentResults the fragment results list
     * @param resource the resource for messages; or {@code null}
     * @param region the region for messages; or {@code null}
     */
    private SpoofaxTestExpectationOutputBuilder(ITestCase testCase, List<IMessage> messages, List<ISpoofaxFragmentResult> fragmentResults, @Nullable FileObject resource, @Nullable ISourceRegion region) {
        this.testCase = testCase;
        this.messages = messages;
        this.fragmentResults = fragmentResults;
        this.resource = resource;
        this.region = region;
    }

    @Override
    public SpoofaxTestExpectationOutput build(boolean success) {
        return new SpoofaxTestExpectationOutput(success, this.messages, this.fragmentResults);
    }

    @Override
    public ISpoofaxTestExpectationOutputBuilder withResource(@Nullable FileObject resource) {
        return new SpoofaxTestExpectationOutputBuilder(this.testCase, this.messages, this.fragmentResults, resource, this.region);
    }

    @Override
    public ISpoofaxTestExpectationOutputBuilder withRegion(@Nullable ISourceRegion region) {
        return new SpoofaxTestExpectationOutputBuilder(this.testCase, this.messages, this.fragmentResults, this.resource, region);
    }

    @Override
    public void addFragmentResult(ISpoofaxFragmentResult result) {
        fragmentResults.add(result);
    }

    @Override
    public void addFragmentResult(IFragmentResult<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> result) {
        if (!(result instanceof ISpoofaxFragmentResult))
            throw new IllegalArgumentException("The fragment result must implement ISpoofaxFragmentResult.");
        addFragmentResult((ISpoofaxFragmentResult)result);
    }

    @Override
    public boolean hasMessages(@Nullable MessageType type, @Nullable MessageSeverity severity) {
        return messages.stream().anyMatch(m ->
                (type == null || m.type().equals(type)) &&
                (severity == null || m.severity().equals(severity))
        );
    }

    @Override
    public Message addMessage(MessageType type, MessageSeverity severity, String message, @Nullable Throwable cause) {
        final Message messageObj = MessageFactory.newMessage(resource, region, message, severity, type, cause);
        messages.add(messageObj);
        return messageObj;
    }

    @Override
    public void propagateMessages(Iterable<IMessage> messages, ISourceRegion bounds) {
        MessageUtil.propagateMessages(messages, this.messages, region, bounds);
    }

    @Override
    public void addMessage(IMessage message) {

    }

}
