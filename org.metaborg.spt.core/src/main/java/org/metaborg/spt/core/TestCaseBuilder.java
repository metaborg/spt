package org.metaborg.spt.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.spt.core.ITestCase.ExpectationPair;
import org.metaborg.spt.core.util.SPTUtil;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Inject;

public class TestCaseBuilder implements ITestCaseBuilder {

    private FileObject resource = null;
    private String description = null;
    private List<IStrategoTerm> expectations = null;

    private final Set<ITestExpectation> expectationEvaluators;
    private final IFragmentBuilder fragmentBuilder;

    @Inject public TestCaseBuilder(Set<ITestExpectation> expectationEvaluators, IFragmentBuilder fragmentBuilder) {
        this.expectationEvaluators = expectationEvaluators;
        this.fragmentBuilder = fragmentBuilder;
    }

    @Override public ITestCaseBuilder withTestFixture(IStrategoTerm testFixture) {
        fragmentBuilder.withFixture(testFixture);
        // TODO: support test fixtures
        throw new UnsupportedOperationException("Test fixtures are not supported yet.");
    }

    @Override public ITestCaseBuilder withTest(IStrategoTerm test, @Nullable FileObject suiteFile) {
        // Expected a Test<n> node
        if(!Tools.isTermAppl(test) || !SPTUtil.TEST_CONS.equals(SPTUtil.consName(test))) {
            throw new IllegalArgumentException("Expected a Test constructor, but got " + test);
        }

        this.resource = suiteFile;

        // It's a Test(desc, marker, fragment, marker, expectations)
        // record the test's description
        description = Tools.asJavaString(Tools.stringAt(test, 0));

        // collect the AST nodes for the test expectations
        expectations = new ArrayList<>();
        for(IStrategoTerm expectation : Tools.listAt(test, 4).getAllSubterms()) {
            expectations.add(expectation);
        }

        // setup the fragment builder
        IStrategoTerm fragmentTerm = test.getSubterm(2);
        fragmentBuilder.withFragment(fragmentTerm);
        if(suiteFile != null) {
            fragmentBuilder.withResource(suiteFile);
        }

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

        // lookup the ITestExpectations that can handle our test expectations
        final List<ExpectationPair> expectationPairs = new LinkedList<>();
        for(IStrategoTerm expectationTerm : expectations) {
            boolean found = false;
            for(ITestExpectation evaluator : expectationEvaluators) {
                if(evaluator.canEvaluate(expectationTerm)) {
                    expectationPairs.add(new ExpectationPair(evaluator, expectationTerm));
                    found = true;
                    break;
                }
            }
            if(!found) {
                expectationPairs.add(new ExpectationPair(null, expectationTerm));
            }
        }

        // build the fragment
        IFragment fragment = fragmentBuilder.build();

        return new TestCase(description, fragment, resource, expectationPairs);
    }

}
