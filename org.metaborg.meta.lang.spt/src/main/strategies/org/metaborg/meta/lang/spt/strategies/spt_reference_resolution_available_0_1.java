package org.metaborg.meta.lang.spt.strategies;

import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.spoofax.core.tracing.ISpoofaxResolverService;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.inject.Injector;

/**
 * Check if reference resolution is available for the given language.
 * If it is, acts like the identity function, otherwise fails.
 */
public class spt_reference_resolution_available_0_1 extends Strategy {
	public static final spt_reference_resolution_available_0_1 instance = new spt_reference_resolution_available_0_1();
	private spt_reference_resolution_available_0_1(){}
	
	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm UNUSED, IStrategoTerm langName) {
		final IContext metaContext = (IContext) context.contextObject();
		final Injector injector = metaContext.injector();
		final ISpoofaxResolverService resolver = injector.getInstance(ISpoofaxResolverService.class);
		final ILanguageService languageService = injector.getInstance(ILanguageService.class);
		
		if (resolver.available(languageService.getLanguage(Term.asJavaString(langName)).activeImpl())) {
			return UNUSED;
		} else {
			return null;
		}
	}
}
