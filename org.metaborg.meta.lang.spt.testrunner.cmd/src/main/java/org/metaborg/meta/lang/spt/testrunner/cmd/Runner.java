package org.metaborg.meta.lang.spt.testrunner.cmd;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.spoofax.meta.spt.testrunner.core.TestRunner;

public class Runner {
    private final IResourceService resourceService;
    private final TestRunner runner;


    public Runner(IResourceService resourceService, TestRunner runner) {
        this.resourceService = resourceService;
        this.runner = runner;
    }


    public void run(String languagePath, String testsPath) throws MetaborgException, FileSystemException {
        final FileObject languageLocation = resourceService.resolve(languagePath);
        final FileObject testsLocation = resourceService.resolve(testsPath);

        runner.discoverLanguages(languageLocation);
        runner.run(testsLocation);
    }
}
