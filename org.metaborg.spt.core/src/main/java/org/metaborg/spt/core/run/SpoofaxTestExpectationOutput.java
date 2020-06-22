package org.metaborg.spt.core.run;

import org.metaborg.core.messages.IMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Concrete instance of the output of evaluating a test expectation when running the test on Spoofax languages.
 */
public final class SpoofaxTestExpectationOutput implements ISpoofaxTestExpectationOutput {

    private final boolean success;
    private final List<IMessage> messages;
    private final List<ISpoofaxFragmentResult> fragmentResults;

    /**
     * Initializes a new instance of the {@link SpoofaxTestExpectationOutput} class.
     *
     * @param success whether the test expectation was met
     * @param messages the messages returned by evaluating the test expectation
     * @param fragmentResults the results of the fragments that where part of the expectation
     */
    public SpoofaxTestExpectationOutput(boolean success, Collection<IMessage> messages, Collection<ISpoofaxFragmentResult> fragmentResults) {
        this.success = success;
        this.messages = copyFromCollection(messages);
        this.fragmentResults = copyFromCollection(fragmentResults);
    }

    @Override public boolean isSuccessful() {
        return success;
    }

    @Override public List<IMessage> getMessages() {
        return messages;
    }

    @Override public List<ISpoofaxFragmentResult> getFragmentResults() {
        return fragmentResults;
    }

    /**
     * Copies the given collection.
     *
     * @param collection the collection to copy
     * @param <T> the type of elements in the collection
     * @return the copied collection
     */
    private static <T> List<T> copyFromCollection(Collection<T> collection) {
        if (collection.isEmpty()) {
            return Collections.emptyList();
        } else if (collection.size() == 1) {
            return Collections.singletonList(collection.iterator().next());
        } else {
            return Collections.unmodifiableList(new ArrayList<>(collection));
        }
    }

}
