package org.metaborg.spt.testrunner.eclipse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.syntax.JSGLRParserConfiguration;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnitService;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spt.core.SPTModule;
import org.metaborg.spt.core.extract.ISpoofaxTestCaseExtractionResult;
import org.metaborg.spt.core.extract.ISpoofaxTestCaseExtractor;
import org.metaborg.spt.core.run.ISpoofaxFragmentParserConfig;
import org.metaborg.spt.core.run.ISpoofaxTestCaseRunner;
import org.metaborg.spt.core.run.ISpoofaxTestResult;
import org.metaborg.spt.core.run.SpoofaxFragmentParserConfig;
import org.metaborg.spt.testrunner.eclipse.model.MultiTestSuiteRun;
import org.metaborg.spt.testrunner.eclipse.model.TestCaseRun;
import org.metaborg.spt.testrunner.eclipse.model.TestSuiteRun;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.resource.FileSelectorUtils;

import com.google.inject.Injector;

/**
 * A simple test runner for SPT tests in Eclipse.
 */
public class TestRunner {

    public static final String SPT_EXT = "spt";

    private static final ILogger logger = LoggerUtils.logger(TestRunner.class);

    /**
     * Runs all tests collected for the given FileObjects.
     * 
     * It uses {@link FileObject#findFiles(org.apache.commons.vfs2.FileSelector)} to get all SPT files if the given
     * FileObject isn't a file.
     */
    public void runAll(FileObject... fobs) {
        final Spoofax spoofax = SpoofaxPlugin.spoofax();
        final ILanguageService langService = spoofax.languageService;
        final ISpoofaxInputUnitService inputService = spoofax.injector.getInstance(ISpoofaxInputUnitService.class);
        final IProjectService projectService = spoofax.projectService;

        final Injector injector = spoofax.injector.createChildInjector(new SPTModule());
        final ISpoofaxTestCaseExtractor extractor = injector.getInstance(ISpoofaxTestCaseExtractor.class);
        final ISpoofaxTestCaseRunner runner = injector.getInstance(ISpoofaxTestCaseRunner.class);

        // get the SPT language
        final ILanguage sptLang = langService.getLanguage("SPT");
        final ILanguageImpl spt = sptLang == null ? null : sptLang.activeImpl();
        if(spt == null) {
            logger.warn("Unable to obtain SPT for running tests");
            return;
        }

        // get the projects
        final IProject[] projects = new IProject[fobs.length];
        for(int i = 0; i < fobs.length; i++) {
            final FileObject fob = fobs[i];
            final IProject project = projectService.get(fob);
            if(project != null) {
                projects[i] = project;
            } else {
                logger.warn("Unable to obtain the project for: {}", fob);
                return;
            }
        }

        // collect all test suites for each given FileObject
        final List<List<FileObject>> allSuites = new ArrayList<>();
        for(FileObject fob : fobs) {
            final List<FileObject> suites = new ArrayList<>();
            allSuites.add(suites);
            try {
                if(fob.isFile()) {
                    suites.add(fob);
                } else {
                    for(FileObject suite : fob.findFiles(FileSelectorUtils.extension(SPT_EXT))) {
                        suites.add(suite);
                    }
                }
            } catch(FileSystemException e1) {
                logger.warn("Couldn't get the test suites of project {}", e1, fob);
                return;
            }
        }

        // get the testrunner view and prepare for this test run
        resetView();

        // create the data model for this test run
        final MultiTestSuiteRun run = new MultiTestSuiteRun();

        // start adding the test suites that we found to the object model
        for(int i = 0; i < allSuites.size(); i++) {
            final List<FileObject> suites = allSuites.get(i);
            final IProject project = projects[i];
            for(FileObject suite : suites) {
                // may be null
                final ISpoofaxTestCaseExtractionResult ext = extractSuite(suite, project, spt, inputService, extractor);
                final TestSuiteRun tsr;
                if(ext == null) {
                    // record the failed extraction
                    logger.error("Test suite {} failed to extract properly.", suite.getName());
                    tsr = new TestSuiteRun(ext, project, suite, run, suite.getName().getBaseName());
                } else {
                    if(ext.isSuccessful()) {
                        // the test suite was properly extracted
                        tsr = new TestSuiteRun(ext, project, suite, run, ext.getName());
                    } else {
                        // record the failed extraction
                        logger.error("Test suite {} failed to extract properly.", suite.getName());
                        for(IMessage m : ext.getAllMessages()) {
                            logger.error("[{}] :: ({},{}) :: {}", m.severity(),
                                m.region() == null ? null : m.region().startOffset(),
                                m.region() == null ? null : m.region().endOffset(), m.message());
                        }
                        tsr = new TestSuiteRun(ext, project, suite, run, suite.getName().getBaseName());
                    }
                }
                run.suites.add(tsr);

                // get and add the test cases to the suite
                if(ext != null && ext.isSuccessful()) {
                    for(ITestCase test : ext.getTests()) {
                        final TestCaseRun tcr = new TestCaseRun(tsr, test);
                        tsr.tests.add(tcr);
                    }
                }

                // load the data in the view
                setData(run);
            }
        }

        logger.debug("Loaded all tests into the view Java model");

        // run the tests
        for(TestSuiteRun tsr : run.suites) {
            // may be null
            final ISpoofaxTestCaseExtractionResult ext = tsr.ext;
            if(ext != null && ext.isSuccessful()) {
                // get lut for the test suite
                final String lutStr = ext.getLanguage();
                if(lutStr == null) {
                    logger.warn("Can't execute tests of suite {}, as there is no language header.", tsr.name);
                    continue;
                }
                final ILanguage lutLang = langService.getLanguage(lutStr);
                if(lutLang == null) {
                    logger.warn("Can't execute tests of suite {}, as the language {} couldn't be loaded.", tsr.name,
                        lutStr);
                    continue;
                }
                final ILanguageImpl lut = lutLang.activeImpl();

                // get start symbol for the test suite
                final String symbol = ext.getStartSymbol();
                final ISpoofaxFragmentParserConfig cfg;
                if(symbol == null) {
                    cfg = null;
                } else {
                    cfg = new SpoofaxFragmentParserConfig();
                    cfg.putConfig(lut, new JSGLRParserConfiguration(symbol));
                }

                // run the test cases
                final IProject project = tsr.project;
                for(TestCaseRun tcr : tsr.tests) {
                    tcr.start();
                    final ISpoofaxTestResult result = runner.run(project, tcr.test, lut, null, cfg);
                    finish(tcr, result);
                }
            }
        }

    }

    private ISpoofaxTestCaseExtractionResult extractSuite(FileObject testSuite, IProject project, ILanguageImpl spt,
        ISpoofaxInputUnitService inputService, ISpoofaxTestCaseExtractor extractor) {
        final String text;
        try(InputStream in = testSuite.getContent().getInputStream()) {
            text = IOUtils.toString(in, (String) null);
        } catch(IOException e) {
            logger.error("Unable to process file {}", e, testSuite);
            return null;
        }
        final ISpoofaxInputUnit input = inputService.inputUnit(testSuite, text, spt, null);
        final ISpoofaxTestCaseExtractionResult extractionResult = extractor.extract(input, project);
        return extractionResult;
    }

    private TestRunViewPart getViewPart() {
        try {
            TestRunViewPart v = (TestRunViewPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                .showView(TestRunViewPart.VIEW_ID);
            return v;
        } catch(PartInitException e) {
            Activator.logError("Could not open view", e);
            return null;
        }

    }

    // reset the view
    private void resetView() {
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                TestRunViewPart vp = getViewPart();
                if(vp != null) {
                    vp.reset();
                }
            }
        });

    }

    private void setData(final MultiTestSuiteRun run) {
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                final TestRunViewPart vp = getViewPart();
                if(vp != null) {
                    vp.setData(run);
                }
            }
        });
    }

    private void finish(final TestCaseRun t, final ISpoofaxTestResult res) {
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                final TestRunViewPart vp = getViewPart();
                if(vp != null) {
                    vp.finish(t, res);
                }
            }
        });
    }

}
