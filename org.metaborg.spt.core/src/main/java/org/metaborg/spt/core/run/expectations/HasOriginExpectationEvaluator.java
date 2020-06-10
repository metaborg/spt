package org.metaborg.spt.core.run.expectations;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.TestPhase;
import org.metaborg.mbt.core.model.expectations.HasOriginExpectation;
import org.metaborg.mbt.core.run.ITestExpectationInput;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.SPTUtil;
import org.metaborg.spt.core.run.ISpoofaxExpectationEvaluator;
import org.metaborg.spt.core.run.ISpoofaxFragmentResult;
import org.metaborg.spt.core.run.ISpoofaxTestExpectationOutput;
import org.metaborg.spt.core.run.SpoofaxTestExpectationOutput;
import org.metaborg.util.iterators.Iterables2;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.util.TermUtils;
import org.spoofax.terms.visitor.AStrategoTermVisitor;
import org.spoofax.terms.visitor.StrategoTermVisitee;

import java.util.Collection;
import java.util.List;

/**
 * Check all terms (except lists) in the analyzed AST to see if there are any terms without a location.
 * 
 * If so, the expectation fails.
 */
public class HasOriginExpectationEvaluator implements ISpoofaxExpectationEvaluator<HasOriginExpectation> {

    private final ISpoofaxTracingService traceService;

    @Inject public HasOriginExpectationEvaluator(ISpoofaxTracingService traceService) {
        this.traceService = traceService;
    }

    @Override public Collection<Integer> usesSelections(IFragment fragment, HasOriginExpectation expectation) {
        return Lists.newLinkedList();
    }

    @Override public TestPhase getPhase(ILanguageImpl language, HasOriginExpectation expectation) {
        return TestPhase.ANALYSIS;
    }

    @Override public ISpoofaxTestExpectationOutput evaluate(
        ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input, HasOriginExpectation expectation) {

        final ITestCase test = input.getTestCase();
        final List<IMessage> messages = Lists.newLinkedList();
        final Iterable<ISpoofaxFragmentResult> fragmentResults = Iterables2.empty();

        ISpoofaxAnalyzeUnit a = input.getFragmentResult().getAnalysisResult();
        if(a == null || !a.valid() || !a.hasAst()) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "An analyzed AST is required to check origin locations.", null));
            return new SpoofaxTestExpectationOutput(false, messages, fragmentResults);
        }

        // check bottomup to see if there is a term without origin
        StrategoTermVisitee.bottomup(new AStrategoTermVisitor() {
            @Override public boolean visit(IStrategoTerm term) {
                // skip lists as they never seem to have origin locations
                if(TermUtils.isList(term)) {
                    return false;
                }
                ISourceLocation loc = traceService.location(term);
                if(loc == null) {
                    messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                        String.format("Term %1$s has no origin.", SPTUtil.noAnnosString(term)), null));
                }
                // we will visit bottomup, so no need to check children.
                return false;
            }
        }, a.ast());

        return new SpoofaxTestExpectationOutput(messages.isEmpty(), messages, fragmentResults);
    }

}
