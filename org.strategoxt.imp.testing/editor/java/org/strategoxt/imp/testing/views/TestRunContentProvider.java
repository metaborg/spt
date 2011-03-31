package org.strategoxt.imp.testing.views;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.strategoxt.imp.testing.model.TestRun;
import org.strategoxt.imp.testing.model.TestcaseRun;
import org.strategoxt.imp.testing.model.TestsuiteRun;


public class TestRunContentProvider implements ITreeContentProvider {

	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub
		
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public Object[] getChildren(Object parentElement) {
	    if(parentElement instanceof TestRun) {
	    	TestRun tr = (TestRun) parentElement;
	    	return tr.getTestSuites().toArray() ;
	    } else if (parentElement instanceof TestsuiteRun) {
	    	TestsuiteRun tsr = (TestsuiteRun) parentElement;
	    	return tsr.getTestcases().toArray();
	    } 
	    return new Object[] {};
	}

	public Object getParent(Object element) {
	    if (element instanceof TestsuiteRun) {
	    	TestsuiteRun tsr = (TestsuiteRun) element;
	    	return tsr.getParent();
	    } else if (element instanceof TestcaseRun) {
	    	TestcaseRun tcr = (TestcaseRun) element;
	    	return tcr.getParent();
	    } 
    	return null ;
	}

	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}
}
