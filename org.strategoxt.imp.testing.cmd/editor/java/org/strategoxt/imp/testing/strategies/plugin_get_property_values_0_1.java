package org.strategoxt.imp.testing.strategies;

import org.spoofax.interpreter.terms.IStrategoTerm;
//import org.spoofax.interpreter.terms.IStrategoAppl;
//import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

/**
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class plugin_get_property_values_0_1 extends Strategy {

	public static plugin_get_property_values_0_1 instance = new plugin_get_property_values_0_1();

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm property, IStrategoTerm language) {
		throw new UnsupportedOperationException("Not supported due to porting to Sunshine");
//		try {
//			Descriptor descriptor = ObserverCache.getInstance().getDescriptor(asJavaString(language));
//			List<IStrategoAppl> results = TermReader.collectTerms(descriptor.getDocument(), asJavaString(property));
//			return context.getFactory().makeList(results.toArray(new IStrategoTerm[results.size()]));
//		} catch (BadDescriptorException e) {
//			return null;
//		}
	}
}
