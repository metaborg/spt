package org.metaborg.spt.core.expectations;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageBuilder;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.IFragment;
import org.metaborg.spt.core.IFragmentBuilder;
import org.metaborg.spt.core.IFragmentParser;
import org.metaborg.spt.core.ITestCase;
import org.metaborg.spt.core.ITestExpectation;
import org.metaborg.spt.core.ITestExpectationInput;
import org.metaborg.spt.core.ITestExpectationOutput;
import org.metaborg.spt.core.TestExpectationOutput;
import org.metaborg.spt.core.TestPhase;
import org.metaborg.spt.core.util.SPTUtil;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;

import com.google.inject.Inject;

/**
 * Implementation for the evaluation of the 'parse succeeds' and 'parse fails' test expectations.
 */
public class ParseExpectation implements ITestExpectation {

    private static final ILogger logger = LoggerUtils.logger(ParseExpectation.class);
    private static final String SUC = "ParseSucceeds";
    private static final String FAIL = "ParseFails";
    private static final String TO = "ParseTo";

    private final IFragmentBuilder fragmentBuilder;
    private final IFragmentParser fragmentParser;
    private final ILanguageService langService;

    @Inject public ParseExpectation(IFragmentBuilder fragmentBuilder, IFragmentParser fragmentParser,
        ILanguageService langService) {
        this.fragmentBuilder = fragmentBuilder;
        this.fragmentParser = fragmentParser;
        this.langService = langService;
    }

    @Override public boolean canEvaluate(IStrategoTerm expectationTerm) {
        String cons = SPTUtil.consName(expectationTerm);
        return cons != null && (SUC.equals(cons) || FAIL.equals(cons));
    }

    @Override public TestPhase getPhase(IStrategoTerm expectationTerm) {
        return TestPhase.PARSING;
    }

    @Override public ITestExpectationOutput evaluate(ITestExpectationInput input) {
        IStrategoTerm expectation = input.getExpectation();
        logger.debug("About to evaluate a ParseExpectation: {}", expectation);
        ISpoofaxParseUnit p = input.getParseResult();
        ITestCase test = input.getTestCase();
        final boolean success;

        List<IMessage> messages = new LinkedList<>();

        switch(SPTUtil.consName(expectation)) {
            case FAIL:
                success = !p.success();
                if(p.success()) {
                    messages.add(buildMessage(test, "Expected a parse failure", test.getDescriptionRegion()));
                }
                break;
            case SUC:
                success = p.success();
                if(!p.success()) {
                    messages.add(buildMessage(test, "Expected parsing to succeed", test.getDescriptionRegion()));
                    // propagate the parse messages
                    propagateMessages(p.messages(), messages, test.getDescriptionRegion());
                }
                break;
            case TO:
                if(!p.success()) {
                    messages.add(buildMessage(test, "Expected parsing to succeed", test.getDescriptionRegion()));
                    // propagate the parse messages
                    propagateMessages(p.messages(), messages, test.getDescriptionRegion());
                }

                // It's a ParseTo(languageName, openMarker, fragment, closeMarker)
                IStrategoTerm langTerm = expectation.getSubterm(0);
                IStrategoTerm fragmentTerm = expectation.getSubterm(2);
                String langName = Term.asJavaString(langTerm);
                ILanguage lang = langService.getLanguage(langName);
                if(lang == null) {
                    messages.add(buildMessage(test, "Could not find the language " + langName,
                        SPTUtil.getRegion(expectation.getSubterm(0))));
                    success = false;
                    break;
                }

                // parse the fragment
                IFragment fragment = fragmentBuilder.withFragment(fragmentTerm).build();
                final ISpoofaxParseUnit parsedFragment;
                try {
                    // TODO: would we ever need to use a dialect?
                    parsedFragment = fragmentParser.parse(fragment, lang.activeImpl(), null);
                } catch(ParseException e) {
                    messages.add(buildMessage(test, "Unable to parse the fragment due to an exception",
                        SPTUtil.getRegion(fragmentTerm), e));
                    success = false;
                    break;
                }
                if(!parsedFragment.success()) {
                    messages.add(buildMessage(test, "Expected the fragment to parse", SPTUtil.getRegion(fragmentTerm)));
                    // propagate messages
                    propagateMessages(parsedFragment.messages(), messages, SPTUtil.getRegion(fragmentTerm));
                }

                // FIXME: TODO: compare the results and set the success boolean
                // the way I do it now is obviously wrong, I just want to commit a compiling program
                if(p != parsedFragment) {
                    // TODO: add a nice diff of the two parse results or something
                    messages.add(buildMessage(test, "The expected parse result did not match the actual parse result",
                        test.getDescriptionRegion()));
                }

                success = messages.isEmpty();

                break;
            default:
                throw new IllegalArgumentException("This test expectation can't evaluate " + expectation);
        }

        return new TestExpectationOutput(success, messages);
    }

    private void propagateMessages(Iterable<IMessage> toPropagate, Collection<IMessage> messages,
        ISourceRegion defaultRegion) {
        for(IMessage message : toPropagate) {
            if(message.region() == null) {
                // assign the message to the test's description if it has no region
                messages.add(SPTUtil.setRegion(message, defaultRegion));
            } else {
                messages.add(message);
            }
        }
    }

    private IMessage buildMessage(ITestCase test, String message, ISourceRegion region) {
        return buildMessage(test, message, region, null);
    }

    private IMessage buildMessage(ITestCase test, String message, ISourceRegion region, Throwable e) {
        MessageBuilder b = MessageBuilder.create()
            // @formatter:off
            .asAnalysis()
            .asError()
            .withMessage(message)
            .withRegion(region);
            // @formatter:on
        if(test.getResource() != null) {
            b.withSource(test.getResource());
        }
        if(e != null) {
            b.withException(e);
        }
        return b.build();
    }

}
