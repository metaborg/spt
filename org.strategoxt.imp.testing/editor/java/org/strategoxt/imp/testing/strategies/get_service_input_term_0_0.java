package org.strategoxt.imp.testing.strategies;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.HybridInterpreter;
import org.strategoxt.imp.runtime.services.InputTermBuilder;
import org.strategoxt.imp.runtime.stratego.StrategoTermPath;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

/**
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class get_service_input_term_0_0 extends Strategy {

	public static get_service_input_term_0_0 instance = new get_service_input_term_0_0();

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current) {
		current = StrategoTermPath.getMatchingAncestor(current, false);
		HybridInterpreter runtime = HybridInterpreter.getInterpreter(context);
		InputTermBuilder inputBuilder = new InputTermBuilder(runtime);
		return inputBuilder.makeInputTerm(current, true);
	}

}
