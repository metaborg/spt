package org.metaborg.meta.lang.spt.strategies;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.spoofax.core.stratego.StrategoFacet;
import org.metaborg.sunshine.environment.ServiceRegistry;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class get_reference_resolvers_0_1 extends Strategy {
    public static final get_reference_resolvers_0_1 instance = new get_reference_resolvers_0_1();


    @Override public IStrategoTerm invoke(Context context, IStrategoTerm unused, IStrategoTerm language) {
        final ILanguageImpl lang =
            ServiceRegistry.INSTANCE().getService(ILanguageService.class).getLanguage(Tools.asJavaString(language));
        final String resolver = lang.facets(StrategoFacet.class).resolverStrategy();
        final ITermFactory factory = context.getFactory();
        return factory.makeList(factory.makeString(resolver));
    }
}
