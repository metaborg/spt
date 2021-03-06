package org.metaborg.spt.testrunner.eclipse;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.junit.ui.JUnitProgressBar;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
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
import org.metaborg.core.messages.IMessage;
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
 * <li>call {@link #refresh()} if the model is updated.</li>
 * <li>call {@link #setTestResult(TestCaseRun, ISpoofaxTestResult)} for each TestCaseRun that finished running.</li>
 * </ul>
 */
public class TestRunViewPart extends ViewPart {

    public final static String VIEW_ID = "org.metaborg.spt.testrunner.eclipse.testrunviewpart";

    private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
    private Label lblRatio;
    private final static int LBLRATIO_WIDTHHINT = 65;
    private final static int TREE_MINWIDTH = 100;
    private final static int CONS_MINWIDTH = 100;
    private final static int SASH_WIDTH = 5;
    private JUnitProgressBar pb;
    private Composite parent;
    private Composite top;
    private TreeViewer treeViewer;
    private SashForm sashForm;
    private Text cons;
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
    @Override public void createPartControl(Composite prnt) {
        this.parent = prnt;
        GridData gd = null;

        GridLayout layout = new GridLayout(1, false);
        parent.setLayout(layout);
        top = new Composite(parent, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.TOP;
        gd.grabExcessHorizontalSpace = true;
        top.setLayoutData(gd);

        layout = new GridLayout(3, false);
        top.setLayout(layout);

        pb = new JUnitProgressBar(top);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.verticalAlignment = SWT.TOP;
        pb.setLayoutData(gd);

        Label lblTests = new Label(top, SWT.NONE);
        lblTests.setText("Tests");
        gd = new GridData();
        gd.horizontalAlignment = SWT.BEGINNING;
        lblTests.setLayoutData(gd);

        lblRatio = new Label(top, SWT.RIGHT);
        gd = new GridData();
        gd.horizontalAlignment = SWT.END;
        gd.widthHint = LBLRATIO_WIDTHHINT;
        lblRatio.setLayoutData(gd);

        // bottom = new Composite(parent, SWT.NONE);
        // bottom.setBackground(new Color(Display.getCurrent(), 100, 0, 0));
        sashForm = new SashForm(parent, SWT.HORIZONTAL);
        sashForm.setSashWidth(SASH_WIDTH);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalSpan = 3;
        // bottom.setLayoutData(gd);
        sashForm.setLayoutData(gd);
        layout = new GridLayout(2, false);
        // bottom.setLayout(layout);
        sashForm.setLayout(layout);

        treeViewer = new TreeViewer(sashForm, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        final Tree tree = treeViewer.getTree();
        gd = new GridData();
        // gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.minimumWidth = TREE_MINWIDTH;
        tree.setLayoutData(gd);

        TreeColumn column = new TreeColumn(treeViewer.getTree(), SWT.NONE);
        column.setText("");
        column.pack();

        treeViewer.setContentProvider(new TestRunContentProvider());
        treeViewer.setLabelProvider(new TestRunLabelProvider());
        treeViewer.setSorter(new ViewerSorter());
        treeViewer.addDoubleClickListener(new JumpToFileListener());
        treeViewer.addSelectionChangedListener(new UpdateConsoleListener());
        tree.addControlListener(new ControlListener() {

            @Override public void controlResized(ControlEvent e) {
                packColumns();
            }

            @Override public void controlMoved(ControlEvent e) {
            }
        });
        tree.addTreeListener(new TreeListener() {

            @Override public void treeExpanded(TreeEvent e) {
                packColumns();
            }

            @Override public void treeCollapsed(TreeEvent e) {
            }
        });

        cons = new Text(sashForm, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.minimumWidth = CONS_MINWIDTH;
        cons.setLayoutData(gd);

        sashForm.setWeights(new int[] { 40, 60 });

        createActions();
        createFilters();
        initializeMenu();

        reset();

        run = new MultiTestSuiteRun();

        treeViewer.expandAll();

        updateHeader();

    }

    private void packColumns() {
        final Tree tree = treeViewer.getTree();
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                for(TreeColumn c : tree.getColumns()) {
                    c.pack();
                }
            }
        });
    }

    private void updateHeader() {
        int nrTests = run.numTests();
        if(nrTests == 0) {
            lblRatio.setText("0 / 0");
        } else {
            lblRatio.setText(String.format("%d / %d    ", run.numPassed(), nrTests));
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

    public void refresh() {
        updateHeader();
        pb.redraw();
        treeViewer.refresh();
        packColumns();
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

    public void setTestResult(TestCaseRun t, ISpoofaxTestResult res) {
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

    /**
     * A listener that (on a double click event) tries to open the file corresponding to the clicked TestCaseRun or
     * TestSuiteRun.
     */
    private class JumpToFileListener implements IDoubleClickListener {

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
    }

    /**
     * Listener that updates the console part if a TestCaseRun or TestSuiteRun is selected.
     */
    private class UpdateConsoleListener implements ISelectionChangedListener {

        @Override public void selectionChanged(SelectionChangedEvent event) {
            final ISelection generalSel = event.getSelection();
            if(generalSel.isEmpty()) {
                cons.setText("");
                return;
            }
            if(generalSel instanceof ITreeSelection) {
                final Object selObj = ((ITreeSelection) generalSel).getFirstElement();
                if(selObj instanceof TestSuiteRun) {
                    final TestSuiteRun tsr = (TestSuiteRun) selObj;
                    if(tsr.ext == null) {
                        cons.setText("Failed to load the contents of this test suite due to an IO error.");
                    } else if(tsr.ext.isSuccessful()) {
                        cons.setText("");
                    } else {
                        final StringWriter strW = new StringWriter();
                        final PrintWriter pw = new PrintWriter(strW);
                        pw.print("Failed to extract test cases from this test suite:\n");
                        for(IMessage m : tsr.ext.getAllMessages()) {
                            printMessage(m, pw);
                            pw.println();
                        }
                        pw.flush();
                        pw.close();
                        cons.setText(strW.toString());
                    }
                } else if(selObj instanceof TestCaseRun) {
                    final TestCaseRun tcr = (TestCaseRun) selObj;
                    if(tcr.result() == null) {
                        cons.setText(
                            "This test case has not yet been executed.\nPlease select it again when it's done.");
                    } else if(tcr.result().isSuccessful()) {
                        cons.setText("");
                    } else {
                        final StringWriter sw = new StringWriter();
                        final PrintWriter pw = new PrintWriter(sw);
                        pw.println("This test cased failed:");
                        for(IMessage m : tcr.result().getAllMessages()) {
                            printMessage(m, pw);
                            pw.println();
                        }
                        pw.flush();
                        pw.close();
                        cons.setText(sw.toString());
                    }
                }
            }
            return;
        }

        private void printMessage(IMessage m, PrintWriter pw) {
            pw.print(m.severity());
            if(m.region() != null) {
                pw.append(" @ (").append(Integer.toString(m.region().startOffset())).append(", ")
                    .append(Integer.toString(m.region().endOffset())).append(")");
            }
            pw.append(" : ").append(m.message());
            if(m.exception() != null) {
                pw.println();
                m.exception().printStackTrace(pw);
            }
        }
    }
}
