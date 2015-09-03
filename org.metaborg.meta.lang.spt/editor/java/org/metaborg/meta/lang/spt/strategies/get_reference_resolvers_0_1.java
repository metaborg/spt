package org.metaborg.meta.lang.spt.strategies;

import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.spoofax.core.tracing.ResolverFacet;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.inject.Injector;

public class get_reference_resolvers_0_1 extends Strategy {
    public static final get_reference_resolvers_0_1 instance = new get_reference_resolvers_0_1();


    @Override public IStrategoTerm invoke(Context context, IStrategoTerm unused, IStrategoTerm language) {
    	final Injector injector = ((IContext) context.contextObject()).injector();
    	final ILanguageService langService = injector.getInstance(ILanguageService.class);
        final ILanguage lang = langService.getLanguage(Tools.asJavaString(language));
        final ILanguageImpl impl = lang.activeImpl();
        final String resolver = impl.facet(ResolverFacet.class).strategyName;
        final ITermFactory factory = context.getFactory();
        return factory.makeList(factory.makeString(resolver));
    }
}
