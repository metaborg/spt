package org.metaborg.spt.testrunner.eclipse;

import java.net.URI;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.ui.handlers.HandlerUtil;

import com.google.common.collect.Lists;

/**
 * The handler for the 'Run All SPT tests' command that is executed from a project's Spoofax context menu.
 */
public class RunAllHandler extends AbstractHandler {
    protected static TestRunner runner = null;

    @Override public Object execute(ExecutionEvent event) throws ExecutionException {
        if(runner == null) {
            runner = new TestRunner();
        }

        final ISelection sel = HandlerUtil.getCurrentSelectionChecked(event);
        if(sel instanceof ITreeSelection) {
            final ITreeSelection tree = (ITreeSelection) sel;
            final List<URI> testLocations = Lists.newArrayList();
            for(TreePath p : tree.getPaths()) {
                for(int i = 0; i < p.getSegmentCount(); i++) {
                    final Object seg = p.getSegment(i);
                    if(seg instanceof IContainer) {
                        final IContainer c = (IContainer) seg;
                        final URI uri = c.getLocationURI();
                        testLocations.add(uri);
                    } else if(seg instanceof IJavaProject) {
                        final IContainer c = ((IJavaProject) seg).getProject();
                        final URI uri = c.getLocationURI();
                        testLocations.add(uri);
                    } else {
                        Activator.logError("SPT Run can't handle a selection of type " + p.getSegment(i).getClass());
                    }
                }
            }
            if(testLocations.isEmpty()) {
                Activator.logError("SPT Run had no locations to execute on.");
            }

            for(URI uri : testLocations) {
                runner.runAll(uri);
            }
        } else {
            Activator.logError("SPT Run can't handle a selection of type " + sel.getClass());
        }
        return null;
    }
}
