package org.metaborg.spt.core;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;

public interface ITestRunner {

    public ITestResult run(IProject project, ITestCase test, ILanguageImpl languageUnderTest, ILanguageImpl spt);

}
