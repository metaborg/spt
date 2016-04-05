package org.metaborg.meta.lang.spt.strategies;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class get_completion_input_term_0_1 extends Strategy {
    public static final get_completion_input_term_0_1 instance = new get_completion_input_term_0_1();

    
    @Override public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm original) {
        throw new UnsupportedOperationException("Completion tests are not yet supported on the command-line.");
    }
}