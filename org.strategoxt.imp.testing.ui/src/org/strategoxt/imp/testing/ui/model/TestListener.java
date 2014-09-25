package org.strategoxt.imp.testing.ui.model;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.strategoxt.imp.testing.strategies.ITestListener;
import org.strategoxt.imp.testing.ui.Activator;
import org.strategoxt.imp.testing.ui.views.TestRunViewPart;

public class TestListener implements ITestListener {

	public TestListener() {
	}

	private TestRunViewPart getViewPart() {
		try {
			return (TestRunViewPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.showView(TestRunViewPart.VIEW_ID);
		} catch (PartInitException e) {
			Activator.logError("Could not open view", e);
			return null;
		}
		
	}

	public void reset() {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				TestRunViewPart vp = getViewPart();
				if (vp != null) {
					vp.reset();
				}
			}
		});

	}

	public void addTestcase(final String testsuite, final String description, final int offset) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				TestRunViewPart vp = getViewPart();
				if (vp != null) {
					vp.addTestcase(testsuite, description, offset);
				}
			}
		});
	}

	public void addTestsuite(final String name, final String filename) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				TestRunViewPart vp = getViewPart();
				if (vp != null) {
					vp.addTestsuite(name, filename);
				}
			}
		});
	}

	public void startTestcase(final String testsuite, final String description) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				TestRunViewPart vp = getViewPart();
				if (vp != null) {
					vp.startTestcase(testsuite, description);
				}
			}
		});
	}

	public void finishTestcase(final String testsuite, final String description, final boolean succeeded) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				TestRunViewPart vp = getViewPart();
				if (vp != null) {
					vp.finishTestcase(testsuite, description, succeeded);
				}
			}
		});
	}

	public void disableRefresh() {
		TestRunViewPart vp = getViewPart();
		if(vp!=null)
			vp.disableRefresh(true);
		
	}

	public void enableRefresh() {
		TestRunViewPart vp = getViewPart();
		if(vp!=null)
			vp.disableRefresh(false);
		
	}

}
