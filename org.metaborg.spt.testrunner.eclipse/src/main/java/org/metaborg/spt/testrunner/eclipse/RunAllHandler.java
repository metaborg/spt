package org.metaborg.spt.testrunner.eclipse;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.ui.handlers.HandlerUtil;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;

/**
 * The handler for the 'Run All SPT tests' command that is executed from a project's Spoofax (meta) context menu. Or
 * from the Spoofax (meta) menu bar entry.
 */
public class RunAllHandler extends AbstractHandler {
    protected static TestRunner runner = null;

    @Override public Object execute(ExecutionEvent event) throws ExecutionException {
        if(runner == null) {
            runner = new TestRunner();
        }

        // will contain all selected locations from which we should collect tests
        final List<FileObject> testLocations = new ArrayList<>();

        final ISelection sel = HandlerUtil.getCurrentSelectionChecked(event);
        // the package explorer returns a tree selection
        if(sel instanceof ITreeSelection) {
            // we need this to resolve to VFS files that Spoofax can handle
            final IEclipseResourceService resolver =
                SpoofaxPlugin.injector().getInstance(IEclipseResourceService.class);

            final ITreeSelection tree = (ITreeSelection) sel;
            for(TreePath p : tree.getPaths()) {
                // get the last segment of the selection (i.e. we only care about the actually selected element)
                final Object selectedObject = p.getLastSegment();
                final IResource resource;

                if(selectedObject instanceof IResource) {
                    // IFiles and (non-java) IProjects are IResources
                    resource = (IResource) selectedObject;
                } else if(selectedObject instanceof IJavaProject) {
                    // for Java projects we need to be more creative
                    resource = ((IJavaProject) selectedObject).getProject();
                } else {
                    resource = null;
                    Activator.logWarn("SPT Run can't handle a selection of type " + selectedObject.getClass()
                        + ". Object: " + selectedObject);
                }
                if(resource != null) {
                    final FileObject file = resolver.resolve(resource);
                    if(file != null) {
                        testLocations.add(file);
                    } else {
                        // just skip it
                        Activator.logWarn("SPT could not resolve the selected resource " + resource.getName());
                    }
                }
            }

        } else {
            Activator.logError(
                "SPT could not find a selected project, directory, or file to run tests from. Instead, we got " + sel);
        }

        if(!testLocations.isEmpty()) {
            // run all tests of these locations
            RunTestsJob job = new RunTestsJob(testLocations);
            job.setRule(ResourcesPlugin.getWorkspace().getRoot());
            job.schedule();
        } else {
            // If we have no location to run on (i.e. selected projects) and no input to run on (i.e. open file in
            // editor) we don't know what to do
            Activator.logError("SPT Run had no locations to execute on.");
        }

        return null;
    }

    private class RunTestsJob extends Job {

        private final List<FileObject> testLocations;

        public RunTestsJob(List<FileObject> testLocations) {
            super(RunTestsJob.class.getSimpleName());
            this.testLocations = testLocations;
        }

        @Override protected IStatus run(IProgressMonitor monitor) {
            runner.runAll(testLocations.toArray(new FileObject[testLocations.size()]));
            return Status.OK_STATUS;
        }
    }



}
