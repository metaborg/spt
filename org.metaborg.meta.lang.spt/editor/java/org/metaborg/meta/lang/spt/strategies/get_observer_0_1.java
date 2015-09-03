package org.metaborg.meta.lang.spt.strategies;

import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.spoofax.core.analysis.AnalysisFacet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.inject.Injector;

/**
 * Retrieves the name of the Observer strategy of the given language. This can then be used by the
 * plugin-strategy-invoke strategy.
 */
public class get_observer_0_1 extends Strategy {
    private static final Logger logger = LoggerFactory.getLogger(get_observer_0_1.class);
    public static final get_observer_0_1 instance = new get_observer_0_1();


    @Override public IStrategoTerm invoke(Context context, IStrategoTerm unused, IStrategoTerm language) {
        logger.info("Getting observer for language {}", language);
        final Injector injector = ((IContext) context.contextObject()).injector();
        final ILanguage lang = injector.getInstance(ILanguageService.class).getLanguage(Tools.asJavaString(language));
        final ILanguageImpl impl = lang.activeImpl();
        return context.getFactory().makeString(impl.facet(AnalysisFacet.class).strategyName);
    }
}
