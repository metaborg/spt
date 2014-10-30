package org.strategoxt.imp.testing.strategies;

import static org.spoofax.interpreter.core.Tools.isTermString;

import java.util.Iterator;

import org.metaborg.spt.listener.ITestReporter;
import org.metaborg.spt.listener.TestReporterProvider;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;
//import org.strategoxt.imp.testing.listener.helper.ListenerWrapper;

public class testlistener_add_testsuite_0_2 extends Strategy {

	public static testlistener_add_testsuite_0_2 instance = new testlistener_add_testsuite_0_2();

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm arg0, IStrategoTerm arg1) {
		if (!isTermString(arg0) || !isTermString(arg1))
			return null;

		String name = ((IStrategoString) arg0).stringValue();
		String filename = ((IStrategoString) arg1).stringValue();

		try {
//			ITestListener listener = ListenerWrapper.instance();
//			listener.addTestsuite(name, filename);
			Iterator<ITestReporter> it = TestReporterProvider.getInstance().getReporters();
			while (it != null && it.hasNext()) {
				it.next().addTestsuite(name, filename);
			}
		} catch (Exception e) {
			ITermFactory factory = context.getFactory();
//			Environment.logException("Failed to add test suite to listener. Maybe no listeners?", e);
			return factory.makeAppl(factory.makeConstructor("Error", 1), factory
					.makeString("Failed to add test suite to listener. Maybe no listeners?: " + e.getLocalizedMessage()));
		}

		return current;
	}

}
