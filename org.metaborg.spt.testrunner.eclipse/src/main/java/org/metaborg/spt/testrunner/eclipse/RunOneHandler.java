package org.metaborg.spt.testrunner.eclipse;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;

public class RunOneHandler extends AbstractHandler {

    protected static TestRunner runner = null;

    @Override public Object execute(ExecutionEvent event) throws ExecutionException {
        if(runner == null) {
            runner = new TestRunner();
        }

        // get the file that is open in the editor
        // may be null
        final IEditorInput editorInput = HandlerUtil.getActiveEditorInput(event);
        if(editorInput == null) {
            Activator.logError("SPT could not find an open file to run tests from.");
            return null;
        }
        // may be null
        final FileObject file =
            SpoofaxPlugin.injector().getInstance(IEclipseResourceService.class).resolve(editorInput);
        if(file == null) {
            Activator.logError("SPT could not resolve the open file to a VFS file to run tests from.");
            return null;
        }
        if(!TestRunner.SPT_EXT.equals(file.getName().getExtension())) {
            Activator.logError("SPT can't run tests from the file: " + file.getName() + ". It's not an SPT file.");
            return null;
        }

        // run the tests of this file
        RunTestFileJob job = new RunTestFileJob(file);
        job.setRule(ResourcesPlugin.getWorkspace().getRoot());
        job.schedule();

        return null;
    }

    /**
     * A job to run the tests of a given file.
     */
    private class RunTestFileJob extends Job {

        private final FileObject file;

        public RunTestFileJob(FileObject file) {
            super(RunTestFileJob.class.getSimpleName());
            this.file = file;
        }

        @Override protected IStatus run(IProgressMonitor monitor) {
            runner.runAll(file);
            return Status.OK_STATUS;
        }
    }
}
