package org.metaborg.spt.core.run.expectations;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.TestPhase;
import org.metaborg.mbt.core.model.expectations.MessageUtil;
import org.metaborg.mbt.core.model.expectations.ParseExpectation;
import org.metaborg.mbt.core.run.ITestExpectationInput;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.run.FragmentUtil;
import org.metaborg.spt.core.run.ISpoofaxExpectationEvaluator;
import org.metaborg.spt.core.run.ISpoofaxFragmentResult;
import org.metaborg.spt.core.run.ISpoofaxTestExpectationOutput;
import org.metaborg.spt.core.run.SpoofaxFragmentResult;
import org.metaborg.spt.core.run.SpoofaxTestExpectationOutput;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.strategoxt.lang.TermEqualityUtil;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class ParseExpectationEvaluator implements ISpoofaxExpectationEvaluator<ParseExpectation> {

    private static final ILogger logger = LoggerUtils.logger(ParseExpectationEvaluator.class);

    private final FragmentUtil fragmentUtil;
    private final ITermFactoryService termFactoryService;

    @Inject public ParseExpectationEvaluator(FragmentUtil fragmentUtil, ITermFactoryService termFactoryService) {
        this.fragmentUtil = fragmentUtil;
        this.termFactoryService = termFactoryService;
    }

    @Override public Collection<Integer> usesSelections(IFragment fragment, ParseExpectation expectation) {
        return Lists.newLinkedList();
    }

    @Override public TestPhase getPhase(IContext languageUnderTestCtx, ParseExpectation expectation) {
        return TestPhase.PARSING;
    }

    @Override public ISpoofaxTestExpectationOutput
        evaluate(ITestExpectationInput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> input, ParseExpectation expectation) {
        ISpoofaxParseUnit p = input.getFragmentResult().getParseResult();
        ITestCase test = input.getTestCase();
        final boolean success;

        List<IMessage> messages = new LinkedList<>();
        List<ISpoofaxFragmentResult> fragmentResults = Lists.newLinkedList();

        if(expectation.outputFragment() == null) {
            // this is a parse fails or succeeds test
            success = p.success() == expectation.successExpected();
            if(!success) {
                final String msg =
                    expectation.successExpected() ? "Expected parsing to succeed" : "Expected a parse failure";
                messages
                    .add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(), msg, null));
                if(expectation.successExpected()) {
                    // propagate the parse messages
                    MessageUtil.propagateMessages(p.messages(), messages, test.getDescriptionRegion(),
                        test.getFragment().getRegion());
                }
            }
        } else {
            // this is 'parse to'
            logger.debug("Evaluating a parse to expectation (expect success: {}, lang: {}, fragment: {}).",
                expectation.successExpected(), expectation.outputLanguage(), expectation.outputFragment());

            if(!p.success()) {
                // The input fragment should parse properly
                messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                    "Expected parsing to succeed", null));
                // propagate the parse messages
                MessageUtil.propagateMessages(p.messages(), messages, test.getDescriptionRegion(),
                    test.getFragment().getRegion());
                success = false;
            } else {
                // parse the output fragment
                final ISpoofaxParseUnit parsedFragment;
                if(expectation.outputLanguage() == null) {
                    // this implicitly means we parse with the LUT
                    parsedFragment = fragmentUtil.parseFragment(expectation.outputFragment(),
                        input.getLanguageUnderTest(), messages, test, input.getFragmentParserConfig());
                } else {
                    // parse with the given language
                    parsedFragment = fragmentUtil.parseFragment(expectation.outputFragment(),
                        expectation.outputLanguage(), messages, test, input.getFragmentParserConfig());
                }
                fragmentResults
                    .add(new SpoofaxFragmentResult(expectation.outputFragment(), parsedFragment, null, null));

                // compare the results and set the success boolean
                if(parsedFragment == null) {
                    messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                        "Expected the output fragment to parse succesfully", null));
                    success = false;
                } else {
                    ILanguage outputLang =
                        expectation.outputLanguage() == null ? input.getLanguageUnderTest().belongsTo()
                            : fragmentUtil.getLanguage(expectation.outputLanguage(), messages, test);
                    if(outputLang != null && !TermEqualityUtil.equalsIgnoreAnnos(p.ast(), parsedFragment.ast(),
                        termFactoryService.get(outputLang.activeImpl(), test.getProject(), false))) {
                        // TODO: add a nice diff of the two parse results or something
                        messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                            "The expected parse result did not match the actual parse result", null));
                    }
                    success = messages.isEmpty();
                }
            }

        }

        return new SpoofaxTestExpectationOutput(success, messages, fragmentResults);
    }

}
