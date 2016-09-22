package org.metaborg.spt.cmd;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageDiscoveryService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.LanguageUtils;
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
    private final ILanguageDiscoveryService languageDiscoveryService;
    private final ISpoofaxInputUnitService inputService;
    private final ISpoofaxTestCaseExtractor extractor;
    private final ISpoofaxTestCaseRunner executor;


    @Inject public Runner(IResourceService resourceService, ISimpleProjectService projectService,
        ILanguageDiscoveryService languageDiscoveryService, ISpoofaxInputUnitService inputService,
        ISpoofaxTestCaseExtractor extractor, ISpoofaxTestCaseRunner executor) {
        this.resourceService = resourceService;
        this.projectService = projectService;

        this.languageDiscoveryService = languageDiscoveryService;
        this.inputService = inputService;
        this.extractor = extractor;
        this.executor = executor;
    }


    public void run(String sptPath, String lutPath, List<String> languagePaths, String testsPath, String startSymbol)
        throws MetaborgException, FileSystemException {
        final FileObject sptLocation = resourceService.resolve(sptPath);
        if(!sptLocation.exists()) {
            throw new IllegalArgumentException("The location for SPT does not exist: " + sptPath);
        }
        final FileObject lutLocation = resourceService.resolve(lutPath);
        if(!lutLocation.exists()) {
            throw new IllegalArgumentException("The location for the language under test does not exist: " + lutPath);
        }
        final List<FileObject> languageLocations = Lists.newLinkedList();
        for(String languagePath : languagePaths) {
            FileObject loc = resourceService.resolve(languagePath);
            languageLocations.add(loc);
            if(!loc.exists()) {
                throw new IllegalArgumentException("The location " + languagePath + " does not exist");
            }
        }
        final FileObject testsLocation = resourceService.resolve(testsPath);
        if(!testsLocation.exists()) {
            throw new IllegalArgumentException("The folder with tests " + testsPath + " does not exist");
        }
        final IProject project = projectService.create(testsLocation);
        try {
            // get SPT
            Iterable<ILanguageComponent> sptComponents =
                languageDiscoveryService.discover(languageDiscoveryService.request(sptLocation));
            final ILanguageImpl spt = LanguageUtils.toImpls(sptComponents).iterator().next();
            // get LUT
            Iterable<ILanguageComponent> lutComponents =
                languageDiscoveryService.discover(languageDiscoveryService.request(lutLocation));
            final ILanguageImpl lut = LanguageUtils.toImpls(lutComponents).iterator().next();
            // load any extra languages
            for(FileObject languageLocation : languageLocations) {
                languageDiscoveryService.discover(languageDiscoveryService.request(languageLocation));
            }
            // process start symbol
            ISpoofaxFragmentParserConfig fragmentConfig =
                startSymbol == null ? null : new SpoofaxFragmentParserConfig();
            if(fragmentConfig != null) {
                fragmentConfig.putConfig(lut, new JSGLRParserConfiguration(startSymbol));
            }

            for(FileObject testSuite : project.location().findFiles(FileSelectorUtils.extension("spt"))) {
                logger.info("Processing test suite {}", testSuite);
                final String text;
                try(InputStream in = testSuite.getContent().getInputStream()) {
                    text = IOUtils.toString(in);
                } catch(IOException e) {
                    logger.error("Unable to process file {}", e, testSuite);
                    continue;
                }
                ISpoofaxInputUnit input = inputService.inputUnit(testSuite, text, spt, null);
                ISpoofaxTestCaseExtractionResult extractionResult = extractor.extract(input, project);

                // use the start symbol of the test suite if no overriding start symbol has been given to this method
                ISpoofaxFragmentParserConfig moduleFragmentConfig = fragmentConfig;
                if(extractionResult.getStartSymbol() != null && moduleFragmentConfig == null) {
                    moduleFragmentConfig = new SpoofaxFragmentParserConfig();
                    moduleFragmentConfig.putConfig(lut,
                        new JSGLRParserConfiguration(extractionResult.getStartSymbol()));
                }

                if(extractionResult.isSuccessful()) {
                    Iterable<ITestCase> tests = extractionResult.getTests();
                    logger.debug("Using the following start symbol for this suite: {}", moduleFragmentConfig == null
                        ? null : moduleFragmentConfig.getParserConfigForLanguage(lut).overridingStartSymbol);
                    for(ITestCase test : tests) {
                        logger.info("Running test '{}' of suite {}.", test.getDescription(), testSuite);
                        ISpoofaxTestResult res = executor.run(project, test, lut, null, moduleFragmentConfig);
                        logger.info("Test passed: {}", res.isSuccessful());
                        for(IMessage m : res.getAllMessages()) {
                            if(m.region() == null) {
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

                for(IMessage m : extractionResult.getAllMessages()) {
                    if(m.region() == null) {
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
}
