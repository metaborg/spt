package org.metaborg.spt.core.spoofax.expectations;

import java.util.Collection;
import java.util.List;

import org.metaborg.core.MetaborgException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.tracing.Resolution;
import org.metaborg.spoofax.core.tracing.ISpoofaxResolverService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.IFragment;
import org.metaborg.spt.core.ITestCase;
import org.metaborg.spt.core.ITestExpectationInput;
import org.metaborg.spt.core.ITestExpectationOutput;
import org.metaborg.spt.core.TestExpectationOutput;
import org.metaborg.spt.core.TestPhase;
import org.metaborg.spt.core.expectations.MessageUtil;
import org.metaborg.spt.core.expectations.ResolveExpectation;
import org.metaborg.spt.core.spoofax.ISpoofaxExpectationEvaluator;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class ResolveExpectationEvaluator implements ISpoofaxExpectationEvaluator<ResolveExpectation> {

    private final ISpoofaxResolverService resolverService;

    @Inject public ResolveExpectationEvaluator(ISpoofaxResolverService resolverService) {
        this.resolverService = resolverService;
    }

    @Override public Collection<Integer> usesSelections(IFragment fragment, ResolveExpectation expectation) {
        List<Integer> used = Lists.newLinkedList();
        used.add(expectation.from());
        if(expectation.to() != -1) {
            used.add(expectation.to());
        }
        return used;
    }

    @Override public TestPhase getPhase(IContext languageUnderTestCtx, ResolveExpectation expectation) {
        return TestPhase.ANALYSIS;
    }

    @Override public ITestExpectationOutput
        evaluate(ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input, ResolveExpectation expectation) {

        final boolean success;
        List<IMessage> messages = Lists.newLinkedList();
        // indicates if, after collecting preliminary messages, we still need to try to resolve
        boolean tryResolve = true;

        ITestCase test = input.getTestCase();
        // note that these indexes start at 1, not at 0, hence the -1
        int num1 = expectation.from() - 1;
        int num2 = expectation.to() - 1;

        List<ISourceRegion> selections = test.getFragment().getSelections();
        if(selections.isEmpty()) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Expected a selection to indicate what should be resolved.", null));
            tryResolve = false;
        } else {
            if(num1 >= selections.size()) {
                messages.add(MessageFactory.newAnalysisError(test.getResource(), expectation.fromRegion(),
                    "No such selection #" + (num1 + 1), null));
                messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                    String.format("Unable to resolve selection #%1$s.", num1 + 1), null));
                tryResolve = false;
            }
            if(num2 >= selections.size()) {
                messages.add(MessageFactory.newAnalysisError(test.getResource(), expectation.toRegion(),
                    "No such selection #" + (num2 + 1), null));
                messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                    String.format("Unable to resolve selection #%1$s.", num2 + 1), null));
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
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Reference resolution caused an unexpected error.", e));
        }
        if(r == null || Iterables.isEmpty(r.targets)) {
            final String msg;
            if(r == null) {
                msg = "Reference resolution failed";
            } else {
                msg = "No targets returned by reference resolution.";
            }
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(), msg, null));
            success = false;
        } else {
            // Selection num1 resolved successfully
            if(expectation.to() == -1) {
                // nothing to resolve to, so we passed
                success = true;
            } else {
                // check if it resolved to the proper term
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
                    messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                        "Resolved to the wrong location.", null));
                }
            }
        }

        if(!success) {
            MessageUtil.propagateMessages(analysisResult.messages(), messages, test.getDescriptionRegion(),
                test.getFragment().getRegion());
        }
        return new TestExpectationOutput(success, messages);
    }

}
