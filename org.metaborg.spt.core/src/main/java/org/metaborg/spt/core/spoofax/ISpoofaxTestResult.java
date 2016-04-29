package org.metaborg.spt.core.spoofax;

import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.ITestResult;

public interface ISpoofaxTestResult extends ITestResult<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> {

    @Override Iterable<ISpoofaxTestExpectationOutput> getExpectationResults();

    @Override ISpoofaxFragmentResult getFragmentResult();
}
