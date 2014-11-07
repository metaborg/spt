package org.strategoxt.imp.testing.cmd.strategies;

import org.spoofax.interpreter.terms.IStrategoTerm;
// import org.spoofax.interpreter.terms.IStrategoAppl;
// import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class plugin_get_property_values_0_1 extends Strategy {
    public static final plugin_get_property_values_0_1 instance = new plugin_get_property_values_0_1();


    @Override public IStrategoTerm invoke(Context context, IStrategoTerm property, IStrategoTerm language) {
        throw new UnsupportedOperationException("Property tests are not yet supported on the command-line.");
    }
}
