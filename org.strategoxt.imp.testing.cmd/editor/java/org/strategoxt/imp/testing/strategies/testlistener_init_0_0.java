package org.strategoxt.imp.testing.strategies;

import java.util.Iterator;

import org.metaborg.spt.listener.ITestReporter;
import org.metaborg.spt.listener.TestReporterProvider;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class testlistener_init_0_0 extends Strategy {

	public static testlistener_init_0_0 instance = new testlistener_init_0_0();

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current) {
		
		try {
//			ITestListener listener = ListenerWrapper.instance();
//			listener.reset();
			Iterator<ITestReporter> it = TestReporterProvider.getInstance().getReporters();
			while (it != null && it.hasNext()) {
				it.next().reset();
			}
		} catch (Exception e) {
			ITermFactory factory = context.getFactory();
//			Environment.logException("Failed to reset test listener. Maybe no listeners?", e);
			return factory.makeAppl(factory.makeConstructor("Error", 1), factory
					.makeString("Failed to reset test listener. Maybe no listeners?: " + e.getLocalizedMessage()));
		}

		return current;
	}

}
