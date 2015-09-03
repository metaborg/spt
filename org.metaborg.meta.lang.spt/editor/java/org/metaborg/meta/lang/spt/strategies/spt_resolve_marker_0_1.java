package org.metaborg.meta.lang.spt.strategies;

import org.metaborg.core.context.IContext;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.inject.Injector;

/**
 * Resolve a Marked term to its corresponding term in the given parsed fragment.
 */
public class spt_resolve_marker_0_1 extends Strategy {
	public static spt_resolve_marker_0_1 instance = new spt_resolve_marker_0_1();

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm fragment) {
		Injector injector = ((IContext) context.contextObject()).injector();
		return new SelectionFetcher(injector).fetchOne(current, fragment);
	}
}
