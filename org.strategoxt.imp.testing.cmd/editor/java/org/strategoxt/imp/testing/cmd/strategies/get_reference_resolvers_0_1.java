package org.strategoxt.imp.testing.cmd.strategies;

import org.metaborg.spoofax.core.stratego.StrategoFacet;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.language.ILanguageService;
import org.metaborg.sunshine.environment.ServiceRegistry;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class get_reference_resolvers_0_1 extends Strategy {
    public static final get_reference_resolvers_0_1 instance = new get_reference_resolvers_0_1();


    @Override public IStrategoTerm invoke(Context context, IStrategoTerm unused, IStrategoTerm language) {
        final ILanguage lang =
            ServiceRegistry.INSTANCE().getService(ILanguageService.class).get(Tools.asJavaString(language));
        final String resolver = lang.facet(StrategoFacet.class).resolverStrategy();
        final ITermFactory factory = context.getFactory();
        return factory.makeList(factory.makeString(resolver));
    }
}
