package org.metaborg.spt.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.ITestCase.ExpectationPair;
import org.metaborg.spt.core.util.SPTUtil;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Inject;

public class TestCaseBuilder implements ITestCaseBuilder {

    private static final ILogger logger = LoggerUtils.logger(TestCaseBuilder.class);

    private FileObject resource = null;
    private IProject project = null;
    private String description = null;
    private ISourceRegion descriptionRegion = null;
    private List<IStrategoTerm> expectations = null;

    private final Set<ITestExpectation> expectationEvaluators;
    private final IFragmentBuilder fragmentBuilder;
    private final ISpoofaxTracingService trace;

    @Inject public TestCaseBuilder(Set<ITestExpectation> expectationEvaluators, IFragmentBuilder fragmentBuilder,
        ISpoofaxTracingService trace) {
        this.expectationEvaluators = expectationEvaluators;
        this.fragmentBuilder = fragmentBuilder;
        this.trace = trace;
    }

    @Override public ITestCaseBuilder withTestFixture(IStrategoTerm testFixture) {
        fragmentBuilder.withFixture(testFixture);
        // TODO: support test fixtures
        throw new UnsupportedOperationException("Test fixtures are not supported yet.");
    }

    @Override public ITestCaseBuilder withResource(FileObject suiteFile) {
        this.resource = suiteFile;
        fragmentBuilder.withResource(suiteFile);
        return this;
    }

    @Override public ITestCaseBuilder withProject(IProject project) {
        this.project = project;
        fragmentBuilder.withProject(project);
        return this;
    }

    @Override public ITestCaseBuilder withTest(IStrategoTerm test) {
        // Expected a Test<n> node
        if(!Tools.isTermAppl(test) || !SPTUtil.TEST_CONS.equals(SPTUtil.consName(test))) {
            throw new IllegalArgumentException("Expected a Test constructor, but got " + test);
        }

        // It's a Test(desc, marker, fragment, marker, expectations)
        // record the test's description
        IStrategoTerm descriptionTerm = Tools.stringAt(test, 0);
        description = Tools.asJavaString(descriptionTerm);
        ISourceLocation descriptionLocation = trace.location(descriptionTerm);
        if(descriptionLocation == null) {
            throw new IllegalArgumentException(
                "The test's description has no source location information attached to it.");
        }
        descriptionRegion = descriptionLocation.region();

        // collect the AST nodes for the test expectations
        expectations = new ArrayList<>();
        for(IStrategoTerm expectation : Tools.listAt(test, 4).getAllSubterms()) {
            if(trace.location(expectation) == null) {
                logger.warn("No origin information on test expectation {}", expectation);
            }
            expectations.add(expectation);
        }

        // setup the fragment builder
        IStrategoTerm fragmentTerm = test.getSubterm(2);
        fragmentBuilder.withFragment(fragmentTerm);

        return this;
    }

    @Override public ITestCase build() {
        if(description == null) {
            throw new IllegalStateException("No test AST added to the builder, so there's nothing to build.");
        }
        if(resource == null) {
            throw new IllegalStateException("No resource added to the builder. We can't build without one.");
        }
        if(project == null) {
            throw new IllegalStateException("No project added to the builder. We can't build without one.");
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

        return new TestCase(description, descriptionRegion, fragment, resource, project, expectationPairs);
    }

}
