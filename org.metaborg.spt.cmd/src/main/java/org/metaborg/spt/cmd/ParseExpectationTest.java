package org.metaborg.spt.cmd;

import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.syntax.ParseResult;
import org.metaborg.spt.core.ITestExpectation;
import org.metaborg.spt.core.ITestExpectationInput;
import org.metaborg.spt.core.ITestExpectationOutput;
import org.metaborg.spt.core.TestExpectationOutput;
import org.metaborg.spt.core.TestPhase;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;

public class ParseExpectationTest implements ITestExpectation {

    private static final ILogger logger = LoggerUtils.logger(ParseExpectationTest.class);

    @Override public boolean canEvaluate(IStrategoTerm expectationTerm) {
        // we can evaluate ParseSucceeds()
        if(Term.isTermAppl(expectationTerm)) {
            IStrategoConstructor cons = ((IStrategoAppl) expectationTerm).getConstructor();
            if("ParseSucceeds".equals(cons.getName()) && cons.getArity() == 0) {
                return true;
            }
        }
        return false;
    }

    @Override public TestPhase getPhase(IStrategoTerm expectationTerm) {
        return TestPhase.PARSING;
    }

    @Override public ITestExpectationOutput evaluate(ITestExpectationInput<IStrategoTerm, IStrategoTerm> input) {
        logger.info("About to evaluate a ParseExpectation: {}", input.getExpectation());
        ParseResult<IStrategoTerm> p = input.getParseResult();
        logger.debug("ParseResult: {}", p);
        boolean success = p.result != null;
        // even if there is a parse error, parsing can still succeed
        // but we should treat it as an error
        for(IMessage m : p.messages()) {
            if(m.severity() == MessageSeverity.ERROR) {
                success = false;
            }
        }
        return new TestExpectationOutput(success, p.messages());
    }

}
