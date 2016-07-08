package org.metaborg.spt.testrunner.eclipse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

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
import org.metaborg.core.resource.IResourceService;
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

    private static final ILogger logger = LoggerUtils.logger(TestRunner.class);

    /**
     * See {@link #runAll(FileObject)}.
     */
    public void runAll(URI uri) {
        final Spoofax spoofax = SpoofaxPlugin.spoofax();
        final IResourceService resService = spoofax.resourceService;
        final FileObject fob = resService.resolve(uri);
        runAll(fob);
    }

    /**
     * Runs all tests under the given path.
     * 
     * NOTE: for now it only works on projects, but this might be extended later to also support any directory or file.
     */
    public void runAll(FileObject parentDirOrFile) {
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
            logger.warn("Unable to obtain SPT for running tests at: {}. {}", parentDirOrFile, sptLang);
            return;
        }

        // get the project
        final IProject project = projectService.get(parentDirOrFile);
        if(project == null) {
            logger.warn("Unable to obtain the project: {}", parentDirOrFile);
            return;
        }

        // collect all test suites
        FileObject[] allSuites = null;
        try {
            allSuites = parentDirOrFile.findFiles(FileSelectorUtils.extension("spt"));
        } catch(FileSystemException e1) {
            logger.warn("Couldn't get the test suites of project {}", e1, parentDirOrFile);
            return;
        }

        // collect all tests by their suites
        final Map<FileObject, ISpoofaxTestCaseExtractionResult> extractionMap = new HashMap<>();
        for(FileObject testSuite : allSuites) {
            final String text;
            try(InputStream in = testSuite.getContent().getInputStream()) {
                text = IOUtils.toString(in);
            } catch(IOException e) {
                logger.error("Unable to process file {}", e, testSuite);
                continue;
            }
            final ISpoofaxInputUnit input = inputService.inputUnit(testSuite, text, spt, null);
            final ISpoofaxTestCaseExtractionResult extractionResult = extractor.extract(input, project);
            extractionMap.put(testSuite, extractionResult);
        }

        // get the testrunner view and prepare for this test run
        resetView();

        // fill in the data model for this test run
        final MultiTestSuiteRun run = new MultiTestSuiteRun();
        final Map<FileObject, TestSuiteRun> suiteRuns = new HashMap<>();
        final Map<ITestCase, TestCaseRun> caseRuns = new HashMap<>();
        for(FileObject suite : allSuites) {
            final ISpoofaxTestCaseExtractionResult ext = extractionMap.get(suite);
            final String suiteName = ext.getName();
            if(ext.isSuccessful()) {
                final TestSuiteRun tsr = new TestSuiteRun(run, suiteName);
                suiteRuns.put(suite, tsr);
                run.suites.add(tsr);
                for(ITestCase test : ext.getTests()) {
                    final TestCaseRun tcr = new TestCaseRun(tsr, test);
                    caseRuns.put(test, tcr);
                }
            } else {
                // TODO what do we want to do in this case?
                logger.error("Test suite {} failed to extract properly.", ext.getName());
                for(IMessage m : ext.getAllMessages()) {
                    logger.error("[{}] :: ({},{}) :: {}", m.severity(),
                        m.region() == null ? null : m.region().startOffset(),
                        m.region() == null ? null : m.region().endOffset(), m.message());
                }
            }
        }

        // load the data in the view
        setData(run);

        // run the tests
        for(FileObject suite : allSuites) {
            final ISpoofaxTestCaseExtractionResult ext = extractionMap.get(suite);
            if(ext.isSuccessful()) {
                // get lut for the test suite
                final String lutStr = ext.getLanguage();
                if(lutStr == null) {
                    logger.warn("Can't execute tests of suite {}, as there is no language header.", suite);
                    continue;
                }
                final ILanguage lutLang = langService.getLanguage(lutStr);
                if(lutLang == null) {
                    logger.warn("Can't execute tests of suite {}, as the language {} couldn't be loaded.", suite,
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
                for(ITestCase test : ext.getTests()) {
                    final TestCaseRun tcr = caseRuns.get(test);
                    tcr.start();
                    final ISpoofaxTestResult result = runner.run(project, test, lut, null, cfg);
                    finish(tcr, result);
                }
            }
        }

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
