package org.metaborg.spt.core.spoofax.expectations;

import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.IFragment;
import org.metaborg.spt.core.ITestExpectation;
import org.metaborg.spt.core.expectations.ParseExpectation;
import org.metaborg.spt.core.spoofax.ISpoofaxFragmentBuilder;
import org.metaborg.spt.core.spoofax.ISpoofaxTestExpectationProvider;
import org.metaborg.spt.core.util.SPTUtil;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Inject;

/**
 * Implementation for the evaluation of the 'parse succeeds' and 'parse fails' test expectations.
 */
public class ParseExpectationProvider implements ISpoofaxTestExpectationProvider {

    private static final ILogger logger = LoggerUtils.logger(ParseExpectationProvider.class);

    private static final String SUC = "ParseSucceeds";
    private static final String FAIL = "ParseFails";
    private static final String TO = "ParseTo";

    private final ISpoofaxTracingService traceService;
    private final ISpoofaxFragmentBuilder fragmentBuilder;

    private final FragmentUtil fragmentUtil;

    @Inject public ParseExpectationProvider(ISpoofaxTracingService traceService,
        ISpoofaxFragmentBuilder fragmentBuilder, FragmentUtil fragmentUtil) {
        this.traceService = traceService;
        this.fragmentBuilder = fragmentBuilder;

        this.fragmentUtil = fragmentUtil;
    }

    @Override public boolean canEvaluate(IFragment inputFragment, IStrategoTerm expectationTerm) {
        String cons = SPTUtil.consName(expectationTerm);
        return cons != null && (SUC.equals(cons) || FAIL.equals(cons) || TO.equals(cons));
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        logger.debug("Creating a ParseExpectation for {}", expectationTerm);
        ISourceLocation loc = traceService.location(expectationTerm);
        ISourceRegion region = loc == null ? inputFragment.getRegion() : loc.region();
        final String cons = SPTUtil.consName(expectationTerm);
        if(TO.equals(cons)) {
            final IStrategoTerm toPart = expectationTerm.getSubterm(0);
            final String lang = FragmentUtil.toPartLangName(toPart);
            final ISourceRegion langRegion = fragmentUtil.toPartLangNameRegion(toPart);
            final IFragment fragment = fragmentBuilder.withFragment(FragmentUtil.toPartFragment(toPart))
                .withProject(inputFragment.getProject()).withResource(inputFragment.getResource()).build();
            return new ParseExpectation(region, true, fragment, lang, langRegion);
        } else {
            return new ParseExpectation(region, !FAIL.equals(cons));
        }
    }

}
