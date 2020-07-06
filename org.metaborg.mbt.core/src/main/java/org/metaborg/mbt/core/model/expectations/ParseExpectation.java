package org.metaborg.mbt.core.model.expectations;

import javax.annotation.Nullable;

import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;

/**
 * A parser expectation.
 *
 * This is used for:
 * 
 * <ul>
 * <li>parse succeeds</li>
 * <li>parse fails</li>
 * <li>parse ambiguous</li>
 * <li>parse to [language?] [fragment]</li>
 * </ul>
 */
public class ParseExpectation extends AToPartExpectation {

    private final Result expectedResult;

    /**
     * Initializes a new instance of the {@link ParseExpectation} class.
     *
     * @param region the region
     * @param expectedResult the expected result
     */
    public ParseExpectation(ISourceRegion region, Result expectedResult) {
        this(region, expectedResult, null, null, null);
    }

    /**
     * Initializes a new instance of the {@link ParseExpectation} class.
     *
     * @param region the region
     * @param expectedResult the expected result
     * @param expectedFragment the expected fragment; or {@code null}
     * @param expectedResultLanguage the expected result language; or {@code null}
     * @param languageRegion the region of the language name; or {@code null}
     */
    public ParseExpectation(ISourceRegion region, Result expectedResult, @Nullable IFragment expectedFragment,
                            @Nullable String expectedResultLanguage, @Nullable ISourceRegion languageRegion) {
        super(region, expectedFragment, expectedResultLanguage, languageRegion);
        this.expectedResult = expectedResult;
    }

    /**
     * Gets the expected result.\
     *
     * @return a member of the {@link Result} enum.
     */
    public Result getExpectedResult() {
        return expectedResult;
    }

    /**
     * Specifies the expected parse result.
     */
    public enum Result {
        Succeeds,
        Ambiguous,
        Fails,
        ToFragment,
    }
}
