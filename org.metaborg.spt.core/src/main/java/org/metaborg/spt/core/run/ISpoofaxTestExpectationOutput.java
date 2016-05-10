package org.metaborg.spt.core.run;

import org.metaborg.mbt.core.run.ITestExpectationOutput;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

/**
 * Type interface for the output of evaluating a test expectation when running the test on Spoofax languages.
 */
public interface ISpoofaxTestExpectationOutput extends ITestExpectationOutput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> {

    @Override Iterable<ISpoofaxFragmentResult> getFragmentResults();
}
