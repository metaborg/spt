package org.strategoxt.imp.testing.ui.views;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.strategoxt.imp.testing.ui.model.TestcaseRun;
import org.strategoxt.imp.testing.ui.model.TestsuiteRun;

public class FailedTestsFilter extends ViewerFilter {

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof TestcaseRun) {
			TestcaseRun tcr = (TestcaseRun) element;
			return !tcr.hasSucceeded();
		} else if (element instanceof TestsuiteRun) {
			TestsuiteRun tsr = (TestsuiteRun) element;
			return tsr.getNrFailedTests() > 0;
		}
		return true;
	}

}
