package org.metaborg.spt.core.expectations;

import java.util.Collection;

import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageBuilder;
import org.metaborg.core.source.ISourceRegion;

public class MessageUtil {

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
     */
    public static void propagateMessages(Iterable<IMessage> toPropagate, Collection<IMessage> messages,
        ISourceRegion defaultRegion) {
        for(IMessage message : toPropagate) {
            if(message.region() == null) {
                // assign the message to the test's description if it has no region
                messages.add(setRegion(message, defaultRegion));
            } else {
                messages.add(message);
            }
        }
    }
}
