package org.metaborg.spt.core.expectations;

import javax.annotation.Nullable;

import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spt.core.IFragment;

/**
 * A superclass for expectations that have an optional 'to [fragment]' part.
 */
public abstract class AToPartExpectation extends ATestExpectation {

    private final @Nullable IFragment expectedResult;
    private final @Nullable String expectedResultLanguage;
    private final @Nullable ISourceRegion languageNameRegion;

    public AToPartExpectation(ISourceRegion region) {
        this(region, null, null, null);
    }

    public AToPartExpectation(ISourceRegion region, IFragment fragment) {
        this(region, fragment, null, null);
    }

    public AToPartExpectation(ISourceRegion region, IFragment fragment, String langName, ISourceRegion langNameRegion) {
        super(region);
        this.expectedResult = fragment;
        this.expectedResultLanguage = langName;
        this.languageNameRegion = langNameRegion;
    }

    /**
     * The output fragment.
     * 
     * May be null if the 'to [fragment]' part is optional.
     */
    public @Nullable IFragment outputFragment() {
        return expectedResult;
    }

    /**
     * The name of the language that should be used for the output fragment of a 'parse to'.
     * 
     * May be null, if either the output fragment is null, or if the language should be determined in some implicit way.
     */
    public @Nullable String outputLanguage() {
        return expectedResultLanguage;
    }

    /**
     * The region spanned by the language name.
     * 
     * Should only be null if the {@link #outputLanguage()} is null.
     */
    public @Nullable ISourceRegion outputLanguageRegion() {
        return languageNameRegion;
    }

}
