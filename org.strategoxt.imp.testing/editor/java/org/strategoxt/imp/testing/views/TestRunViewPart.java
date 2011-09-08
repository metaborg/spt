package org.strategoxt.imp.testing.views;

import java.io.File;
import java.io.FileNotFoundException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.internal.junit.ui.JUnitProgressBar;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;
import org.strategoxt.imp.runtime.EditorState;
import org.strategoxt.imp.runtime.stratego.EditorIOAgent;
import org.strategoxt.imp.testing.model.TestRun;
import org.strategoxt.imp.testing.model.TestcaseRun;
import org.strategoxt.imp.testing.model.TestsuiteRun;

public class TestRunViewPart extends ViewPart {

	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	private TestRun testrun = null;
	private Label lblRatio;
	private final static int LBLRATIO_WIDTHHINT = 65;
	private JUnitProgressBar pb;
	private TreeViewer treeViewer;
	private int nrFailedTests = 0;
	private Action onlyFailedTestsAction;
	private ViewerFilter failedTestsFilter;

	public TestRunViewPart() {

	}

	/**
	 * Create contents of the view part.
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		GridData gd = null;

		GridLayout layout = new GridLayout(3, false);
		parent.setLayout(layout);

		pb = new JUnitProgressBar(parent);
		gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.verticalAlignment = SWT.TOP;
		pb.setLayoutData(gd);

		Label lblTests = new Label(parent, SWT.NONE);
		lblTests.setText("Tests");
		gd = new GridData();
		gd.horizontalAlignment = SWT.BEGINNING;
		lblTests.setLayoutData(gd);

		lblRatio = new Label(parent, SWT.RIGHT);
		gd = new GridData();
		gd.horizontalAlignment = SWT.END;
		gd.widthHint = LBLRATIO_WIDTHHINT;
		lblRatio.setLayoutData(gd);

		treeViewer = new TreeViewer(parent, SWT.BORDER);
		Tree tv = treeViewer.getTree();
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		gd.horizontalAlignment = SWT.FILL;
		gd.verticalAlignment = SWT.FILL;
		gd.horizontalSpan = 3;
		tv.setLayoutData(gd);

		TreeColumn column = new TreeColumn(treeViewer.getTree(), SWT.NONE);

		column.setText("");
		column.pack();

		treeViewer.setContentProvider(new TestRunContentProvider());
		treeViewer.setLabelProvider(new TestRunLabelProvider());
		treeViewer.setSorter(new ViewerSorter());
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(DoubleClickEvent event) {
				Object selectObject = ((IStructuredSelection) treeViewer
						.getSelection()).getFirstElement();

				String file = null;
				int offset = 0;

				if (selectObject instanceof TestcaseRun) {
					TestcaseRun tcr = (TestcaseRun) selectObject;
					file = tcr.getParent().getFilename();
					offset = tcr.getOffset();
				} else if (selectObject instanceof TestsuiteRun) {
					file = ((TestsuiteRun) selectObject).getFilename();
				}

				if (file != null) {
					File f = new File(file);
					IResource res;
					try {
						res = EditorIOAgent.getResource(f);
						EditorState.asyncOpenEditor(Display.getDefault(),
								(IFile) res, offset, true);
					} catch (FileNotFoundException e) {
						org.strategoxt.imp.runtime.Environment.logException(
								"File not found", e);
					}
				}
			}
		});

		createActions();
		createFilters();
		initializeToolBar();
		initializeMenu();

		reset();

		testrun = new TestRun();

		treeViewer.expandAll();

		updateHeader();

	}

	private void updateHeader() {
		int nrTests = testrun.getNrTests();
		if (testrun == null) {
			lblRatio.setText("0 / 0");
		} else {
			lblRatio.setText(String.format("%d / %d    ",
					(nrTests - nrFailedTests), nrTests));
		}
		pb.setMaximum(nrTests);
	}

	@Override
	public void dispose() {
		toolkit.dispose();
		super.dispose();
	}

	/**
	 * Create the actions.
	 */
	private void createActions() {
		onlyFailedTestsAction = new Action("Show only failed tests",Action.AS_CHECK_BOX) {
			public void run() {
				if (onlyFailedTestsAction.isChecked()) {
					treeViewer.addFilter(failedTestsFilter);
				} else {
					treeViewer.removeFilter(failedTestsFilter);
				}

			}
		};

	}

	private void createFilters(){
		failedTestsFilter = new FailedTestsFilter();
	}
	
	/**
	 * Initialize the toolbar.
	 */
	private void initializeToolBar() {
	}

	/**
	 * Initialize the menu.
	 */
	private void initializeMenu() {
		IMenuManager mgr = getViewSite().getActionBars().getMenuManager();
        mgr.add(onlyFailedTestsAction);
	}

	@Override
	public void setFocus() {
	}

	public void reset() {
		nrFailedTests = 0;
		testrun = new TestRun();
		treeViewer.setInput(testrun);
		pb.reset();
	}

	public void refresh() {
		updateHeader();
		treeViewer.refresh();
		treeViewer.expandAll();

		final Tree tree = treeViewer.getTree();
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				for (TreeColumn tc : tree.getColumns()) {
					tc.pack();
				}
			}
		});
	}

	public void addTestsuite(String name, String filename) {
		testrun.addTestsuite(name, filename);
		refresh();
	}

	public void addTestcase(String testsuite, String description, int offset) {
		TestsuiteRun ts = testrun.getTestsuite(testsuite);
		ts.addTestCase(description, offset);
		refresh();
	}

	public void startTestcase(String testsuite, String description) {
		TestcaseRun tcr = testrun.getTestsuite(testsuite).getTestcase(
				description);
		tcr.start();
	}

	public void finishTestcase(String testsuite, String description,
			boolean succeeded) {
		TestcaseRun tcr = testrun.getTestsuite(testsuite).getTestcase(
				description);
		tcr.finished(succeeded);
		if (!succeeded)
			nrFailedTests++;
		pb.step(nrFailedTests);
		refresh();
	}
}
