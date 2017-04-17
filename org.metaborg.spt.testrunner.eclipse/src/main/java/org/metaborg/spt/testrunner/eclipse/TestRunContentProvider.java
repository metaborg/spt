package org.metaborg.spt.testrunner.eclipse;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.metaborg.spt.testrunner.eclipse.model.MultiTestSuiteRun;
import org.metaborg.spt.testrunner.eclipse.model.TestCaseRun;
import org.metaborg.spt.testrunner.eclipse.model.TestSuiteRun;

/**
 * A content provider to turn our data model into something the treeviewer can understand.
 */
public class TestRunContentProvider implements ITreeContentProvider {

    @Override public void dispose() {
        // apparently we don't need this function
        // TODO Auto-generated method stub
    }

    @Override public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // apparently we don't need this function
        // TODO Auto-generated method stub
    }

    @Override public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    @Override public Object[] getChildren(Object parentElement) {
        if(parentElement instanceof TestSuiteRun) {
            return ((TestSuiteRun) parentElement).tests.toArray();
        } else if(parentElement instanceof MultiTestSuiteRun) {
            return ((MultiTestSuiteRun) parentElement).suites.toArray();
        }
        return new Object[] {};
    }

    @Override public Object getParent(Object element) {
        if(element instanceof TestCaseRun) {
            return ((TestCaseRun) element).parent;
        } else if(element instanceof TestSuiteRun) {
            return ((TestSuiteRun) element).parent;
        }
        return null;
    }

    @Override public boolean hasChildren(Object element) {
        return getChildren(element).length > 0;
    }

}