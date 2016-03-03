package org.metaborg.spt.core;

import java.util.ArrayList;
import java.util.List;

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

    @Override public ITestCaseBuilder withTest(IStrategoTerm test, FileObject suiteFile) {
        this.resource = suiteFile;
        return withTest(test);
    }

    @Override public ITestCaseBuilder withTest(IStrategoTerm test) {
        // Expected a Test<n> node
        if(!Tools.isTermAppl(test)) {
            throw new IllegalArgumentException("Expected a Test constructor, but got " + test);
        }
        IStrategoAppl testAppl = (IStrategoAppl) test;
        String testConsName = testAppl.getConstructor().getName();
        final int markerLength;
        if("Test2".equals(testConsName)) {
            markerLength = 2;
        } else if("Test3".equals(testConsName)) {
            markerLength = 3;
        } else if("Test4".equals(testConsName)) {
            markerLength = 4;
        } else {
            throw new IllegalArgumentException("Expected a Test constructor, but got " + test);
        }
        // It's a Test<n>(desc, marker, fragment, marker, expectations)
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
                    String cons = ((IStrategoAppl) term).getConstructor().getName();
                    if(cons.equals("Selection" + markerLength)) {
                        selections.add(SPTUtil.getRegion(term));
                    }
                }
            }
        }.visit(fragment);

        return this;
    }

    @Override public ITestCase build() {
        if(description == null) {
            throw new IllegalStateException("No test AST added to the builder, so there's nothing to build.");
        }
        return new TestCase(description, fragment, resource, selections, expectations);
    }

}
