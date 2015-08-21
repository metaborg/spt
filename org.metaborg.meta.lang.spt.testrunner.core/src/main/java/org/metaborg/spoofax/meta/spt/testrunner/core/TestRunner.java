package org.metaborg.spoofax.meta.spt.testrunner.core;

import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.build.BuildInput;
import org.metaborg.core.build.BuildInputBuilder;
import org.metaborg.core.build.dependency.IDependencyService;
import org.metaborg.core.build.paths.ILanguagePathService;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageDiscoveryService;
import org.metaborg.core.language.ILanguageIdentifierService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.LanguageFileSelector;
import org.metaborg.core.language.LanguageUtils;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.transform.NamedGoal;
import org.metaborg.spoofax.core.processing.ISpoofaxProcessorRunner;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class TestRunner {
    private static final ILogger logger = LoggerUtils.logger(TestRunner.class);

    private final IResourceService resourceService;
    private final ILanguageDiscoveryService languageDiscoveryService;
    private final ILanguageIdentifierService languageIdentifierService;
    private final IProjectService projectService;
    private final IDependencyService dependencyService;
    private final ILanguagePathService languagePathService;
    private final ISpoofaxProcessorRunner runner;

    private final Collection<ILanguageComponent> components = Lists.newLinkedList();

    private ILanguageImpl sptLanguage = null;


    @Inject public TestRunner(IResourceService resourceService, ILanguageDiscoveryService languageDiscoveryService,
        ILanguageIdentifierService languageIdentifierService, IProjectService projectService,
        IDependencyService dependencyService, ILanguagePathService languagePathService, ISpoofaxProcessorRunner runner) {
        this.resourceService = resourceService;
        this.languageDiscoveryService = languageDiscoveryService;
        this.languageIdentifierService = languageIdentifierService;
        this.projectService = projectService;
        this.dependencyService = dependencyService;
        this.languagePathService = languagePathService;
        this.runner = runner;
    }


    public Iterable<ILanguageComponent> discoverLanguages(FileObject location) throws MetaborgException {
        final Iterable<ILanguageComponent> components = languageDiscoveryService.discover(location);
        Iterables.addAll(this.components, components);
        return components;
    }


    public void run(FileObject location) throws MetaborgException, FileSystemException {
        final ILanguageImpl sptLanguage = discoverSPT();
        final FileObject[] sptFiles =
            location.findFiles(new LanguageFileSelector(languageIdentifierService, sptLanguage));
        runTests(location, Iterables2.from(sptFiles));
    }

    public void run(FileObject location, Iterable<FileObject> tests) throws MetaborgException {
        discoverSPT();
        runTests(location, tests);
    }


    private ILanguageImpl discoverSPT() throws MetaborgException {
        if(sptLanguage != null) {
            return sptLanguage;
        }

        logger.debug("Discovering SPT language");
        final FileObject sptLocation = resourceService.resolve("res:spt");
        final Iterable<ILanguageComponent> components = discoverLanguages(sptLocation);
        final Iterable<ILanguageImpl> impls = LanguageUtils.toImpls(components);
        final int size = Iterables.size(impls);
        if(size == 0) {
            throw new MetaborgException("Failed to discover SPT language implementation");
        } else if(size > 1) {
            throw new MetaborgException("Discovered multiple SPT language implementations, expected one");
        } else {
            return Iterables.get(impls, 0);
        }
    }

    private void runTests(FileObject location, Iterable<FileObject> tests) throws MetaborgException {
        try {
            if(!location.exists()) {
                throw new MetaborgException("Location does not exist");
            }
        } catch(FileSystemException e) {
            throw new MetaborgException("Checking if location exists failed unexpectedly", e);
        }

        final IProject project = projectService.get(location);
        if(project == null) {
            throw new MetaborgException("Could not retrieve project");
        }

        final BuildInputBuilder builder = new BuildInputBuilder(project);
        // @formatter:off
        final BuildInput input = builder
            .withComponents(components)
            .withDefaultIncludePaths(false)
            .withSources(tests)
            .withTransformation(false)
            .addTransformGoal(new NamedGoal("testrunnerfile"))
            .build(dependencyService, languagePathService)
            ;
        // @formatter:on

        try {
            runner.build(input, null, null).schedule().block();
        } catch(InterruptedException e) {
            // Ignore interruption
        }
    }
}
