package org.metaborg.spt.core.run;

import org.metaborg.core.messages.IMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SpoofaxTestExpectationOutput implements ISpoofaxTestExpectationOutput {

    private final boolean success;
    private final List<IMessage> messages;
    private final List<ISpoofaxFragmentResult> fragmentResults;

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
