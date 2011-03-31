package org.strategoxt.imp.testing.strategies;

import java.lang.reflect.Method;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class testview_init_0_0 extends Strategy {

	public static testview_init_0_0 instance = new testview_init_0_0();
	
	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current) {
		Display.getDefault().syncExec(new Runnable() {
            public void run() {
			  	try {       
					IViewPart a = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("org.strategoxt.imp.testing.views.testrunviewpart");
					// Using reflection, because if I use a cast, I get a ClassCastException 
					// cannot cast type <x> to <x>. Probably because of some different classloader issue.
					Method m = a.getClass().getMethod("reset", new Class[] {}) ;
					m.invoke(a);
			  	} catch(Exception e) {
			  		e.printStackTrace();
			  	}  
            }
		});
		
		return current;
	}

}
