package org.metaborg.spt.cmd;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.*;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.spoofax.core.syntax.JSGLRParserConfiguration;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnitService;
import org.metaborg.spt.core.extract.ISpoofaxTestCaseExtractionResult;
import org.metaborg.spt.core.extract.ISpoofaxTestCaseExtractor;
import org.metaborg.spt.core.run.ISpoofaxFragmentParserConfig;
import org.metaborg.spt.core.run.ISpoofaxTestCaseRunner;
import org.metaborg.spt.core.run.ISpoofaxTestResult;
import org.metaborg.spt.core.run.SpoofaxFragmentParserConfig;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.resource.FileSelectorUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class Runner {
    private static final ILogger logger = LoggerUtils.logger(Runner.class);

    private final IResourceService resourceService;
    private final ISimpleProjectService projectService;
    private final ILanguageService languageService;
    private final ILanguageComponentFactory languageComponentFactory;
    private final ISpoofaxInputUnitService inputService;
    private final ISpoofaxTestCaseExtractor extractor;
    private final ISpoofaxTestCaseRunner executor;


    @Inject
    public Runner(IResourceService resourceService, ISimpleProjectService projectService,
                  ISpoofaxInputUnitService inputService,
                  ILanguageService languageService,
                  ISpoofaxTestCaseExtractor extractor, ISpoofaxTestCaseRunner executor,
                  ILanguageComponentFactory languageComponentFactory) {
        this.resourceService = resourceService;
        this.projectService = projectService;

        this.languageComponentFactory = languageComponentFactory;
        this.languageService = languageService;
        this.inputService = inputService;
        this.extractor = extractor;
        this.executor = executor;
    }


    public void run(String sptPath, String lutPath, List<String> languagePaths, String testsPath, String startSymbol)
            throws MetaborgException, FileSystemException {

        final FileObject testsLocation = resourceService.resolve(testsPath);
        if (!testsLocation.exists()) {
            throw new IllegalArgumentException("The folder with tests " + testsPath + " does not exist");
        }
        final IProject project = projectService.create(testsLocation);
        try {
            // get SPT
            final ILanguageImpl spt = getLanguageImplFromPath("SPT language", sptPath);
            // get LUT
            final ILanguageImpl lut = getLanguageImplFromPath("language under test", lutPath);
            // load any extra languages
            for (String languagePath : languagePaths) {
                loadLanguagesFromPath("extra languages", languagePath);
            }
            // process start symbol
            ISpoofaxFragmentParserConfig fragmentConfig =
                    startSymbol == null ? null : new SpoofaxFragmentParserConfig();
            if (fragmentConfig != null) {
                fragmentConfig.putConfig(lut, new JSGLRParserConfiguration(startSymbol));
            }

            for (FileObject testSuite : project.location().findFiles(FileSelectorUtils.extension("spt"))) {
                logger.info("Processing test suite {}", testSuite);
                final String text;
                try (InputStream in = testSuite.getContent().getInputStream()) {
                    text = IOUtils.toString(in);
                } catch (IOException e) {
                    logger.error("Unable to process file {}", e, testSuite);
                    continue;
                }
                ISpoofaxInputUnit input = inputService.inputUnit(testSuite, text, spt, null);
                ISpoofaxTestCaseExtractionResult extractionResult = extractor.extract(input, project);

                // use the start symbol of the test suite if no overriding start symbol has been given to this method
                ISpoofaxFragmentParserConfig moduleFragmentConfig = fragmentConfig;
                if (extractionResult.getStartSymbol() != null && moduleFragmentConfig == null) {
                    moduleFragmentConfig = new SpoofaxFragmentParserConfig();
                    moduleFragmentConfig.putConfig(lut,
                            new JSGLRParserConfiguration(extractionResult.getStartSymbol()));
                }

                if (extractionResult.isSuccessful()) {
                    Iterable<ITestCase> tests = extractionResult.getTests();
                    logger.debug("Using the following start symbol for this suite: {}", moduleFragmentConfig == null
                            ? null : moduleFragmentConfig.getParserConfigForLanguage(lut).overridingStartSymbol);
                    for (ITestCase test : tests) {
                        logger.info("Running test '{}' of suite {}.", test.getDescription(), testSuite);
                        ISpoofaxTestResult res = executor.run(project, test, lut, null, moduleFragmentConfig);
                        logger.info("Test passed: {}", res.isSuccessful());
                        for (IMessage m : res.getAllMessages()) {
                            if (m.region() == null) {
                                logger.info("\t{} : {}", m.severity(), m.message());
                            } else {
                                logger.info("\t@({}, {}) {} : {}", m.region().startOffset(), m.region().endOffset(),
                                        m.severity(), m.message());
                            }
                        }
                    }
                } else {
                    logger.error("Failed to run tests at {}. Extraction of tests failed.", testSuite);
                }

                for (IMessage m : extractionResult.getAllMessages()) {
                    if (m.region() == null) {
                        logger.info("\t{} : {}", m.severity(), m.message());
                    } else {
                        logger.info("\t@({}, {}) {} : {}", m.region().startOffset(), m.region().endOffset(),
                                m.severity(), m.message());
                    }
                }
            }
        } finally {
            projectService.remove(project);
        }
    }

    private ILanguageImpl getLanguageImplFromPath(String name, String path) throws FileSystemException, MetaborgException {
        Collection<ILanguageComponent> components = loadLanguagesFromPath(name, path);

        // FIXME: Will this always pick the right language? Can the iterator ever be empty?
        return LanguageUtils.toImpls(components).iterator().next();
    }

    private Collection<ILanguageComponent> loadLanguagesFromPath(String name, String path) throws FileSystemException, MetaborgException {
        Collection<ComponentCreationConfig> configs = getComponentConfigsFromPath(name, path);

        final Collection<ILanguageComponent> components = Lists.newArrayList();
        for (ComponentCreationConfig config : configs) {
            ILanguageComponent component = languageService.add(config);
            components.add(component);
        }
        return components;
    }

    private Collection<ComponentCreationConfig> getComponentConfigsFromPath(String name, String path) throws FileSystemException, MetaborgException {
        final FileObject location = resourceService.resolve(path);
        if (!location.exists()) {
            throw new IllegalArgumentException("The location for " + name + " does not exist: " + path);
        }

        Collection<IComponentCreationConfigRequest> requests = languageComponentFactory.requestAllInDirectory(location);
        Collection<IComponentCreationConfigRequest> validRequests = requests.stream()
                .filter(IComponentCreationConfigRequest::valid)
                .collect(Collectors.toList());

        if (validRequests.isEmpty()) {
            throw new IllegalArgumentException("The location for " + name + " contains no (valid) languages: " + path);
        }
        return languageComponentFactory.createConfigs(validRequests);
    }

}