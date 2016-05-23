package org.metaborg.spt.core.extract;

import org.metaborg.core.project.IProject;
import org.metaborg.mbt.core.extract.ITestCaseExtractor;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

/**
 * Type interface for an ITestCaseExtractor that extracts tests from a Spoofax SPT test suite specification.
 */
public interface ISpoofaxTestCaseExtractor
    extends ITestCaseExtractor<ISpoofaxInputUnit, ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> {

    @Override ISpoofaxTestCaseExtractionResult extract(ISpoofaxInputUnit input, IProject project);

    @Override ISpoofaxTestCaseExtractionResult extract(ISpoofaxParseUnit input, IProject project);
}
