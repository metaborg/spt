package org.metaborg.spt.core.extract;

import org.metaborg.mbt.core.extract.ITestCaseExtractionResult;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

/**
 * Type interface for an ITestCaseExtractionResult from extracting using a Spoofax SPT language.
 */
public interface ISpoofaxTestCaseExtractionResult
    extends ITestCaseExtractionResult<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> {

}
