package org.metaborg.spt.core.run;

import org.metaborg.mbt.core.run.ITestExpectationInput;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

/**
 * Type interface for the input to a test's expectations, produced while running the test on Spoofax languages.
 */
public interface ISpoofaxTestExpectationInput extends ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> {

}
