package org.metaborg.spt.core;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.source.ISourceRegion;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.TermVisitor;

public class TestCaseBuilder implements ITestCaseBuilder {

    private FileObject resource = null;
    private String description = null;
    private IStrategoTerm fragment = null;
    private List<ISourceRegion> selections = null;
    private List<IStrategoTerm> expectations = null;

    @Override public ITestCaseBuilder withTestFixture(IStrategoTerm testFixture) {
        // TODO: support test fixtures
        throw new UnsupportedOperationException("Test fixtures are not supported yet.");
    }

    @Override public ITestCaseBuilder withTest(IStrategoTerm test, @Nullable FileObject suiteFile) {
        this.resource = suiteFile;

        // Expected a Test<n> node
        if(!Tools.isTermAppl(test)) {
            throw new IllegalArgumentException("Expected a Test constructor, but got " + test);
        }
        IStrategoAppl testAppl = (IStrategoAppl) test;
        String testConsName = testAppl.getConstructor().getName();
        if(!SPTUtil.TEST_CONS.equals(testConsName)) {
            throw new IllegalArgumentException("Expected a Test constructor, but got " + test);
        }
        // It's a Test(desc, marker, fragment, marker, expectations)
        description = Tools.asJavaString(Tools.stringAt(test, 0));
        fragment = test.getSubterm(2);
        expectations = new ArrayList<>();
        for(IStrategoTerm expectation : Tools.listAt(test, 4).getAllSubterms()) {
            expectations.add(expectation);
        }
        // Get source regions of the Selection<n> nodes
        selections = new ArrayList<>();
        new TermVisitor() {

            @Override public void preVisit(IStrategoTerm term) {
                if(Tools.isTermAppl(term)) {
                    if(SPTUtil.SELECTION_CONS.equals(SPTUtil.consName(term))) {
                        selections.add(SPTUtil.getRegion(term));
                    }
                }
            }
        }.visit(fragment);

        return this;
    }

    @Override public ITestCaseBuilder withTest(IStrategoTerm test) {
        withTest(test, null);

        return this;
    }

    @Override public ITestCase build() {
        if(description == null) {
            throw new IllegalStateException("No test AST added to the builder, so there's nothing to build.");
        }
        return new TestCase(description, fragment, resource, selections, expectations);
    }

}
