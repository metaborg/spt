package org.metaborg.spt.core.expectations;

import java.util.LinkedList;
import java.util.List;

import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
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
import org.strategoxt.lang.TermEqualityUtil;

import com.google.inject.Inject;

/**
 * Implementation for the evaluation of the 'parse succeeds' and 'parse fails' test expectations.
 */
public class ParseExpectation implements ITestExpectation {

    private static final ILogger logger = LoggerUtils.logger(ParseExpectation.class);
    private static final String SUC = "ParseSucceeds";
    private static final String FAIL = "ParseFails";
    private static final String TO = "ParseTo";

    private final FragmentUtil fragmentUtil;
    private final ITermFactoryService termFactoryService;

    @Inject public ParseExpectation(FragmentUtil fragmentUtil, ITermFactoryService termFactoryService) {
        this.fragmentUtil = fragmentUtil;
        this.termFactoryService = termFactoryService;
    }

    @Override public boolean canEvaluate(IStrategoTerm expectationTerm) {
        String cons = SPTUtil.consName(expectationTerm);
        return cons != null && (SUC.equals(cons) || FAIL.equals(cons) || TO.equals(cons));
    }

    @Override public TestPhase getPhase(IStrategoTerm expectationTerm) {
        return TestPhase.PARSING;
    }

    @Override public ITestExpectationOutput evaluate(ITestExpectationInput input) {
        IStrategoTerm expectation = input.getExpectation();
        ISpoofaxParseUnit p = input.getParseResult();
        ITestCase test = input.getTestCase();
        final boolean success;

        List<IMessage> messages = new LinkedList<>();

        switch(SPTUtil.consName(expectation)) {
            case FAIL:
                success = !p.success();
                if(p.success()) {
                    messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                        "Expected a parse failure", null));
                }
                break;
            case SUC:
                success = p.success();
                if(!p.success()) {
                    messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                        "Expected parsing to succeed", null));
                    // propagate the parse messages
                    MessageUtil.propagateMessages(p.messages(), messages, test.getDescriptionRegion());
                }
                break;
            case TO:
                if(!p.success()) {
                    logger.debug("Parsing of test failed with ast {}, valid {}", p.ast(), p.valid());
                    messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                        "Expected parsing to succeed", null));
                    // propagate the parse messages
                    MessageUtil.propagateMessages(p.messages(), messages, test.getDescriptionRegion());
                }

                // It's a ParseTo(ToPart(languageName, openMarker, fragment, closeMarker))
                IStrategoTerm toPart = expectation.getSubterm(0);
                ISpoofaxParseUnit parsedFragment = fragmentUtil.parseToPart(toPart, messages, test);
                if(parsedFragment == null) {
                    success = false;
                    break;
                }

                // compare the results and set the success boolean
                if(!TermEqualityUtil.equalsIgnoreAnnos(p.ast(), parsedFragment.ast(),
                    termFactoryService.get(fragmentUtil.langFromToPart(toPart).activeImpl()))) {
                    // TODO: add a nice diff of the two parse results or something
                    messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                        "The expected parse result did not match the actual parse result", null));
                }

                success = messages.isEmpty();

                break;
            default:
                throw new IllegalArgumentException("This test expectation can't evaluate " + expectation);
        }

        return new TestExpectationOutput(success, messages);
    }
}
