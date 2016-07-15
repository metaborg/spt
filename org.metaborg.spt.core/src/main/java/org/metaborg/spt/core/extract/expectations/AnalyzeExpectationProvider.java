package org.metaborg.spt.core.extract.expectations;

import java.util.List;

import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.expectations.AnalysisMessageExpectation;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.SPTUtil;
import org.metaborg.spt.core.extract.ISpoofaxTestExpectationProvider;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;

import com.google.common.collect.Lists;
import com.google.inject.Inject;


public class AnalyzeExpectationProvider implements ISpoofaxTestExpectationProvider {

    private static final String ERR = "Errors";
    private static final String ERR_AT = "ErrorsAt";
    private static final String WARN = "Warnings";
    private static final String WARN_AT = "WarningsAt";
    private static final String NOTE = "Notes";
    private static final String NOTE_AT = "NotesAt";

    private final ISpoofaxTracingService traceService;

    @Inject public AnalyzeExpectationProvider(ISpoofaxTracingService traceService) {
        this.traceService = traceService;
    }

    @Override public boolean canEvaluate(IFragment inputFragment, IStrategoTerm expectationTerm) {
        String cons = SPTUtil.consName(expectationTerm);
        switch(cons) {
            case ERR_AT:
            case WARN_AT:
            case NOTE_AT:
                return expectationTerm.getSubtermCount() == 2 && Term.isTermInt(expectationTerm.getSubterm(0))
                    && Term.isTermList(expectationTerm.getSubterm(1));
            case ERR:
            case WARN:
            case NOTE:
                return expectationTerm.getSubtermCount() == 1 && Term.isTermInt(expectationTerm.getSubterm(0));
            default:
                return false;
        }
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        final MessageSeverity severity;
        final String cons = SPTUtil.consName(expectationTerm);
        switch(cons) {
            case ERR:
            case ERR_AT:
                // it's an Errors(n) term
                severity = MessageSeverity.ERROR;
                break;
            case WARN:
            case WARN_AT:
                // it's a Warnings(n) term
                severity = MessageSeverity.WARNING;
                break;
            case NOTE:
            case NOTE_AT:
                // it's a Notes(n) term
                severity = MessageSeverity.NOTE;
                break;
            default:
                throw new IllegalArgumentException("This test expectation provider can't evaluate " + expectationTerm);
        }
        final List<Integer> selections = Lists.newArrayList();
        switch(cons) {
            case ERR_AT:
            case WARN_AT:
            case NOTE_AT:
                IStrategoList list = (IStrategoList) expectationTerm.getSubterm(1);
                for(IStrategoTerm sel : list) {
                    selections.add(Term.asJavaInt(sel));
                }
                break;
            default:
        }
        ISourceLocation loc = traceService.location(expectationTerm);
        ISourceRegion region = loc == null ? inputFragment.getRegion() : loc.region();
        return new AnalysisMessageExpectation(region, Term.asJavaInt(expectationTerm.getSubterm(0)), severity,
            selections);
    }

}
