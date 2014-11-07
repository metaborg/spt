package org.strategoxt.imp.testing.cmd.strategies;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class plugin_strategy_evaluate_1_2 extends Strategy {
    public static final plugin_strategy_evaluate_1_2 instance = new plugin_strategy_evaluate_1_2();


    @Override public IStrategoTerm invoke(Context context, IStrategoTerm current, Strategy printTrace,
        IStrategoTerm languageName, IStrategoTerm strategy) {
        throw new UnsupportedOperationException(
            "Transformation tests are not yet supported on the command-line.");
    }
}
