package org.metaborg.spt.core.run;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.run.ITestCaseRunner;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

/**
 * Type interface for an ITestCaseRunner that runs tests on Spoofax languages.
 */
public interface ISpoofaxTestCaseRunner extends ITestCaseRunner<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> {

    @Override ISpoofaxTestResult run(IProject project, ITestCase test, ILanguageImpl languageUnderTest,
        ILanguageImpl dialectUnderTest);
}
