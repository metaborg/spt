package org.metaborg.meta.lang.spt.testrunner.cmd;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.spoofax.meta.spt.testrunner.core.TestRunner;

import com.google.inject.Inject;

public class Runner {
    private final IResourceService resourceService;
    private final ISimpleProjectService projectService;
    private final TestRunner runner;


    @Inject public Runner(IResourceService resourceService, ISimpleProjectService projectService, TestRunner runner) {
        this.resourceService = resourceService;
        this.projectService = projectService;
        this.runner = runner;
    }


    public void run(String languagePath, String testsPath) throws MetaborgException, FileSystemException {
        final FileObject languageLocation = resourceService.resolve(languagePath);
        final FileObject testsLocation = resourceService.resolve(testsPath);
        final IProject project = projectService.create(testsLocation);
        try {
            runner.discoverLanguages(languageLocation);
            runner.run(project);
        } finally {
            projectService.remove(project);
        }
    }
}
