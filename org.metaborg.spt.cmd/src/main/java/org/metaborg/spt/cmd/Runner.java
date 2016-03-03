package org.metaborg.spt.cmd;

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
import org.metaborg.spt.core.ITestCaseExtractor;
import org.metaborg.spt.core.ITestResult;
import org.metaborg.spt.core.ITestRunner;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.Inject;

public class Runner {
    private static final ILogger logger = LoggerUtils.logger(Runner.class);

    private final IResourceService resourceService;
    private final ISimpleProjectService projectService;
    private final ILanguageDiscoveryService languageDiscoveryService;
    private final ILanguageIdentifierService langIdentService;
    private final ITestCaseExtractor extractor;
    private final ITestRunner executor;


    @Inject public Runner(IResourceService resourceService, ISimpleProjectService projectService,
        ILanguageDiscoveryService languageDiscoveryService, ILanguageIdentifierService langIdentService,
        ITestCaseExtractor extractor, ITestRunner executor) {
        this.resourceService = resourceService;
        this.projectService = projectService;

        this.languageDiscoveryService = languageDiscoveryService;
        this.langIdentService = langIdentService;
        this.extractor = extractor;
        this.executor = executor;
    }


    public void run(String sptPath, String languagePath, String testsPath)
        throws MetaborgException, FileSystemException {
        final FileObject sptLocation = resourceService.resolve(sptPath);
        final FileObject languageLocation = resourceService.resolve(languagePath);
        final FileObject testsLocation = resourceService.resolve(testsPath);
        final IProject project = projectService.create(testsLocation);
        try {
            // get SPT
            Iterable<ILanguageComponent> sptComponents =
                languageDiscoveryService.discover(languageDiscoveryService.request(sptLocation));
            final ILanguageImpl spt = LanguageUtils.toImpls(sptComponents).iterator().next();
            // get LUT
            Iterable<ILanguageComponent> lutComponents =
                languageDiscoveryService.discover(languageDiscoveryService.request(languageLocation));
            final ILanguageImpl lut = LanguageUtils.toImpls(lutComponents).iterator().next();


            for(FileObject testSuite : project.location().findFiles(new LanguageFileSelector(langIdentService, spt))) {
                Iterable<ITestCase> tests = extractor.extract(spt, project, testSuite);
                for(ITestCase test : tests) {
                    logger.info("Running test {} of suite {}.", test, testSuite);
                    ITestResult res = executor.run(project, test, lut, spt);
                    logger.info("Test passed: {}", res.isSuccessful());
                    for(IMessage m : res.getAllMessages()) {
                        logger.info("\t{} : {}", m.severity(), m.message());
                    }
                }
            }
        } finally {
            projectService.remove(project);
        }
    }
}
