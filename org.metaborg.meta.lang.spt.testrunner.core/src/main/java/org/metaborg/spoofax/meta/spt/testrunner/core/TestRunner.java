package org.metaborg.spoofax.meta.spt.testrunner.core;

import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.analysis.AnalysisFileResult;
import org.metaborg.core.analysis.AnalysisResult;
import org.metaborg.core.build.BuildInput;
import org.metaborg.core.build.ConsoleBuildMessagePrinter;
import org.metaborg.core.build.IBuildOutput;
import org.metaborg.core.build.NewBuildInputBuilder;
import org.metaborg.core.build.dependency.INewDependencyService;
import org.metaborg.core.build.paths.INewLanguagePathService;
import org.metaborg.core.language.*;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.project.ILanguageSpec;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.source.ISourceTextService;
import org.metaborg.spoofax.core.processing.ISpoofaxProcessorRunner;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class TestRunner {
    private static final ILogger logger = LoggerUtils.logger(TestRunner.class);

    private final IResourceService resourceService;
    private final INewLanguageDiscoveryService languageDiscoveryService;
    private final ILanguageIdentifierService languageIdentifierService;
    private final INewDependencyService dependencyService;
    private final INewLanguagePathService languagePathService;
    private final ISpoofaxProcessorRunner runner;
    private final ISourceTextService sourceTextService;

    private final Collection<ILanguageComponent> components = Lists.newLinkedList();

    private ILanguageImpl sptLanguage = null;


    @Inject public TestRunner(IResourceService resourceService, INewLanguageDiscoveryService languageDiscoveryService,
        ILanguageIdentifierService languageIdentifierService, INewDependencyService dependencyService,
        INewLanguagePathService languagePathService, ISpoofaxProcessorRunner runner, ISourceTextService sourceTextService) {
        this.resourceService = resourceService;
        this.languageDiscoveryService = languageDiscoveryService;
        this.languageIdentifierService = languageIdentifierService;
        this.dependencyService = dependencyService;
        this.languagePathService = languagePathService;
        this.runner = runner;
        this.sourceTextService = sourceTextService;
    }


    public Iterable<ILanguageComponent> discoverLanguages(FileObject location) throws MetaborgException {
        final Iterable<ILanguageComponent> components = languageDiscoveryService.discover(languageDiscoveryService.request(location));
        Iterables.addAll(this.components, components);
        return components;
    }


    public void run(ILanguageSpec languageSpec) throws MetaborgException, FileSystemException {
        final ILanguageImpl sptLanguage = discoverSPT();
        final FileObject location = languageSpec.location();
        final FileObject[] sptFiles =
            location.findFiles(new LanguageFileSelector(languageIdentifierService, sptLanguage));
        runTests(languageSpec, Iterables2.from(sptFiles));
    }

    public void run(ILanguageSpec languageSpec, Iterable<FileObject> tests) throws MetaborgException {
        discoverSPT();
        runTests(languageSpec, tests);
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

    private void runTests(ILanguageSpec languageSpec, Iterable<FileObject> tests) throws MetaborgException {
        final NewBuildInputBuilder builder = new NewBuildInputBuilder(languageSpec);
        // @formatter:off
        final BuildInput input = builder
            .withComponents(components)
            .withDefaultIncludePaths(false)
            .withSources(tests)
            .withTransformation(false)
//            .addTransformGoal(new NamedGoal("testrunnerfile"))
            .withMessagePrinter(new ConsoleBuildMessagePrinter(sourceTextService, true, true, logger))
            .build(dependencyService, languagePathService)
            ;
        // @formatter:on

        try {
            IBuildOutput<IStrategoTerm, IStrategoTerm, IStrategoTerm> bRes = runner.build(input, null, null).schedule().block().result();
            for (AnalysisResult<IStrategoTerm, IStrategoTerm> aRes : bRes.analysisResults()) {
            	for (AnalysisFileResult<IStrategoTerm, IStrategoTerm> fRes : aRes.fileResults) {
            		logger.info("Analysis results for file {}", fRes.source.getName());
            		for (IMessage message : fRes.messages) {
            			logger.info("{}: {} || ({})", message.severity(), message.message(), message.region());
            		}
            	}
            }
        } catch(InterruptedException e) {
            // Ignore interruption
        }
    }
}
