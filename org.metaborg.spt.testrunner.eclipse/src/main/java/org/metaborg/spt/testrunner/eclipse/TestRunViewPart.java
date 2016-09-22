package org.metaborg.spt.testrunner.eclipse;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spt.core.run.ISpoofaxTestResult;
import org.metaborg.spt.testrunner.eclipse.model.MultiTestSuiteRun;
import org.metaborg.spt.testrunner.eclipse.model.TestCaseRun;
import org.metaborg.spt.testrunner.eclipse.model.TestSuiteRun;

/**
 * The main View for the test runner.
 * 
 * Contains the progressbar and the treeview of test suites and test cases.
 * 
 * Basic usage:
 * <ul>
 * <li>call {@link #reset()} to reset the view.</li>
 * <li>call {@link #setData(MultiTestSuiteRun)} with the model of all the suites and tests you want to display.</li>
 * <li>call {@link #finish(TestCaseRun, ISpoofaxTestResult)} for each TestCaseRun that finished running.</li>
 * </ul>
 */
public class TestRunViewPart extends ViewPart {

    public final static String VIEW_ID = "org.metaborg.spt.testrunner.eclipse.testrunviewpart";

    private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
    private Label lblRatio;
    private final static int LBLRATIO_WIDTHHINT = 65;
    private JUnitProgressBar pb;
    private TreeViewer treeViewer;
    private Action onlyFailedTestsAction;
    private ViewerFilter failedTestsFilter;

    // the model part
    private int nrFailedTests = 0;
    private MultiTestSuiteRun run = null;

    /**
     * Create contents of the view part.
     * 
     * @param parent
     */
    @Override public void createPartControl(Composite parent) {
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
                Object selectObject = ((IStructuredSelection) treeViewer.getSelection()).getFirstElement();

                FileObject file = null;
                int offset = 0;

                if(selectObject instanceof TestCaseRun) {
                    TestCaseRun tcr = (TestCaseRun) selectObject;
                    file = tcr.test.getResource();
                    offset = tcr.test.getDescriptionRegion().startOffset();
                } else if(selectObject instanceof TestSuiteRun) {
                    TestSuiteRun tsr = ((TestSuiteRun) selectObject);
                    file = tsr.file;
                }

                if(file != null) {
                    // GROSS!
                    final Spoofax spoofax = SpoofaxPlugin.spoofax();
                    IEclipseResourceService r = spoofax.injector.getInstance(IEclipseResourceService.class);
                    IPath p = r.unresolve(file).getFullPath();
                    IFile f = ResourcesPlugin.getWorkspace().getRoot().getFile(p);
                    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                    try {
                        IEditorPart ep = IDE.openEditor(page, f);
                        if(ep instanceof ITextEditor) {
                            ((ITextEditor) ep).selectAndReveal(offset, 0);
                        }
                    } catch(PartInitException e) {
                        // whatever
                    }
                }
            }
        });

        createActions();
        createFilters();
        initializeMenu();

        reset();

        run = new MultiTestSuiteRun();

        treeViewer.expandAll();

        updateHeader();

    }

    private void updateHeader() {
        int nrTests = run.numTests();
        if(nrTests == 0) {
            lblRatio.setText("0 / 0");
        } else {
            lblRatio.setText(String.format("%d / %d    ", (nrTests - nrFailedTests), nrTests));
        }
        pb.setMaximum(nrTests);
    }

    @Override public void dispose() {
        toolkit.dispose();
        super.dispose();
    }

    /**
     * Create the actions.
     */
    private void createActions() {
        onlyFailedTestsAction = new Action("Show only failed tests", Action.AS_CHECK_BOX) {
            public void run() {
                if(onlyFailedTestsAction.isChecked()) {
                    treeViewer.addFilter(failedTestsFilter);
                } else {
                    treeViewer.removeFilter(failedTestsFilter);
                }

            }
        };

    }

    private void createFilters() {
        failedTestsFilter = new FailedTestsFilter();
    }

    /**
     * Initialize the menu.
     */
    private void initializeMenu() {
        IMenuManager mgr = getViewSite().getActionBars().getMenuManager();
        mgr.add(onlyFailedTestsAction);
    }

    @Override public void setFocus() {
    }

    public void reset() {
        nrFailedTests = 0;
        run = new MultiTestSuiteRun();
        treeViewer.setInput(run);
        pb.reset();
        if(!refreshDisabled)
            refresh();
    }

    private void refresh() {
        updateHeader();
        treeViewer.refresh();
        pb.redraw();
        treeViewer.expandAll();

        final Tree tree = treeViewer.getTree();
        Display.getCurrent().asyncExec(new Runnable() {
            public void run() {
                for(TreeColumn tc : tree.getColumns()) {
                    tc.pack();
                }
            }
        });
    }

    public void setData(MultiTestSuiteRun run) {
        this.run = run;
        this.nrFailedTests = run.numFailed();
        treeViewer.setInput(run);
        pb.reset();
        pb.setMaximum(run.numTests());
        if(!refreshDisabled) {
            refresh();
        }
    }

    public void finish(TestCaseRun t, ISpoofaxTestResult res) {
        t.finish(res);
        if(!res.isSuccessful()) {
            nrFailedTests++;
        }
        pb.step(nrFailedTests);
        if(!refreshDisabled)
            refresh();
    }

    private boolean refreshDisabled = false;

    public void disableRefresh(boolean b) {
        refreshDisabled = b;
        if(!b)
            refresh();
    }
}
