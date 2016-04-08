package org.metaborg.spt.cmd;

import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageDiscoveryService;
import org.metaborg.core.language.ILanguageIdentifierService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.LanguageFileSelector;
import org.metaborg.core.language.LanguageUtils;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.spt.core.ITestCase;
import org.metaborg.spt.core.ITestCaseExtractionResult;
import org.metaborg.spt.core.ITestCaseExtractor;
import org.metaborg.spt.core.ITestCaseRunner;
import org.metaborg.spt.core.ITestResult;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class Runner {
    private static final ILogger logger = LoggerUtils.logger(Runner.class);

    private final IResourceService resourceService;
    private final ISimpleProjectService projectService;
    private final ILanguageDiscoveryService languageDiscoveryService;
    private final ILanguageIdentifierService langIdentService;
    private final ITestCaseExtractor extractor;
    private final ITestCaseRunner executor;


    @Inject public Runner(IResourceService resourceService, ISimpleProjectService projectService,
        ILanguageDiscoveryService languageDiscoveryService, ILanguageIdentifierService langIdentService,
        ITestCaseExtractor extractor, ITestCaseRunner executor) {
        this.resourceService = resourceService;
        this.projectService = projectService;

        this.languageDiscoveryService = languageDiscoveryService;
        this.langIdentService = langIdentService;
        this.extractor = extractor;
        this.executor = executor;
    }


    public void run(String sptPath, String lutPath, List<String> languagePaths, String testsPath)
        throws MetaborgException, FileSystemException {
        final FileObject sptLocation = resourceService.resolve(sptPath);
        final FileObject lutLocation = resourceService.resolve(lutPath);
        final List<FileObject> languageLocations = Lists.newLinkedList();
        for(String languagePath : languagePaths) {
            languageLocations.add(resourceService.resolve(languagePath));
        }
        final FileObject testsLocation = resourceService.resolve(testsPath);
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

            for(FileObject testSuite : project.location().findFiles(new LanguageFileSelector(langIdentService, spt))) {
                ITestCaseExtractionResult extractionResult = extractor.extract(spt, project, testSuite);
                if(extractionResult.isSuccessful()) {
                    Iterable<ITestCase> tests = extractionResult.getTests();
                    for(ITestCase test : tests) {
                        logger.info("Running test '{}' of suite {}.", test.getDescription(), testSuite);
                        ITestResult res = executor.run(project, test, lut, null);
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
                    logger.error("Failed to run tests at {}. Extraction of tests failed.", testsPath);
                    for(IMessage m : extractionResult.getAllMessages()) {
                        if(m.region() == null) {
                            logger.info("\t{} : {}", m.severity(), m.message());
                        } else {
                            logger.info("\t@({}, {}) {} : {}", m.region().startOffset(), m.region().endOffset(),
                                m.severity(), m.message());
                        }
                    }
                }
            }
        } finally {
            projectService.remove(project);
        }
    }
}
