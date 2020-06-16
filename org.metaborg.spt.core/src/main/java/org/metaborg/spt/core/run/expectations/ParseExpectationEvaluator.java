package org.metaborg.spt.core.run.expectations;

import java.util.Collection;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.TestPhase;
import org.metaborg.mbt.core.model.expectations.ParseExpectation;
import org.metaborg.mbt.core.run.ITestExpectationInput;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.run.*;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.TermEqualityUtil;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class ParseExpectationEvaluator implements ISpoofaxExpectationEvaluator<ParseExpectation> {

    private static final ILogger logger = LoggerUtils.logger(ParseExpectationEvaluator.class);

    private final FragmentUtil fragmentUtil;
    private final ITermFactory termFactory;

    @Inject public ParseExpectationEvaluator(FragmentUtil fragmentUtil, ITermFactory termFactory) {
        this.fragmentUtil = fragmentUtil;
        this.termFactory = termFactory;
    }

    @Override public Collection<Integer> usesSelections(IFragment fragment, ParseExpectation expectation) {
        return Lists.newLinkedList();
    }

    @Override public TestPhase getPhase(ILanguageImpl language, ParseExpectation expectation) {
        return TestPhase.PARSING;
    }

    @Override public ISpoofaxTestExpectationOutput
        evaluate(ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input, ParseExpectation expectation) {
        final ISpoofaxParseUnit p = input.getFragmentResult().getParseResult();
        final ITestCase test = input.getTestCase();
        final SpoofaxTestExpectationOutputBuilder outputBuilder = new SpoofaxTestExpectationOutputBuilder(input.getTestCase());

        IFragment outputFragment = expectation.outputFragment();
        if (outputFragment != null) {
            // parse to [[concrete fragment]]
            logger.debug("Evaluating a parse to expectation (expect success: {}, lang: {}, fragment: {}).",
                expectation.successExpected(), expectation.outputLanguage(), outputFragment);

            if(!p.success()) {
                // The input fragment should parse properly
                outputBuilder.addAnalysisError("Expected parsing to succeed");
                // propagate the parse messages
                outputBuilder.propagateMessages(p.messages(), test.getFragment().getRegion());
                return outputBuilder.build(false);
            } else {
                // parse the output fragment
                final ISpoofaxParseUnit parsedFragment;
                if(expectation.outputLanguage() == null) {
                    // this implicitly means we parse with the LUT
                    parsedFragment = fragmentUtil.parseFragment(outputFragment,
                        input.getLanguageUnderTest(), input.getFragmentParserConfig(), outputBuilder);
                } else {
                    // parse with the given language
                    parsedFragment = fragmentUtil.parseFragment(outputFragment,
                        expectation.outputLanguage(), input.getFragmentParserConfig(), outputBuilder);
                }
                outputBuilder.addFragmentResult(new SpoofaxFragmentResult(outputFragment, parsedFragment, null, null));

                // compare the results and set the success boolean
                if(parsedFragment == null) {
                    outputBuilder.addAnalysisError("Expected the output fragment to parse successfully");
                    return outputBuilder.build(false);
                } else {
                    if(!TermEqualityUtil.equalsIgnoreAnnos(p.ast(), parsedFragment.ast(),
                        termFactory)) {
                        // TODO: add a nice diff of the two parse results or something
                        String message = String.format(
                            "The expected parse result did not match the actual parse result.\nParse result was: %1$s\nExpected result was: %2$s",
                            p.ast(), parsedFragment.ast());
                        outputBuilder.addAnalysisError(message);
                    }
                    return outputBuilder.build(!outputBuilder.hasErrorMessages());
                }
            }
        } else {
            if (expectation.successExpected()) {
                // parse succeeds
                if (!p.success()) {
                    final String msg = "Expected parsing to succeed";
                    outputBuilder.addAnalysisError(msg);
                        outputBuilder.propagateMessages(p.messages(), test.getFragment().getRegion());
                    return outputBuilder.build(false);
                } else {
                    return outputBuilder.build(true);
                }
            } else {
                // parse fails
                if (p.success()) {
                    final String msg = "Expected a parse failure";
                    outputBuilder.addAnalysisError(msg);
                    return outputBuilder.build(false);
                } else {
                    return outputBuilder.build(true);
                }
            }
        }
    }

}
