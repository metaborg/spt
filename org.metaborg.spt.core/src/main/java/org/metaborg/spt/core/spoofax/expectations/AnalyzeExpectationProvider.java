package org.metaborg.spt.core.spoofax.expectations;

import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.IFragment;
import org.metaborg.spt.core.ITestExpectation;
import org.metaborg.spt.core.expectations.AnalysisMessageExpectation;
import org.metaborg.spt.core.spoofax.ISpoofaxTestExpectationProvider;
import org.metaborg.spt.core.util.SPTUtil;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;

import com.google.inject.Inject;


public class AnalyzeExpectationProvider implements ISpoofaxTestExpectationProvider {

    private static final String ERR = "Errors";
    private static final String WARN = "Warnings";
    private static final String NOTE = "Notes";

    private final ISpoofaxTracingService traceService;

    @Inject public AnalyzeExpectationProvider(ISpoofaxTracingService traceService) {
        this.traceService = traceService;
    }

    @Override public boolean canEvaluate(IFragment inputFragment, IStrategoTerm expectationTerm) {
        String cons = SPTUtil.consName(expectationTerm);
        return (ERR.equals(cons) || WARN.equals(cons) || NOTE.equals(cons)) && expectationTerm.getSubtermCount() == 1
            && Term.isTermInt(expectationTerm.getSubterm(0));
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        final MessageSeverity severity;
        switch(SPTUtil.consName(expectationTerm)) {
            case ERR:
                // it's an Errors(n) term
                severity = MessageSeverity.ERROR;
                break;
            case WARN:
                // it's a Warnings(n) term
                severity = MessageSeverity.WARNING;
                break;
            case NOTE:
                // it's a Notes(n) term
                severity = MessageSeverity.NOTE;
                break;
            default:
                throw new IllegalArgumentException("This test expectation provider can't evaluate " + expectationTerm);
        }
        ISourceLocation loc = traceService.location(expectationTerm);
        ISourceRegion region = loc == null ? inputFragment.getRegion() : loc.region();
        return new AnalysisMessageExpectation(region, Term.asJavaInt(expectationTerm.getSubterm(0)), severity);
    }

}
