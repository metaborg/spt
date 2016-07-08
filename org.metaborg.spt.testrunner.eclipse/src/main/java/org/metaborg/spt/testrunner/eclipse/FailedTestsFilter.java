package org.metaborg.spt.testrunner.eclipse;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.metaborg.spt.core.run.ISpoofaxTestResult;
import org.metaborg.spt.testrunner.eclipse.model.TestCaseRun;
import org.metaborg.spt.testrunner.eclipse.model.TestSuiteRun;

public class FailedTestsFilter extends ViewerFilter {

    @Override public boolean select(Viewer viewer, Object parentElement, Object element) {
        if(element instanceof TestCaseRun) {
            ISpoofaxTestResult r = ((TestCaseRun) element).result();
            return r != null && !r.isSuccessful();
        } else if(element instanceof TestSuiteRun) {
            return ((TestSuiteRun) element).numFailed() > 0;
        }
        return true;
    }

}
