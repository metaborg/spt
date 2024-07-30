package org.metaborg.spt.core.extract.expectations;

import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.expectations.ITestExpectation;
import org.metaborg.mbt.core.model.expectations.ParseExpectation;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spt.core.SPTUtil;
import org.metaborg.spt.core.extract.ISpoofaxFragmentBuilder;
import org.metaborg.spt.core.extract.ISpoofaxTestExpectationProvider;
import org.metaborg.spt.core.run.FragmentUtil;
import org.spoofax.interpreter.terms.IStrategoTerm;


/**
 * Implementation for the evaluation of the 'parse succeeds' and 'parse fails' test expectations.
 */
public class ParseExpectationProvider implements ISpoofaxTestExpectationProvider {

    // private static final ILogger logger = LoggerUtils.logger(ParseExpectationProvider.class);

    private static final String SUC = "ParseSucceeds";
    private static final String AMB = "ParseAmbiguous";
    private static final String FAIL = "ParseFails";
    private static final String TO = "ParseTo";

    private final ISpoofaxTracingService traceService;
    private final ISpoofaxFragmentBuilder fragmentBuilder;

    private final FragmentUtil fragmentUtil;

    @jakarta.inject.Inject public ParseExpectationProvider(ISpoofaxTracingService traceService,
        ISpoofaxFragmentBuilder fragmentBuilder, FragmentUtil fragmentUtil) {
        this.traceService = traceService;
        this.fragmentBuilder = fragmentBuilder;

        this.fragmentUtil = fragmentUtil;
    }

    @Override public boolean canEvaluate(IFragment inputFragment, IStrategoTerm expectationTerm) {
        String cons = SPTUtil.consName(expectationTerm);
        return cons != null && (SUC.equals(cons) || AMB.equals(cons) || FAIL.equals(cons) || TO.equals(cons));
    }

    @Override public ITestExpectation createExpectation(IFragment inputFragment, IStrategoTerm expectationTerm) {
        // logger.debug("Creating a ParseExpectation for {}", expectationTerm);
        ISourceLocation loc = traceService.location(expectationTerm);
        ISourceRegion region = loc == null ? inputFragment.getRegion() : loc.region();
        final String cons = SPTUtil.consName(expectationTerm);
        switch (cons) {
            case TO:
                final IStrategoTerm toPart = expectationTerm.getSubterm(0);
                final String lang = FragmentUtil.toPartLangName(toPart);
                final ISourceRegion langRegion = fragmentUtil.toPartLangNameRegion(toPart);
                final IFragment fragment = fragmentBuilder.withFragment(FragmentUtil.toPartFragment(toPart))
                        .withProject(inputFragment.getProject()).withResource(inputFragment.getResource()).build();
                return new ParseExpectation(region, ParseExpectation.Result.ToFragment, fragment, lang, langRegion);
            case SUC:
                return new ParseExpectation(region, ParseExpectation.Result.Succeeds);
            case FAIL:
                return new ParseExpectation(region, ParseExpectation.Result.Fails);
            case AMB:
                return new ParseExpectation(region, ParseExpectation.Result.Ambiguous);
            default:
                throw new UnsupportedOperationException("Expectation term " + cons + " is not supported.");
        }
    }

}
