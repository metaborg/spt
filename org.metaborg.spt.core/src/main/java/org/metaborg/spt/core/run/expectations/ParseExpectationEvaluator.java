package org.metaborg.spt.core.run.expectations;

import java.util.Collection;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.TestPhase;
import org.metaborg.mbt.core.model.expectations.ParseExpectation;
import org.metaborg.mbt.core.run.IFragmentParserConfig;
import org.metaborg.mbt.core.run.ITestExpectationInput;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.run.FragmentUtil;
import org.metaborg.spt.core.run.ISpoofaxExpectationEvaluator;
import org.metaborg.spt.core.run.ISpoofaxTestExpectationOutput;
import org.metaborg.spt.core.run.SpoofaxFragmentResult;
import org.metaborg.spt.core.run.SpoofaxTestExpectationOutputBuilder;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.TermEqualityUtil;

import com.google.common.collect.Lists;
import javax.inject.Inject;

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
        final ISpoofaxParseUnit parseUnit = input.getFragmentResult().getParseResult();
        final SpoofaxTestExpectationOutputBuilder outputBuilder = new SpoofaxTestExpectationOutputBuilder(input.getTestCase());
        final IFragment outputFragment = expectation.outputFragment();

        final boolean success;

        switch (expectation.getExpectedResult()) {
            case Succeeds:
                // parse succeeds
                success = parseSucceeds(parseUnit, input.getTestCase(), outputBuilder);
                break;
            case Ambiguous:
                // parse ambiguous
                success = parseAmbiguous(parseUnit, input.getTestCase(), outputBuilder);
                break;
            case Fails:
                // parse fails
                success = parseFails(parseUnit, outputBuilder);
                break;
            case ToFragment:
                // parse to [[concrete fragment]]
                success = parseToConcreteFragment(input, expectation, parseUnit, input.getTestCase(), outputBuilder, outputFragment);
                break;
            default:
                throw new UnsupportedOperationException();
        }

        return outputBuilder.build(success);
    }

    private boolean parseToConcreteFragment(ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input, ParseExpectation expectation,
                                                                  ISpoofaxParseUnit parseUnit, ITestCase testCase, SpoofaxTestExpectationOutputBuilder outputBuilder, IFragment outputFragment) {
        // parse to [[concrete fragment]]
        logger.debug("Evaluating a parse to expectation (expect: {}, lang: {}, fragment: {}).",
            expectation.getExpectedResult(), expectation.outputLanguage(), outputFragment);

        // The input fragment should parse properly
        if (!parseSucceeds(parseUnit, testCase, outputBuilder)) return false;

        // parse the output fragment
        final ISpoofaxParseUnit parsedFragment;
        parsedFragment = parseFragment(input.getLanguageUnderTest(), input.getFragmentParserConfig(), expectation, outputFragment, outputBuilder);
        outputBuilder.addFragmentResult(new SpoofaxFragmentResult(outputFragment, parsedFragment, null, null));

        // compare the results and set the success boolean
        if(parsedFragment == null) {
            outputBuilder.addAnalysisError("Expected the output fragment to parse successfully");
            return false;
        }

        if(!TermEqualityUtil.equalsIgnoreAnnos(parseUnit.ast(), parsedFragment.ast(),
            termFactory)) {
            // TODO: add a nice diff of the two parse results or something
            String message = String.format(
                "The expected parse result did not match the actual parse result.\nParse result was: %1$s\nExpected result was: %2$s",
                parseUnit.ast(), parsedFragment.ast());
            outputBuilder.addAnalysisError(message);
        }

        return !outputBuilder.hasErrorMessages();
    }

    private boolean parseFails(ISpoofaxParseUnit parseUnit, SpoofaxTestExpectationOutputBuilder outputBuilder) {
        // parse fails
        if (parseUnit.success()) {
            final String msg = "Expected parsing to fail, but it succeeded" + (parseUnit.isAmbiguous() ? " with ambiguities" : "");
            outputBuilder.addAnalysisError(msg);
            return false;
        }

        return true;
    }

    private boolean parseSucceeds(ISpoofaxParseUnit parseUnit, ITestCase testCase, SpoofaxTestExpectationOutputBuilder outputBuilder) {
        // parse succeeds
        if (!parseUnit.success()) {
            final String msg = "Expected parsing to succeed, but it failed";
            outputBuilder.addAnalysisError(msg);
            outputBuilder.propagateMessages(parseUnit.messages(), testCase.getFragment().getRegion());
            return false;
        }

        if (parseUnit.isAmbiguous()) {
            final String msg = "Expected parsing to be unambiguous, but it was ambiguous";
            outputBuilder.addAnalysisError(msg);
            outputBuilder.propagateMessages(parseUnit.messages(), testCase.getFragment().getRegion());
            return false;
        }

        return true;
    }

    private boolean parseAmbiguous(ISpoofaxParseUnit parseUnit, ITestCase testCase, SpoofaxTestExpectationOutputBuilder outputBuilder) {
        // parse succeeds
        if (!parseUnit.success()) {
            final String msg = "Expected parsing to succeed, but it failed";
            outputBuilder.addAnalysisError(msg);
            outputBuilder.propagateMessages(parseUnit.messages(), testCase.getFragment().getRegion());
            return false;
        }

        if (!parseUnit.isAmbiguous()) {
            final String msg = "Expected parsing to be ambiguous, but it was unambiguous";
            outputBuilder.addAnalysisError(msg);
            outputBuilder.propagateMessages(parseUnit.messages(), testCase.getFragment().getRegion());
            return false;
        }

        return true;
    }

    private ISpoofaxParseUnit parseFragment(ILanguageImpl languageUnderTest, IFragmentParserConfig fragmentParserConfig,
                                            ParseExpectation expectation, IFragment outputFragment, SpoofaxTestExpectationOutputBuilder outputBuilder) {
        if(expectation.outputLanguage() == null) {
            // this implicitly means we parse with the LUT
            return fragmentUtil.parseFragment(outputFragment,
                languageUnderTest, fragmentParserConfig, outputBuilder);
        } else {
            // parse with the given language
            return fragmentUtil.parseFragment(outputFragment,
                expectation.outputLanguage(), fragmentParserConfig, outputBuilder);
        }
    }

}
