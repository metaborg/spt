package org.strategoxt.imp.testing.views;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.strategoxt.imp.testing.model.TestcaseRun;
import org.strategoxt.imp.testing.model.TestsuiteRun;

public class TestRunLabelProvider extends LabelProvider implements ITableLabelProvider, ITableFontProvider, ITableColorProvider {
	
	
	@Override
	public Image getImage(Object element) {
		return super.getImage(element);
	}
	
	@Override
	public String getText(Object element) {
		if(element instanceof TestsuiteRun) {
			TestsuiteRun tsr = (TestsuiteRun)element;
			int failed = tsr.getNrFailedTests();
			return failed == 0 ? tsr.getName() : String.format("%s (%d failed)", tsr.getName(), failed);
		} else if(element instanceof TestcaseRun) {
			TestcaseRun tcr = (TestcaseRun)element;
			String lbl = tcr.getDescription();
			if(tcr.isFinished()) {
				lbl = lbl + " (" + String.format("%.2f", tcr.getDuration()/1000.0) +"s)";
				if (! tcr.hasSucceeded() ) {
					lbl += " : FAILED";
				}
			}
			return lbl;
		}
		return super.getText(element);
	}

	public Color getForeground(Object element, int columnIndex) {
		if(element instanceof TestcaseRun) {
			TestcaseRun tcr = (TestcaseRun)element;
			if(tcr.isFinished() && !tcr.hasSucceeded())
				return new Color(Display.getCurrent(), 159, 63, 63);
			if(tcr.isFinished() && tcr.hasSucceeded())
				return new Color(Display.getCurrent(), 10, 100, 10);
		}
		return null;
	}

	public Color getBackground(Object element, int columnIndex) {
		return null;
	}

	public Font getFont(Object element, int columnIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	public Image getColumnImage(Object element, int columnIndex) {
		return getImage(element);
	}

	public String getColumnText(Object element, int columnIndex) {
		return getText(element);
	}

}
