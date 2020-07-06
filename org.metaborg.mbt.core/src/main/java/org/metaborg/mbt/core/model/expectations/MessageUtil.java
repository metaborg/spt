package org.metaborg.mbt.core.model.expectations;

import java.util.Collection;

import javax.annotation.Nullable;

import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageBuilder;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.source.SourceRegion;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class MessageUtil {

    private static final ILogger logger = LoggerUtils.logger(MessageUtil.class);

    /**
     * Create a new message with the same information, but the given region.
     * 
     * @param m
     *            the message to copy.
     * @param r
     *            the region to set on the new message.
     * @return the new message with the new region.
     */
    public static IMessage setRegion(IMessage m, ISourceRegion r) {
        MessageBuilder b = MessageBuilder.create();
        b.withMessage(m.message());
        b.withSeverity(m.severity());
        b.withType(m.type());
        b.withRegion(r);
        if(m.source() != null) {
            b.withSource(m.source());
        }
        if(m.exception() != null) {
            b.withException(m.exception());
        }
        return b.build();
    }

    /**
     * Create a new message with the same information, but the given message text.
     * 
     * @param m
     *            the message to copy.
     * @param msg
     *            the message text to change to.
     * @return the new message with the new message text.
     */
    public static IMessage setMessage(IMessage m, String msg) {
        MessageBuilder b = MessageBuilder.create();
        b.withMessage(msg);
        b.withSeverity(m.severity());
        b.withType(m.type());
        b.withRegion(m.region());
        if(m.source() != null) {
            b.withSource(m.source());
        }
        if(m.exception() != null) {
            b.withException(m.exception());
        }
        return b.build();
    }

    /**
     * Adds all messages in toPropagate to the given collection.
     * 
     * If any of the propagated messages does not have a region, it will be set to the given default region. This is
     * useful to bind messages from analyzing or parsing a fragment to the top of the test case, if these messages for
     * some reason don't have a region.
     * 
     * @param toPropagate
     *            the messages to propagate and add to messages.
     * @param messages
     *            the collection of messages to copy the messages to.
     * @param defaultRegion
     *            the default region to use if any of the propagated messages doesn't have a region.
     * @param bounds
     *            the region to which we should limit error messages. If the message has a region completely outside of
     *            the bounds, the default region will be used instead. If the region crosses one side of bounds, it will
     *            be bounded to be within the given bounded region. If the region is inside the bounds, the message
     *            won't have to be altered.
     */
    public static void propagateMessages(Iterable<IMessage> toPropagate, Collection<IMessage> messages,
        ISourceRegion defaultRegion, @Nullable ISourceRegion bounds) {
        logger.debug("Propagating messages: {}", toPropagate);
        for(IMessage message : toPropagate) {
            ISourceRegion region = message.region();
            if(bounds == null && region != null) {
                logger.debug("Propagating {} at its own region {}, because no bounds were given", message.message(),
                    region);
                messages.add(message);
            } else if(region == null || region.endOffset() < bounds.startOffset()
                || region.startOffset() > bounds.endOffset()) {
                logger.debug("Propagating '{}' at the default region due to bounds {}, not its own region {}",
                    message.message(), bounds, region);
                messages.add(setMessage(setRegion(message, defaultRegion),
                    message.message() + " (Relocated this message. Original location: (" + region + "))"));
            } else if(region.startOffset() < bounds.startOffset() && region.endOffset() > bounds.endOffset()) {
                logger.debug("Propagating '{}' and cutting of the beginning and end offsets", message.message());
                messages.add(setRegion(message, new SourceRegion(bounds.startOffset(), bounds.endOffset())));
            } else if(region.startOffset() < bounds.startOffset()) {
                logger.debug("Propagating '{}' and cutting of the beginning offset", message.message());
                messages.add(setRegion(message, new SourceRegion(bounds.startOffset(), region.endOffset())));
            } else if(region.endOffset() > bounds.endOffset()) {
                logger.debug("Propagating '{}' and cutting of the end offset", message.message());
                messages.add(setRegion(message, new SourceRegion(region.startOffset(), bounds.endOffset())));
            } else {
                logger.debug("Propagating '{}' at its own region {}, as it was within the bounds", message.message(),
                    region);
                messages.add(message);
            }
        }
    }
}
