package org.metaborg.spt.core.expectations;

import java.util.List;

import org.metaborg.core.MetaborgException;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.tracing.Resolution;
import org.metaborg.spoofax.core.tracing.ISpoofaxResolverService;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spt.core.ITestCase;
import org.metaborg.spt.core.ITestExpectation;
import org.metaborg.spt.core.ITestExpectationInput;
import org.metaborg.spt.core.ITestExpectationOutput;
import org.metaborg.spt.core.TestExpectationOutput;
import org.metaborg.spt.core.TestPhase;
import org.metaborg.spt.core.util.SPTUtil;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Runs the ISpoofaxResolverService at the start offset of a selection to see if it resolved properly.
 * 
 * Note that we require the test's analysis context to still be valid and open, as it is reused to run the resolver
 * service.
 */
public class ResolveExpectation implements ITestExpectation {

    private static final String RESOLVE = "Resolve";
    private static final String TO = "ResolveTo";

    private final ISpoofaxTracingService traceService;
    private final ISpoofaxResolverService resolverService;

    @Inject public ResolveExpectation(ISpoofaxTracingService traceService, ISpoofaxResolverService resolverService) {
        this.traceService = traceService;
        this.resolverService = resolverService;
    }

    @Override public boolean canEvaluate(IStrategoTerm expectationTerm) {
        String cons = SPTUtil.consName(expectationTerm);
        return RESOLVE.equals(cons) || TO.equals(cons);
    }

    @Override public TestPhase getPhase(IStrategoTerm expectationTerm) {
        return TestPhase.ANALYSIS;
    }

    @Override public ITestExpectationOutput evaluate(ITestExpectationInput input) {
        final boolean success;
        List<IMessage> messages = Lists.newLinkedList();
        // indicates if, after collecting preliminary messages, we still need to try to resolve
        boolean tryResolve = true;

        ITestCase test = input.getTestCase();
        IStrategoTerm expectation = input.getExpectation();
        String cons = SPTUtil.consName(expectation);
        int num1 = -1;
        int num2 = -1;

        switch(cons) {
            case TO:
                // it's a ResolveTo(num1, num2), where num1 and num2 are ints referring to selections
                // note that these indexes start at 1, not at 0, hence the -1
                num2 = Term.asJavaInt(expectation.getSubterm(1)) - 1;
                // FALL THROUGH!
            case RESOLVE:
                // it's a Resolve(num1), or a fall-through of ResolveTo(num1, num2)
                num1 = Term.asJavaInt(expectation.getSubterm(0)) - 1;
                break;
            default:
                // TODO: is this allowed or should we fail more gracefully?
                throw new IllegalArgumentException("Can't handle expectation " + expectation);
        }


        List<ISourceRegion> selections = test.getFragment().getSelections();
        if(selections.isEmpty()) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Expected a selection to indicate what should be resolved.", null));
            tryResolve = false;
        } else {
            if(num1 >= selections.size()) {
                ISourceLocation loc = traceService.location(expectation.getSubterm(0));
                ISourceRegion region = loc == null ? test.getDescriptionRegion() : loc.region();
                messages.add(MessageFactory.newAnalysisError(test.getResource(), region,
                    "No such selection #" + (num1 + 1), null));
                tryResolve = false;
            }
            if(num2 >= selections.size()) {
                ISourceLocation loc = traceService.location(expectation.getSubterm(1));
                ISourceRegion region = loc == null ? test.getDescriptionRegion() : loc.region();
                messages.add(MessageFactory.newAnalysisError(test.getResource(), region,
                    "No such selection #" + (num2 + 1), null));
                tryResolve = false;
            }
        }

        ISpoofaxAnalyzeUnit analysisResult = input.getAnalysisResult();
        if(analysisResult == null) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Expected analysis to succeed", null));
            return new TestExpectationOutput(false, messages);
        }

        // If the referenced selections couldn't be found, we won't need to try to resolve
        // otherwise, we may assume the selections exist
        if(!tryResolve) {
            return new TestExpectationOutput(false, messages);
        }

        // Try resolving selection at index num1
        Resolution r = null;
        try {
            r = resolverService.resolve(selections.get(num1).startOffset(), analysisResult);
        } catch(MetaborgException e) {
            ISourceLocation loc = traceService.location(expectation);
            ISourceRegion region = loc == null ? test.getDescriptionRegion() : loc.region();
            messages.add(MessageFactory.newAnalysisError(test.getResource(), region,
                "Reference resolution caused an unexpected error.", e));
        }
        if(r == null || Iterables.isEmpty(r.targets)) {
            final String msg;
            if(r == null) {
                msg = "Reference resolution failed";
            } else {
                msg = "No targets returned by reference resolution.";
            }
            ISourceLocation loc = traceService.location(expectation);
            ISourceRegion region = loc == null ? test.getDescriptionRegion() : loc.region();
            messages.add(MessageFactory.newAnalysisError(test.getResource(), region, msg, null));
            success = false;
        } else {
            // Selection num1 resolved successfully
            // check if it resolved to the proper term
            if(TO.equals(cons)) {
                boolean found = false;
                ISourceRegion selection = selections.get(num2);
                for(ISourceLocation loc : r.targets) {
                    if(loc.region().startOffset() == selection.startOffset() // match start offset
                        && loc.region().endOffset() == selection.endOffset() // match end offset
                        && (loc.resource() == null && test.getResource() == null // match resource
                            || loc.resource() != null && loc.resource().equals(test.getResource()))) {
                        found = true;
                        break;
                    }
                }
                success = found;
                if(!found) {
                    ISourceRegion target = r.targets.iterator().next().region();
                    String targetStr = new StringBuilder().append("(").append(target.startOffset()).append(", ")
                        .append(target.endOffset()).append(")").toString();
                    String msg = new StringBuilder().append("Resolved to region ").append(targetStr)
                        .append(" instead of selection #").append(num2 + 1).append(" at region (")
                        .append(selection.startOffset()).append(", ").append(selection.endOffset()).append(")")
                        .toString();
                    messages.add(MessageFactory.newAnalysisError(test.getResource(), selections.get(num1), msg, null));
                }
            } else {
                // nothing to resolve to, so we passed
                success = true;
            }
        }

        if(!success) {
            MessageUtil.propagateMessages(analysisResult.messages(), messages, test.getDescriptionRegion());
        }
        return new TestExpectationOutput(success, messages);
    }

}
