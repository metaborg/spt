package org.metaborg.spt.core;

import org.apache.commons.vfs2.FileObject;
import org.spoofax.interpreter.terms.IStrategoTerm;

public interface ITestCaseBuilder {

    public ITestCaseBuilder withTestFixture(IStrategoTerm testFixture);

    public ITestCaseBuilder withTest(IStrategoTerm test);

    public ITestCaseBuilder withTest(IStrategoTerm test, FileObject suiteFile);

    public ITestCase build();
}
