package org.metaborg.spt.core.expectations;

import javax.annotation.Nullable;

import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spt.core.IFragment;

/**
 * A generic expectation for:
 * 
 * <ul>
 * <li>parse succeeds</li>
 * <li>parse fails</li>
 * <li>parse to [language] [fragment]</li>
 * </ul>
 */
public class ParseExpectation extends AToPartExpectation {

    private final boolean successExpected;


    public ParseExpectation(ISourceRegion region, boolean sucessExpected) {
        this(region, sucessExpected, null, null, null);
    }

    public ParseExpectation(ISourceRegion region, boolean sucessExpected, @Nullable IFragment expectedResult,
        String expectedResultLanguage, ISourceRegion languageRegion) {
        super(region, expectedResult, expectedResultLanguage, languageRegion);
        this.successExpected = sucessExpected;
    }

    /**
     * Whether parsing is expected to succeed.
     */
    public boolean successExpected() {
        return successExpected;
    }

}
