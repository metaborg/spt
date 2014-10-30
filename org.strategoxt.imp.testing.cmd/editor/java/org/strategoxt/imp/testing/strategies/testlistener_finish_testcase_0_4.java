package org.strategoxt.imp.testing.strategies;

import static org.spoofax.interpreter.core.Tools.isTermString;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.metaborg.spt.listener.ITestReporter;
import org.metaborg.spt.listener.TestReporterProvider;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class testlistener_finish_testcase_0_4 extends Strategy {

	public static testlistener_finish_testcase_0_4 instance = new testlistener_finish_testcase_0_4();

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm filename, IStrategoTerm description,
			IStrategoTerm result, IStrategoTerm message) {
		if (!isTermString(filename) || !isTermString(filename))
			return null;

		String ts = ((IStrategoString) filename).stringValue();
		String desc = ((IStrategoString) description).stringValue();
		String appl = ((IStrategoAppl) result).getConstructor().getName();
		
		// FIXME SPT only really gives 1 message, but maybe in the future we can support more
		List<String> messages = new ArrayList<String>();
		if (message instanceof IStrategoTuple) {
			IStrategoList messageList = (IStrategoList) ((IStrategoTuple) message).get(1);
			for (IStrategoTerm messageTerm : messageList.getAllSubterms()) {
				messages.add(((IStrategoString) messageTerm).stringValue());			
			}
		}
		
		try {
//			ITestListener listener = ListenerWrapper.instance();
//			if (appl.equals("True"))
//				listener.finishTestcase(ts, desc, true, messages);
//			else
//				listener.finishTestcase(ts, desc, false, messages);
			Iterator<ITestReporter> it = TestReporterProvider.getInstance().getReporters();
			while (it != null && it.hasNext()) {
				if (appl.equals("True")) {
					it.next().finishTestcase(ts, desc, true, messages);
				} else {
					it.next().finishTestcase(ts, desc, false, messages);
				}
			}
			
		} catch (Exception e) {
			ITermFactory factory = context.getFactory();
//			Environment.logException("Failed to finish test case to listener. Maybe no listeners?", e);
			return factory.makeAppl(
					factory.makeConstructor("Error", 1),
					factory.makeString("Failed to finish test case to listener. Maybe no listeners?: "
							+ e.getLocalizedMessage()));
		}

		return current;
	}

}
