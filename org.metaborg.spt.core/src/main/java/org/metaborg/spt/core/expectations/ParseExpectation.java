package org.metaborg.spt.core.expectations;

import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.syntax.ParseResult;
import org.metaborg.spt.core.ITestExpectation;
import org.metaborg.spt.core.ITestExpectationInput;
import org.metaborg.spt.core.ITestExpectationOutput;
import org.metaborg.spt.core.TestExpectationOutput;
import org.metaborg.spt.core.TestPhase;
import org.metaborg.spt.core.util.SPTUtil;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * Implementation for the evaluation of the 'parse succeeds' and 'parse fails' test expectations.
 */
public class ParseExpectation implements ITestExpectation {

    private static final ILogger logger = LoggerUtils.logger(ParseExpectation.class);
    private static final String SUC = "ParseSucceeds";
    private static final String FAIL = "ParseFails";

    @Override public boolean canEvaluate(IStrategoTerm expectationTerm) {
        String cons = SPTUtil.consName(expectationTerm);
        return cons != null && (SUC.equals(cons) || FAIL.equals(cons));
    }

    @Override public TestPhase getPhase(IStrategoTerm expectationTerm) {
        return TestPhase.PARSING;
    }

    @Override public ITestExpectationOutput evaluate(ITestExpectationInput<IStrategoTerm, IStrategoTerm> input) {
        logger.debug("About to evaluate a ParseExpectation: {}", input.getExpectation());
        ParseResult<IStrategoTerm> p = input.getParseResult();
        logger.debug("ParseResult: {}", p);
        boolean success = p.result != null;
        // even if there is a parse error, parsing can still return an AST
        // but we should treat it as a parse failure if there are errors
        for(IMessage m : p.messages()) {
            if(m.severity() == MessageSeverity.ERROR) {
                success = false;
            }
        }

        if(FAIL.equals(SPTUtil.consName(input.getExpectation()))) {
            // if a failure is expected, a failed parse is considered a success
            success = !success;
        }

        return new TestExpectationOutput(success, p.messages());
    }

}
