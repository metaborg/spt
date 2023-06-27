package org.metaborg.spt.core.extract.expectations;

import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.SPTUtil;
import org.metaborg.spt.core.expectations.ParseToAtermExpectation;
import org.metaborg.spt.core.extract.ISpoofaxTestExpectationProvider;
import org.spoofax.interpreter.terms.IStrategoTerm;

import javax.inject.Inject;

/**
 * Deals with 'parse to [ATerm]' expectations, which are specific to Spoofax.
 */
public class ParseToAtermExpectationProvider implements ISpoofaxTestExpectationProvider {

    // ParseToAterm(ToAterm(ast))
    private static final String PARSE = "ParseToAterm";
    private static final String ATERM = "ToAterm";

    private final ISpoofaxTracingService traceService;

    @Inject public ParseToAtermExpectationProvider(ISpoofaxTracingService traceService) {
        this.traceService = traceService;
    }

    @Override public boolean canEvaluate(IFragment inputFragment, IStrategoTerm expectationTerm) {
        String cons = SPTUtil.consName(expectationTerm);
        boolean success = cons != null && PARSE.equals(cons) && expectationTerm.getSubtermCount() == 1;
        if(success) {
            IStrategoTerm aterm = expectationTerm.getSubterm(0);
            cons = SPTUtil.consName(aterm);
            success = cons != null && ATERM.equals(cons) && aterm.getSubtermCount() == 1;
        }
        return success;
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        ISourceLocation loc = traceService.location(expectationTerm);
        ISourceRegion region = loc == null ? inputFragment.getRegion() : loc.region();
        return new ParseToAtermExpectation(region, expectationTerm.getSubterm(0).getSubterm(0));
    }

}
