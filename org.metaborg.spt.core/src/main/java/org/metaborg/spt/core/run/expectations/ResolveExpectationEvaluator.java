package org.metaborg.spt.core.run.expectations;

import java.util.Collection;
import java.util.List;

import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.tracing.Resolution;
import org.metaborg.core.tracing.ResolutionTarget;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.TestPhase;
import org.metaborg.mbt.core.model.expectations.MessageUtil;
import org.metaborg.mbt.core.model.expectations.ResolveExpectation;
import org.metaborg.mbt.core.run.ITestExpectationInput;
import org.metaborg.spoofax.core.tracing.ISpoofaxResolverService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.run.ISpoofaxExpectationEvaluator;
import org.metaborg.spt.core.run.ISpoofaxFragmentResult;
import org.metaborg.spt.core.run.ISpoofaxTestExpectationOutput;
import org.metaborg.spt.core.run.SpoofaxTestExpectationOutput;
import org.metaborg.util.iterators.Iterables2;

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

    @Override public TestPhase getPhase(ILanguageImpl language, ResolveExpectation expectation) {
        return TestPhase.ANALYSIS;
    }

    @Override public ISpoofaxTestExpectationOutput
        evaluate(ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input, ResolveExpectation expectation) {

        final boolean success;
        List<IMessage> messages = Lists.newLinkedList();
        // resolving expectations don't have output fragments
        Iterable<ISpoofaxFragmentResult> fragmentResults = Iterables2.empty();
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

        ISpoofaxAnalyzeUnit analysisResult = input.getFragmentResult().getAnalysisResult();
        if(analysisResult == null) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Expected analysis to succeed", null));
            return new SpoofaxTestExpectationOutput(false, messages, fragmentResults);
        }

        // If the referenced selections couldn't be found, we won't need to try to resolve
        // otherwise, we may assume the selections exist
        if(!tryResolve) {
            return new SpoofaxTestExpectationOutput(false, messages, fragmentResults);
        }

        // Try resolving selection at index num1
        Resolution r = null;
        try {
            final ISourceRegion sel = selections.get(num1);
            int midOffset = sel.startOffset() + ((sel.endOffset() - sel.startOffset()) / 2);
            r = resolverService.resolve(midOffset, analysisResult);
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
                for(ResolutionTarget target : r.targets) {
                    ISourceLocation loc = target.location;
                    if(loc.region().startOffset() == selection.startOffset() // match start offset
                        && loc.region().endOffset() == selection.endOffset() // match end offset
                        && loc.resource() != null // match resource
                        && loc.resource().compareTo(test.getResource()) == 0) {
                        found = true;
                        break;
                    }
                }
                success = found;
                if(!found) {
                    ISourceLocation target = r.targets.iterator().next().location;
                    ISourceRegion targetRegion = target.region();
                    final String msg;
                    if(targetRegion.startOffset() != selection.startOffset()
                        || targetRegion.endOffset() != selection.endOffset()) {
                        msg = new StringBuilder().append("Resolved to region (").append(targetRegion.startOffset())
                            .append(", ").append(targetRegion.endOffset()).append(") instead of selection #")
                            .append(num2 + 1).append(" at region (").append(selection.startOffset()).append(", ")
                            .append(selection.endOffset()).append(")").toString();
                    } else if(target.resource() == null) {
                        msg = "Resolved to a location in an unknown file.";
                    } else {
                        msg = "Resolved to a region in a different file: " + target.toString();
                    }
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
        return new SpoofaxTestExpectationOutput(success, messages, fragmentResults);
    }

}
