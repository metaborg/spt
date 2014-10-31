package org.strategoxt.imp.testing.cmd.strategies;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

/**
 * Dummy implementation
 */
public class get_reference_resolvers_0_1 extends Strategy {

    public static get_reference_resolvers_0_1 instance = new get_reference_resolvers_0_1();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm unused, IStrategoTerm language) {
        // ILanguage lang =
        // ServiceRegistry.INSTANCE().getService(ILanguageService.class).get(Tools.asJavaString(language));
        // String[] resolverStrings = new String[0];//lang.getResolverFunctions();
        // ITermFactory f = context.getFactory();
        // IStrategoTerm[] resolvers = new IStrategoTerm[resolverStrings.length];
        // for (int i = 0; i < resolverStrings.length; i++) {
        // resolvers[i] = f.makeString(resolverStrings[i]);
        // }
        // return f.makeList(resolvers);
        return context.getFactory().makeList();
    }
}
