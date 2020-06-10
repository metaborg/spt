package org.metaborg.spt.core.extract;

import javax.annotation.Nullable;

import org.metaborg.mbt.core.extract.ITestCaseExtractionResult;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

/**
 * Interface for an ITestCaseExtractionResult from extracting using a Spoofax SPT language.
 * 
 * Also records the start symbol of the specification.
 */
public interface ISpoofaxTestCaseExtractionResult
    extends ITestCaseExtractionResult<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> {

    /**
     * The start symbol of the test suite specification from which you extracted.
     */
    @Nullable String getStartSymbol();
}
