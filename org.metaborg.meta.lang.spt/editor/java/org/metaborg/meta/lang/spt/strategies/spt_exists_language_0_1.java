package org.metaborg.meta.lang.spt.strategies;

import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.inject.Injector;

/**
 * Check if the given language is registered.
 */
public class spt_exists_language_0_1 extends Strategy {
    public static final spt_exists_language_0_1 instance = new spt_exists_language_0_1();

    private static final Logger logger = LoggerFactory.getLogger(spt_exists_language_0_1.class);

    @Override
    public IStrategoTerm invoke(Context context, IStrategoTerm current,
            IStrategoTerm languageNameTerm) {

        final String languageName = Tools.asJavaString(languageNameTerm);
        final Injector injector = ((IContext) context.contextObject()).injector();
        final ILanguageService langService = injector.getInstance(ILanguageService.class);

        final ILanguage lang = langService.getLanguage(languageName);
        logger.debug("Found {} when looking for language {}", lang, languageName);

        return lang == null ? null : languageNameTerm;
    }

}
