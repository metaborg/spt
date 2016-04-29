package org.metaborg.spt.core.spoofax;

import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.ITestExpectationOutput;

public interface ISpoofaxTestExpectationOutput extends ITestExpectationOutput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> {

    @Override Iterable<ISpoofaxFragmentResult> getFragmentResults();
}
