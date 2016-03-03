package org.metaborg.spt.core;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;

public interface ITestCaseExtractor {

    public Iterable<ITestCase> extract(ILanguageImpl spt, IProject project, final FileObject testSuite);
}
