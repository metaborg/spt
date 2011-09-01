package org.strategoxt.imp.testing.strategies;

import static org.spoofax.interpreter.core.Tools.isTermString;

import java.lang.reflect.Method;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class testview_finish_testcase_0_3 extends Strategy {

	public static testview_finish_testcase_0_3 instance = new testview_finish_testcase_0_3();
	
	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm arg0, IStrategoTerm arg1, IStrategoTerm arg2) {
		if (!isTermString(arg0) || !isTermString(arg1) ) return null;
		
		final String ts = ((IStrategoString)arg0).stringValue();
		final String desc = ((IStrategoString)arg1).stringValue();
		final String appl = ((IStrategoAppl)arg2).getConstructor().getName();
		
		Display.getDefault().syncExec(new Runnable() {
            public void run() {
			  	try {
					IViewPart a = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("org.strategoxt.imp.testing.views.testrunviewpart");
					// Using reflection, because if I use a cast, I get a ClassCastException 
					// cannot cast type <x> to <x>. Probably because of some different classloader issue.
					Method m = a.getClass().getMethod("finishTestcase", new Class[] {String.class, String.class, boolean.class}) ;
					if(appl.equals("True")) {
						m.invoke(a, ts, desc, true);
					} else {
						m.invoke(a, ts, desc, false);
					}
			  	} catch(Exception e) {
			  		e.printStackTrace();
			  	}  
            }
		});
		
		return current;
	}

}
