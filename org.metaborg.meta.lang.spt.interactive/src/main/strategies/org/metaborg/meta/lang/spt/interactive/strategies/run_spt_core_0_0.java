package org.metaborg.meta.lang.spt.interactive.strategies;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.spoofax.core.syntax.JSGLRParserConfiguration;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnitService;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;
import org.metaborg.spoofax.core.unit.ParseContrib;
import org.metaborg.spt.core.SPTModule;
import org.metaborg.spt.core.SPTUtil;
import org.metaborg.spt.core.extract.ISpoofaxTestCaseExtractionResult;
import org.metaborg.spt.core.extract.ISpoofaxTestCaseExtractor;
import org.metaborg.spt.core.run.ISpoofaxFragmentParserConfig;
import org.metaborg.spt.core.run.ISpoofaxTestCaseRunner;
import org.metaborg.spt.core.run.ISpoofaxTestResult;
import org.metaborg.spt.core.run.SpoofaxFragmentParserConfig;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.terms.Term;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Injector;

public class run_spt_core_0_0 extends Strategy {
    public static final run_spt_core_0_0 instance = new run_spt_core_0_0();

    private static final ILogger logger = LoggerUtils.logger(run_spt_core_0_0.class);

    private static final String MESSAGE = "MESSAGE";

    private static final String SUITE = "TestSuite";
    private static final String NAME = "Name";
    private static final String LUT = "Language";
    private static final String START = "StartSymbol";


    private static Map<Injector, Injector> injectorMap = Maps.newHashMap();


    @Override public IStrategoTerm invoke(Context strategoContext, IStrategoTerm current) {
        // Get the injector for the required services
        final IContext context = (IContext) strategoContext.contextObject();
        final Injector spoofaxInjector = context.injector();
        Injector injector = injectorMap.get(spoofaxInjector);
        if(injector == null) {
            injector = spoofaxInjector.createChildInjector(new SPTModule());
            injectorMap.put(spoofaxInjector, injector);
        }

        // Setup the things we need to return
        IStrategoTerm ast = null;
        final ITermFactory termFactory = strategoContext.getFactory();
        final List<IStrategoTerm> errors = Lists.newLinkedList();
        final List<IStrategoTerm> warnings = Lists.newLinkedList();
        final List<IStrategoTerm> notes = Lists.newLinkedList();

        // Get required services
        final ISpoofaxInputUnitService inputService = injector.getInstance(ISpoofaxInputUnitService.class);
        final ISpoofaxUnitService unitService = injector.getInstance(ISpoofaxUnitService.class);
        final IResourceService resourceService = injector.getInstance(IResourceService.class);
        final IProjectService projectService = injector.getInstance(IProjectService.class);
        final ILanguageService langService = injector.getInstance(ILanguageService.class);
        final ISpoofaxTestCaseExtractor extractor = injector.getInstance(ISpoofaxTestCaseExtractor.class);
        final ISpoofaxTestCaseRunner runner = injector.getInstance(ISpoofaxTestCaseRunner.class);

        // input term should be (ast, relative-path, project-path)
        if(!(current instanceof IStrategoTuple) || current.getSubtermCount() != 3) {
            // fail
            logger.warn("Why are you trying to analyze this thing? It isn't a (ast, filePath, projectPath) tuple! {}",
                current);
            return null;
        }

        // input terms
        final IStrategoTerm baseAst = current.getSubterm(0);
        ast = baseAst;
        final String relativePathString = Term.asJavaString(current.getSubterm(1));
        final String projectPathString = Term.asJavaString(current.getSubterm(2));
        logger.debug("Trying to analyze SPT testsuite: {}\n located at {}, in project {}.", baseAst, relativePathString,
            projectPathString);

        // Obtain the module name and language under test from the given ast
        String name = null;
        String lutName = null;
        String startSymbol = null;
        if(SUITE.equals(SPTUtil.consName(baseAst)) && baseAst.getSubtermCount() == 3) {
            IStrategoTerm headers = baseAst.getSubterm(0);
            for(IStrategoTerm header : headers.getAllSubterms()) {
                switch(SPTUtil.consName(header)) {
                    case NAME:
                        name = Term.asJavaString(header.getSubterm(0));
                        break;
                    case LUT:
                        lutName = Term.asJavaString(header.getSubterm(0));
                        break;
                    case START:
                        startSymbol = Term.asJavaString(header.getSubterm(0));
                        break;
                    default:
                        // just ignore it for now
                }
            }
            if(name == null) {
                errors.add(termFactory.makeTuple(headers, termFactory
                    .makeString("SPT test suites in Eclipse require a module name. (e.g., 'module mytestsuite')")));
            }
            if(lutName == null) {
                errors.add(termFactory.makeTuple(headers, termFactory.makeString(
                    "SPT test suites in Eclipse require a language under test. (e.g., 'language MyLanguage')")));
            }
        } else {
            errors.add(termFactory.makeTuple(baseAst, termFactory.makeString("Expected a TestSuite.")));
        }
        final ILanguage lutLang = lutName == null ? null : langService.getLanguage(lutName);
        final ILanguageImpl lut = lutLang == null ? null : lutLang.activeImpl();
        if(lutName != null && lut == null) {
            String msg = String.format("Unable to access the language under test: '%1$s'.", lutName);
            logger.error(msg);
            errors.add(termFactory.makeTuple(termFactory.makeString(msg)));
        }
        logger.debug("Finished looking for the language under test");

        // Create or get the project
        final FileObject projectPath = resourceService.resolve(projectPathString);
        final IProject project = projectService.get(projectPath);
        if(project == null) {
            String msg = String.format("Unable to create a project at location %1$s", projectPathString);
            logger.error(msg);
            errors.add(termFactory.makeTuple(termFactory.makeString(msg)));
        } else {
            logger.debug("Got the project for the tests.");
        }

        // Create or get the testsuite resource
        final FileObject testSuitePath = resourceService.resolve(projectPath, relativePathString);

        // Get the SPT core language implementation to extract test cases
        final ILanguage sptLang = langService.getLanguage("SPT");
        final ILanguageImpl spt = sptLang == null ? null : sptLang.activeImpl();
        if(spt == null) {
            String msg = "Unable to access the SPT core language.";
            logger.error(msg);
            errors.add(termFactory.makeTuple(termFactory.makeString(msg)));
        } else {
            logger.debug("Obtained the SPT language.");
        }

        // make sure we have all the data we need
        if(name == null || lut == null || spt == null || project == null || testSuitePath == null) {
            logger.debug("Not extracting tests, things are missing.");
            // assuming the proper error messages have been collected already
            return termFactory.makeTuple(baseAst, termFactory.makeList(errors), termFactory.makeList(),
                termFactory.makeList());
        }

        /*
         * -------------------------- Extract the test cases --------------------------
         */
        final ISpoofaxInputUnit input = inputService.emptyInputUnit(testSuitePath, spt, null);
        final ISpoofaxParseUnit parseUnit =
            unitService.parseUnit(input, new ParseContrib(true, true, false, baseAst, Iterables2.<IMessage>empty(), -1));
        logger.debug("Extracting tests.");
        final ISpoofaxTestCaseExtractionResult extractionResult = extractor.extract(parseUnit, project);
        logger.debug("Finished extracting tests.");
        final ISpoofaxParseUnit sptParseUnit = extractionResult.getParseResult();
        final ISpoofaxAnalyzeUnit sptAnalyzeUnit = extractionResult.getAnalysisResult();

        // Check if we have an analyzed ast
        if(sptParseUnit != null && sptParseUnit.success() && sptAnalyzeUnit.success() && sptAnalyzeUnit.hasAst()) {
            ast = sptAnalyzeUnit.ast();
        }

        // Propagate the messages
        // note that SPT parse messages have been recorded already
        if(sptParseUnit.success() && sptAnalyzeUnit != null) {
            gatherMessages(baseAst, testSuitePath, sptAnalyzeUnit.messages(), errors, warnings, notes, termFactory);
        }
        gatherMessages(baseAst, testSuitePath, extractionResult.getMessages(), errors, warnings, notes, termFactory);

        // Stop if extraction failed
        if(!extractionResult.isSuccessful()) {
            logger.debug("Extraction failed.");
            return termFactory.makeTuple(ast, termFactory.makeList(errors), termFactory.makeList(warnings),
                termFactory.makeList(notes));
        } else {
            logger.debug("Extraction succeeded.");
        }


        /*
         * ---------------------------- Execute all test cases ----------------------------
         */
        ISpoofaxFragmentParserConfig fragmentConfig = startSymbol == null ? null : new SpoofaxFragmentParserConfig();
        if(fragmentConfig != null) {
            fragmentConfig.putConfig(lut, new JSGLRParserConfiguration(startSymbol));
        }
        List<ISpoofaxTestResult> testResults = Lists.newLinkedList();
        for(ITestCase test : extractionResult.getTests()) {
            logger.debug("About to run test {}.", test.getDescription());
            // TODO: we don't support dialects yet
            ISpoofaxTestResult result = runner.run(project, test, lut, null, fragmentConfig);
            logger.debug("Ran test {}.", test.getDescription());
            testResults.add(result);
            if(result.isSuccessful()) {
                gatherMessages(ast, testSuitePath, result.getAllMessages(), errors, warnings, notes, termFactory);
                // TODO: inline the AST of the fragment in the token stream to get syntax highlighting
                // TODO: we might want to replace the SPT fragment node with the parsed/analyzed fragment AST
            } else {
                logger.debug("TestCase {} of test suite {} failed.", test.getDescription(), testSuitePath.getName());
                // We assume a failing test has at least one error in its result
                gatherMessages(ast, testSuitePath, result.getAllMessages(), errors, warnings, notes, termFactory);
            }
        }

        // TODO: get syntax coloring on the fragments

        return termFactory.makeTuple(ast, termFactory.makeList(errors), termFactory.makeList(warnings),
            termFactory.makeList(notes));
    }

    private static void gatherMessages(IStrategoTerm defaultTerm, FileObject testSuite, Iterable<IMessage> messages,
        Collection<IStrategoTerm> errors, Collection<IStrategoTerm> warnings, Collection<IStrategoTerm> notes,
        ITermFactory termFactory) {

        Collection<IStrategoTerm> addTo;
        for(IMessage message : messages) {

            switch(message.severity()) {
                case ERROR:
                    addTo = errors;
                    break;
                case WARNING:
                    addTo = warnings;
                    break;
                case NOTE:
                    addTo = notes;
                    break;
                default:
                    throw new IllegalStateException(String
                        .format("The analyzer framework can't place messages of severity %1$s", message.severity()));
            }

            addTo.add(makeMessage(message, defaultTerm, testSuite, termFactory));
        }
    }

    /**
     * Create a (ATerm, String) message tuple out of an IMessage.
     *
     * @param message
     *            the message to construct a Spoofax message ATerm from.
     * @param defaultTerm
     *            the default ATerm to use if the message had no region.
     * @param termFactory
     *            a term factory to make terms.
     */
    private static IStrategoTerm makeMessage(IMessage message, IStrategoTerm defaultTerm, FileObject testSuite,
        ITermFactory termFactory) {
        ISourceRegion region = message.region();
        IStrategoTerm term = region == null ? defaultTerm : createTermWithRegion(region, testSuite, termFactory);
        return termFactory.makeTuple(term, termFactory.makeString(message.message()));
    }

    /**
     * This is part of a hack to get messages printed.
     *
     * Spoofax core uses ISourceRegions for messages. Spoofax itself uses an ATerm and a String as a message. It then
     * uses an ImploderAttachment (or an OriginAttachment returning a term with an ImploderAttachment) to get the region
     * of the term and displays the message there.
     *
     * This makes a lot of sense from a Stratego point of view, where you just give the term you want the message to be
     * on, but it doesn't make sense for us, as we get the message as an IMessage with an ISourceRegion, and then have
     * to find the term that belongs to it, only to get that transformed back into a start and end offset.
     *
     * The question is: where do we find this term? It could be in a parsed fragment, or it could be in the SPT AST. The
     * old SPT did its error checking in Stratego, so it didn't have this problem.
     *
     * The hacky workaround is to just create a term with a custom ImploderAttachment with tokens on a bogus Tokenizer,
     * just to get the offsets right.
     *
     * In the future, we probably want to either look up the appropriate term (unnecessary work), or report the messages
     * in Spoofax as offset regions instead of as terms (would make most sense).
     */
    private static IStrategoTerm createTermWithRegion(ISourceRegion region, FileObject testSuite,
        ITermFactory termFactory) {
        IStrategoTerm term = termFactory.makeAppl(termFactory.makeConstructor(MESSAGE, 0));
        ImploderAttachment imploder = ImploderAttachment.createCompactPositionAttachment(testSuite.getName().toString(),
            -1, region.startColumn(), region.startOffset(), region.endOffset());
        term.putAttachment(imploder);
        return term;
    }

}

