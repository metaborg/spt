package org.metaborg.spt.core.run;

import org.metaborg.mbt.core.run.ITestResult;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

/**
 * Type interface for the result of running a test on Spoofax languages.
 */
public interface ISpoofaxTestResult extends ITestResult<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> {

    @Override Iterable<ISpoofaxTestExpectationOutput> getExpectationResults();

    @Override ISpoofaxFragmentResult getFragmentResult();
}
