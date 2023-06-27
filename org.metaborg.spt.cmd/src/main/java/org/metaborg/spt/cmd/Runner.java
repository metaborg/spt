package org.metaborg.spt.cmd;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ComponentCreationConfig;
import org.metaborg.core.language.IComponentCreationConfigRequest;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageComponentFactory;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.language.LanguageUtils;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.testing.ITestReporterService;
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
import org.metaborg.util.log.Level;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.resource.FileSelectorUtils;

import com.google.common.collect.Lists;
import javax.inject.Inject;

public class Runner {
    private static final ILogger logger = LoggerUtils.logger(Runner.class);

    private final IResourceService resourceService;
    private final ISimpleProjectService projectService;
    private final ILanguageService languageService;
    private final ILanguageComponentFactory languageComponentFactory;
    private final ISpoofaxInputUnitService inputService;
    private final ISpoofaxTestCaseExtractor extractor;
    private final ISpoofaxTestCaseRunner executor;
    private final ITestReporterService testReporter;


    @Inject
    public Runner(IResourceService resourceService, ISimpleProjectService projectService,
                  ISpoofaxInputUnitService inputService, ITestReporterService testReporter,
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
        this.testReporter = testReporter;
    }


    public void run(String sptPath, String lutPath, List<String> languagePaths, String testsPath, String startSymbol)
            throws MetaborgException, FileSystemException {

        testReporter.sessionStarted();

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
                final String text;
                try (InputStream in = testSuite.getContent().getInputStream()) {
                    text = IOUtils.toString(in, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    testReporter.getLogger().error("Unable to process file {}", e, testSuite);
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
                    String testSuiteName = extractionResult.getName();
                    testReporter.testSuiteStarted(testSuiteName);
                    Iterable<ITestCase> tests = extractionResult.getTests();
                    logger.debug("Using the following start symbol for this suite: {}", moduleFragmentConfig == null
                            ? null : moduleFragmentConfig.getParserConfigForLanguage(lut).overridingStartSymbol);
                    for (ITestCase test : tests) {
                        String testName = test.getDescription();
                        testReporter.testStarted(testName);
                        ISpoofaxTestResult res = executor.run(project, test, lut, null, moduleFragmentConfig);
                        if (res.isSuccessful()) {
                            for (IMessage m : res.getAllMessages()) {
                                logMessage(m);
                            }
                            testReporter.testPassed(testName);
                        } else {
                            StringBuilder details = new StringBuilder();
                            IMessage firstMessage = null;
                            for (IMessage m : res.getAllMessages()) {
                                if (firstMessage == null) {
                                    firstMessage = m;
                                } else {
                                    details.append(formatMessage(m));
                                }
                            }
                            String failureReason = firstMessage != null ? formatMessage(firstMessage) : "Test failed.";
                            testReporter.testFailed(testName, failureReason, details.toString());
                        }
                    }
                    testReporter.testSuiteFinished(testSuiteName);
                } else {
                    testReporter.getLogger().error("Failed to run tests at {}. Extraction of tests failed.", testSuite);
                }

                for (IMessage m : extractionResult.getAllMessages()) {
                    logMessage(m);
                }
            }
        } finally {
            projectService.remove(project);
            testReporter.sessionFinished();
        }
    }

    private void logMessage(IMessage m) {
        testReporter.getLogger().log(getMessageLevel(m), formatMessage(m));
    }

    private String formatMessage(IMessage m) {
        if (m.region() == null) {
            return String.format("%s", m.message());
        } else {
            return String.format("@(%d, %d) %s", m.region().startOffset(), m.region().endOffset(), m.message());
        }
    }

    private Level getMessageLevel(IMessage m) {
        switch (m.severity()) {
            case WARNING:
                return Level.Warn;
            case ERROR:
                return Level.Error;
            case NOTE:
            default:
                return Level.Info;
        }
    }

    private ILanguageImpl getLanguageImplFromPath(String name, String path) throws FileSystemException, MetaborgException {
        Collection<ILanguageComponent> components = loadLanguagesFromPath(name, path);

        Set<ILanguageImpl> languages = LanguageUtils.toImpls(components);
        if (languages.size() > 1) {
            throw new IllegalArgumentException("Found more than one language for " + name + " at: " + path);
        } else if (languages.isEmpty()) {
            throw new IllegalArgumentException("Found no language for " + name + " at: " + path);
        }
        return languages.iterator().next();
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

        Collection<IComponentCreationConfigRequest> requests;
        if (location.isFile()) {
            // Hopefully a language artifact.
            requests = Lists.newArrayList(languageComponentFactory.requestFromArchive(location));
        } else {
            // Directory hopefully contains some languages.
            requests = languageComponentFactory.requestAllInDirectory(location);
        }

        Collection<IComponentCreationConfigRequest> validRequests = requests.stream()
                .filter(IComponentCreationConfigRequest::valid)
                .collect(Collectors.toList());

        if (validRequests.isEmpty()) {
            throw new IllegalArgumentException("The location for " + name + " contains no (valid) languages: " + path);
        }
        return languageComponentFactory.createConfigs(validRequests);
    }

}