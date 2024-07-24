package org.metaborg.spt.core;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.FileTypeSelector;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.project.IProject;
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
import mb.util.vfs2.resource.FileSelectorUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SPTRunner {
    private static final ILogger logger = LoggerUtils.logger(SPTRunner.class);

    private final ISpoofaxInputUnitService unitService;
    private final ISpoofaxTestCaseExtractor extractor;
    private final ISpoofaxTestCaseRunner runner;


    @jakarta.inject.Inject public SPTRunner(ISpoofaxInputUnitService unitService, ISpoofaxTestCaseExtractor extractor,
        ISpoofaxTestCaseRunner runner) {
        this.unitService = unitService;
        this.extractor = extractor;
        this.runner = runner;
    }


    public void test(IProject project, ILanguageImpl sptLang, ILanguageImpl testLang) throws MetaborgException {
        final FileObject[] sptFiles;
        try {
            sptFiles = project.location().findFiles(
                FileSelectorUtils.and(FileSelectorUtils.extension("spt"), new FileTypeSelector(FileType.FILE)));
        } catch(FileSystemException e) {
            throw new MetaborgException("Finding SPT test files failed unexpectedly", e);
        }

        if(sptFiles == null || sptFiles.length == 0) {
            return;
        }

        int filesTotal = 0;
        int filesFailed = 0;
        int testsTotal = 0;
        int testsFailed = 0;

        for(FileObject testSuite : sptFiles) {
            ++filesTotal;
            logger.info("Processing {}", testSuite);
            final String text;
            try(InputStream in = testSuite.getContent().getInputStream()) {
                text = IOUtils.toString(in, StandardCharsets.UTF_8);
            } catch(IOException e) {
                logger.error("Unable to process {}", e, testSuite);
                continue;
            }
            final ISpoofaxInputUnit testInput = unitService.inputUnit(testSuite, text, sptLang, null);
            final ISpoofaxTestCaseExtractionResult extractionResult = extractor.extract(testInput, project);

            // use the start symbol of the test suite if no overriding start symbol has been given to this method
            ISpoofaxFragmentParserConfig moduleFragmentConfig = null;
            if(extractionResult.getStartSymbol() != null) {
                moduleFragmentConfig = new SpoofaxFragmentParserConfig();
                moduleFragmentConfig.putConfig(testLang,
                    new JSGLRParserConfiguration(extractionResult.getStartSymbol()));
            }

            boolean fileFailed = false;
            if(extractionResult.isSuccessful()) {
                final Iterable<ITestCase> tests = extractionResult.getTests();
                for(ITestCase test : tests) {
                    ++testsTotal;
                    logger.debug("Running test '{}'", test.getDescription());
                    final ISpoofaxTestResult res = runner.run(project, test, testLang, null, moduleFragmentConfig);
                    if(!res.isSuccessful()) {
                        ++testsFailed;
                        fileFailed = true;
                        logger.error("Test '{}' failed", test.getDescription());
                        for(IMessage m : res.getAllMessages()) {
                            if(m.region() == null) {
                                logger.error("  {} : {}", m.severity(), m.message());
                            } else {
                                logger.error("  @({}, {}) {} : {}", m.region().startOffset(),
                                    m.region().endOffset(), m.severity(), m.message());
                            }
                        }
                    }
                }
            } else {
                fileFailed = true;
                final String message = logger.format("Extraction of tests failed for {}", testSuite);
                logger.error(message);
                for(IMessage m : extractionResult.getAllMessages()) {
                    if(m.region() == null) {
                        logger.error("  {} : {}", m.severity(), m.message());
                    } else {
                        logger.error("  @({}, {}) {} : {}", m.region().startOffset(), m.region().endOffset(),
                            m.severity(), m.message());
                    }
                }
            }

            if(fileFailed) {
                ++filesFailed;
            }
        }

        if(filesFailed > 0 || testsFailed > 0) {
            final String message =
                logger.format("Testing failed: {}/{} tests failed ({}/{} files)", testsFailed, testsTotal, filesFailed,
                    filesTotal);
            logger.error(message);
            throw new MetaborgException(message);
        } else {
            logger.info("Testing successful: {} tests succeeded ({} files)", testsTotal, filesTotal);
        }
    }
}
