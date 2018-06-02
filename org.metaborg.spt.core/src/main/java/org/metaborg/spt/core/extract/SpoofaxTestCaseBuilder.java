package org.metaborg.spt.core.extract;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.TestCase;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;
import org.metaborg.mbt.core.model.expectations.NoExpectationError;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.SPTUtil;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.terms.StrategoString;
import org.spoofax.terms.TermFactory;
import com.google.inject.Inject;

public class SpoofaxTestCaseBuilder implements ISpoofaxTestCaseBuilder {

    private static final ILogger logger = LoggerUtils.logger(SpoofaxTestCaseBuilder.class);

    private FileObject resource = null;
    private IProject project = null;
    private String description = null;
    private ISourceRegion descriptionRegion = null;
    private List<IStrategoTerm> expectationTerms = null;

    private final Set<ISpoofaxTestExpectationProvider> expectationProviders;
    private final ISpoofaxFragmentBuilder fragmentBuilder;
    private final ISpoofaxTracingService trace;

    @Inject public SpoofaxTestCaseBuilder(Set<ISpoofaxTestExpectationProvider> expectationProviders,
        ISpoofaxFragmentBuilder fragmentBuilder, ISpoofaxTracingService trace) {
        this.expectationProviders = expectationProviders;
        this.fragmentBuilder = fragmentBuilder;
        this.trace = trace;
    }

    @Override public ISpoofaxTestCaseBuilder withTestFixture(IStrategoTerm testFixture) {
        fragmentBuilder.withFixture(testFixture);
        return this;
    }

    @Override public ISpoofaxTestCaseBuilder withResource(FileObject suiteFile) {
        this.resource = suiteFile;
        fragmentBuilder.withResource(suiteFile);
        return this;
    }

    @Override public ISpoofaxTestCaseBuilder withProject(IProject project) {
        this.project = project;
        fragmentBuilder.withProject(project);
        return this;
    }

    @Override public ISpoofaxTestCaseBuilder withTest(IStrategoTerm test) {
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
        expectationTerms = new ArrayList<>();
        for(IStrategoTerm expectation : Tools.listAt(test, 4).getAllSubterms()) {
            // if(trace.location(expectation) == null) {
            // logger.warn("No origin information on test expectation {}", expectation);
            // }
        	expectationTerms.add(unescapeExpectation(expectation));
        }

        // setup the fragment builder
        IStrategoTerm fragmentTerm = test.getSubterm(2);
        fragmentBuilder.withFragment(fragmentTerm);

        return this;
    }
    
    /**
     * Unescape string terms of expectations by replacing \" with ".
     */
    private IStrategoTerm unescapeExpectation(IStrategoTerm expectation) {
    	ITermFactory factory = new TermFactory();
    	
    	IStrategoTerm unescapedExpectation;
    	
    	switch(expectation.getTermType()) {
	        case IStrategoTerm.APPL:
	        	IStrategoAppl appl = (IStrategoAppl) expectation;
	        	IStrategoTerm[] kids;
	        	
	        	if ("String".equals(appl.getConstructor().getName()) ) {
	        		StrategoString escapedString = (StrategoString) appl.getSubterm(0);
	        		IStrategoString unescapedString = factory.makeString(escapedString.stringValue().replace("\\\"", "\""));
	        		
	        		kids = new IStrategoTerm[] {unescapedString};
	        	} else {
	        		kids = unescapeExpectationKids(expectation);
	        	}
		        
	        	unescapedExpectation = factory.makeAppl(appl.getConstructor(), kids, expectation.getAnnotations()); break;
	        case IStrategoTerm.LIST:
	        	unescapedExpectation = factory.makeList(unescapeExpectationKids(expectation), expectation.getAnnotations()); break;
	        case IStrategoTerm.TUPLE:
	        	unescapedExpectation = factory.makeTuple(unescapeExpectationKids(expectation), expectation.getAnnotations()); break;
	        default:
	            return expectation;
	    }
    	
    	ImploderAttachment attachment = expectation.getAttachment(ImploderAttachment.TYPE);
    	
    	unescapedExpectation.putAttachment(attachment);
    	
    	return unescapedExpectation;
    }
    
    private IStrategoTerm[] unescapeExpectationKids(IStrategoTerm expectation) {
    	IStrategoTerm[] kids = new IStrategoTerm[expectation.getSubtermCount()];
    	
    	for (int i = 0; i < expectation.getSubtermCount(); i++)
    		kids[i] = unescapeExpectation(expectation.getSubterm(i));
    	
    	return kids;
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

        // build the fragment
        IFragment fragment = fragmentBuilder.build();

        // lookup the ITestExpectationProviders that can handle our test expectations
        final List<ITestExpectation> expectations = new LinkedList<>();
        for(IStrategoTerm expectationTerm : expectationTerms) {
            boolean found = false;
            for(ISpoofaxTestExpectationProvider provider : expectationProviders) {
                if(provider.canEvaluate(fragment, expectationTerm)) {
                    final ITestExpectation expectation = provider.createExpectation(fragment, expectationTerm);
                    expectations.add(expectation);
                    found = true;
                    break;
                }
            }
            if(!found) {
                logger.warn("Unable to find a provider for {}", expectationTerm);
                // TODO: for now we add this specific expectation if we couldn't find a proper one.
                // We might want to have a way to make it less dirty.
                // The main reason for this is that the builder can't give back any messages, so we rely on someone
                // after us to check for these things and do the error message reporting.
                ISourceLocation loc = trace.location(expectationTerm);
                expectations.add(new NoExpectationError(loc == null ? descriptionRegion : loc.region()));
            }
        }


        return new TestCase(description, descriptionRegion, fragment, resource, project, expectations);
    }

}
