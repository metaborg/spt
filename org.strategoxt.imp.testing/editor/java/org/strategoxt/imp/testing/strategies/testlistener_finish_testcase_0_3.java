package org.strategoxt.imp.testing.strategies;

import static org.spoofax.interpreter.core.Tools.isTermString;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.imp.testing.Activator;
import org.strategoxt.imp.testing.listener.ITestListener;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class testlistener_finish_testcase_0_3 extends Strategy {

	public static testlistener_finish_testcase_0_3 instance = new testlistener_finish_testcase_0_3();

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm arg0, IStrategoTerm arg1,
			IStrategoTerm arg2) {
		if (!isTermString(arg0) || !isTermString(arg1))
			return null;

		final String ts = ((IStrategoString) arg0).stringValue();
		final String desc = ((IStrategoString) arg1).stringValue();
		final String appl = ((IStrategoAppl) arg2).getConstructor().getName();

		// Display.getDefault().syncExec(new Runnable() {
		// public void run() {
		// try {
		// IViewPart a =
		// PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("org.strategoxt.imp.testing.views.testrunviewpart");
		// // Using reflection, because if I use a cast, I get a ClassCastException
		// // cannot cast type <x> to <x>. Probably because of some different classloader issue.
		// Method m = a.getClass().getMethod("finishTestcase", new Class[] {String.class, String.class, boolean.class})
		// ;
		// if(appl.equals("True")) {
		// m.invoke(a, ts, desc, true);
		// } else {
		// m.invoke(a, ts, desc, false);
		// }
		// } catch(Exception e) {
		// e.printStackTrace();
		// }
		// }
		// });
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(
				ITestListener.EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				final Object o = e.createExecutableExtension("class");
				ISafeRunnable runner = new ISafeRunnable() {

					public void run() throws Exception {
						// Using reflection, because if I use a cast, I get a ClassCastException
						// cannot cast type <x> to <x>. Probably because of some different classloader issue.
						Method m = o.getClass().getMethod("finishTestcase",
								new Class[] { String.class, String.class, boolean.class });
						if (!Modifier.isAbstract(m.getModifiers())) {
							if (appl.equals("True")) {
								m.invoke(o, ts, desc, true);
							} else {
								m.invoke(o, ts, desc, false);
							}
						}
					}

					public void handleException(Throwable exception) {
						//
					}
				};
				SafeRunner.run(runner);

			}
			if(config.length == 0){
				Activator.getInstance().getLog().log(new Status(IStatus.INFO, Activator.kPluginID, "No TestListeners available to listen for test status"));
			}
		} catch (CoreException cex) {
			Activator.getInstance().getLog().log(new Status(IStatus.ERROR, Activator.kPluginID, "Failed to notify listeners of updated test status. Maybe no listeners?", cex));
		}
		return current;
	}

}
