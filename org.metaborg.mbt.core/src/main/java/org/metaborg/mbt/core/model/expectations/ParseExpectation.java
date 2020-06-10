package org.metaborg.mbt.core.model.expectations;

import javax.annotation.Nullable;

import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;

/**
 * A generic expectation.
 *
 * This is used for:
 * 
 * <ul>
 * <li>parse succeeds</li>
 * <li>parse fails</li>
 * <li>parse to [language?] [fragment]</li>
 * </ul>
 */
public class ParseExpectation extends AToPartExpectation {

    private final boolean successExpected;


    public ParseExpectation(ISourceRegion region, boolean successExpected) {
        this(region, successExpected, null, null, null);
    }

    public ParseExpectation(ISourceRegion region, boolean successExpected, @Nullable IFragment expectedResult,
        @Nullable String expectedResultLanguage, @Nullable ISourceRegion languageRegion) {
        super(region, expectedResult, expectedResultLanguage, languageRegion);
        this.successExpected = successExpected;
    }

    /**
     * Whether parsing is expected to succeed.
     */
    public boolean successExpected() {
        return successExpected;
    }

}
