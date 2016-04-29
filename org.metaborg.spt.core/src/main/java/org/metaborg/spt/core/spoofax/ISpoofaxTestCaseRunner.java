package org.metaborg.spt.core.spoofax;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.ITestCase;
import org.metaborg.spt.core.ITestCaseRunner;

public interface ISpoofaxTestCaseRunner extends ITestCaseRunner<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> {

    @Override ISpoofaxTestResult run(IProject project, ITestCase test, ILanguageImpl languageUnderTest,
        ILanguageImpl dialectUnderTest);
}
