package org.metaborg.spt.core.spoofax;

import org.metaborg.core.project.IProject;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.ITestCaseExtractor;

public interface ISpoofaxTestCaseExtractor
    extends ITestCaseExtractor<ISpoofaxInputUnit, ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> {

    @Override ISpoofaxTestCaseExtractionResult extract(ISpoofaxInputUnit input, IProject project);

    @Override ISpoofaxTestCaseExtractionResult extract(ISpoofaxParseUnit input, IProject project);
}
