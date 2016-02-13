package org.metaborg.meta.lang.spt.testrunner.cmd;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.project.ILanguageSpec;
import org.metaborg.core.project.ILanguageSpecService;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.spoofax.meta.spt.testrunner.core.TestRunner;

import com.google.inject.Inject;

public class Runner {
    private final IResourceService resourceService;
    private final ISimpleProjectService projectService;
    private final ILanguageSpecService languageSpecService;
    private final TestRunner runner;


    @Inject public Runner(IResourceService resourceService, ISimpleProjectService projectService, ILanguageSpecService languageSpecService, TestRunner runner) {
        this.resourceService = resourceService;
        this.projectService = projectService;
        this.languageSpecService = languageSpecService;
        this.runner = runner;
    }


    public void run(String languagePath, String testsPath) throws MetaborgException, FileSystemException {
        final FileObject languageLocation = resourceService.resolve(languagePath);
        final FileObject testsLocation = resourceService.resolve(testsPath);
        final IProject project = projectService.create(testsLocation);
        final ILanguageSpec languageSpec = languageSpecService.get(project);
        try {
            runner.discoverLanguages(languageLocation);
            runner.run(languageSpec);
        } finally {
            projectService.remove(project);
        }
    }
}
