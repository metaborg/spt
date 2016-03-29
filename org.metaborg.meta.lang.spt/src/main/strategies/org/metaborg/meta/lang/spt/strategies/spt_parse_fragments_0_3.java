package org.metaborg.meta.lang.spt.strategies;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.jsglr.client.imploder.Tokenizer;
import org.spoofax.terms.Term;
import org.spoofax.terms.TermTransformer;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.inject.Injector;

/**
 * Parse the Test fragments of an SPT file.
 *
 * Usage: <spt_parse_fragments(|filePath, languageUnderTestName, targetLanguageName)> rawAst
 * <ul>
 * <li>
 *   filePath : the absolute path to the resource associated with rawAST.
 *   We use {@link IResourceService#resolve(String)} to get the resource.
 * </li>
 * <li>
 *   languageUnderTestName : the name of the language under test (LUT).
 *   Should be a String.
 * </li>
 * <li>
 *   targetLanguageName : the name of the target language.
 *   The target language is the language used to parse Output (e.g. parse to) fragments.
 *   Should be a String if the target language was specified.
 *   Any non-String term will be treated as if no target language was specified.
 *   This defaults to using the LUT as target language too.
 * </li>
 * <li>rawAst : the AST as retrieved by the normal parsing process.</li>
 * </ul>
 *
 * @author Volker Lanting
 */
public class spt_parse_fragments_0_3 extends Strategy {

    public static final spt_parse_fragments_0_3 instance = new spt_parse_fragments_0_3();

    private static final ILogger logger = LoggerUtils.logger(spt_parse_fragments_0_3.class);

    private static SelectionFetcher selectionFetcher;

    @Override
    public IStrategoTerm invoke(Context context, IStrategoTerm ast, IStrategoTerm filePath, IStrategoTerm lutNameTerm, IStrategoTerm targetLangNameTerm) {
        final Injector injector = ((IContext) context.contextObject()).injector();
        // TODO: should this term factory be specific to SPT? or maybe the LUT or target language?
        final ITermFactory termFactory = injector.getInstance(ITermFactoryService.class).getGeneric();
        final ILanguageService langService = injector.getInstance(ILanguageService.class);
        final IResourceService resourceService = injector.getInstance(IResourceService.class);

        if (selectionFetcher == null)
        	selectionFetcher = new SelectionFetcher(injector);

        // Get the Tokenizer for retokenization of parsed fragments' tokens into the current AST's tokens
        final ITokenizer itokenizer = ImploderAttachment.getTokenizer(ast);
        if (!(itokenizer instanceof Tokenizer)) {
            throw new IllegalStateException("Can't retokenize from an ITokenizer instance other than Tokenizer. Found " + itokenizer.getClass().getName());
        }
        final Tokenizer tokenizer = (Tokenizer) itokenizer;
        final Retokenizer retokenizer = new Retokenizer(injector, tokenizer);

        // Get the textual SPT specification where the AST was parsed from
        final String inputString = tokenizer.getInput();

        // Get the SPT file for which we are parsing fragments 
        final FileObject sptFile = resourceService.resolve(Tools.asJavaString(filePath));

        // Get the language under test (LUT)
        final ILanguage lutLang = langService.getLanguage(Tools.asJavaString(lutNameTerm));
        if (lutLang == null) {
            throw new IllegalStateException("The language under test should be loaded by now, but it isn't.");
        }
        // Get the language of any Output fragments (defaults to the LUT)
        final ILanguage targetLang;
        final boolean targetLangGiven = Tools.isTermString(targetLangNameTerm);
        if (targetLangGiven) {
            targetLang = langService.getLanguage(Tools.asJavaString(targetLangNameTerm));
            if (targetLang == null) {
                throw new IllegalStateException("The target language should be loaded by now, but it isn't.");
            }
        } else {
            // default to LUT
            targetLang = lutLang;
        }

        // Create the FragmentParsers that will parse the Input and Output fragments
        final IStrategoConstructor inputSetupCons = termFactory.makeConstructor("Setup", 3);
        final IStrategoConstructor outputSetupCons = termFactory.makeConstructor("TargetSetup", 3);
        // TODO: start symbol is not supported yet, so we just pass null for now
        final FragmentParser inputFragmentParser = new FragmentParser(injector, inputSetupCons, null);
        inputFragmentParser.configure(lutLang.activeImpl(), sptFile, ast);
        final FragmentParser outputFragmentParser = new FragmentParser(injector, outputSetupCons, null);
        outputFragmentParser.configure(targetLang.activeImpl(), sptFile, ast);

        // Look for fragments and parse them
        final IStrategoConstructor inputCons = termFactory.makeConstructor("Input", 4);
        final IStrategoConstructor outputCons = termFactory.makeConstructor("Output", 4);
        final IStrategoConstructor errorCons = termFactory.makeConstructor("Error", 1);
        IStrategoTerm newAst = new TermTransformer(termFactory, true) {
            private boolean inSetup = false;

            @Override
            public IStrategoTerm preTransform(IStrategoTerm term) {
                IStrategoConstructor termCons = Term.tryGetConstructor(term);
                // keep track of whether we are in a (target) setup block or not
                if (termCons == inputSetupCons || termCons == outputSetupCons) {
                    inSetup = true;
                }

                // pick the parser for Test Input or Output fragments
                FragmentParser fragmentParser = null;
                if (!inSetup && termCons == inputCons) {
                    fragmentParser = inputFragmentParser;
                } else if (!inSetup && termCons == outputCons) {
                    fragmentParser = outputFragmentParser;
                }

                // Process the fragment
                if (fragmentParser == null) {
                    // this term is not a fragment
                    return term;
                } else {
                    // parse the fragment
                    IStrategoTerm parsedFragment;
                    try {
                        parsedFragment = fragmentParser.parse(inputString, term, false);
                    } catch (ParseException e) {
                        // parsing of the fragment failed unexpectedly
                        // TODO: verify that parse fails tests don't throw this exception!
                        throw new MetaborgRuntimeException(e);
                    }

                    // retokenize: add all leftover tokens up to this fragment to the new tokenizer
                    IStrategoTerm fragmentHead = Term.termAt(term, 1);
                    IStrategoTerm fragmentTail = Term.termAt(term, 2);
                    int fragmentStartIndex = ImploderAttachment.getLeftToken(fragmentHead).getIndex();
                    int fragmentEndIndex = ImploderAttachment.getRightToken(fragmentTail).getIndex();
                    retokenizer.copyTokensUpToIndex(fragmentStartIndex - 1);
                    // retokenize: add all tokens of the parsed fragment instead of the Input/Output term
                    // NOTE: this messes up the SPT fragment's start and end token!
                    retokenizer.copyTokensFromFragment(fragmentHead, fragmentTail, parsedFragment,
                            ImploderAttachment.getLeftToken(fragmentHead).getStartOffset(), 
                            ImploderAttachment.getRightToken(fragmentTail).getEndOffset());
                    // retokenize: skip all tokens of the Input/Output term
                    retokenizer.skipTokensUpToIndex(fragmentEndIndex);

                    if (!fragmentParser.isLastSyntaxCorrect()) {
                        parsedFragment = termFactory.makeAppl(errorCons, parsedFragment);
                    }
                    // Fetch the fragments
                    IStrategoList selections = selectionFetcher.fetch(term, parsedFragment);
                    /* TODO annotating a term wraps it into a new term, without keeping attachments
                     * SPT used to use .clone() on the ImploderAttachment
                     * and keep the ParentAttachment by using a ParentTermFactory.
                     * However, TermTransformer just uses ITermBuilder.copyAttachments.
                     * That seems cleaner than manually managing it.
                     * Would it break anything?
                     */
                    IStrategoTerm annotatedTerm = termFactory.annotateTerm(term, termFactory.makeListCons(parsedFragment, selections));
                    termFactory.copyAttachments(term, annotatedTerm);
                    return annotatedTerm;
                }
            }

            public IStrategoTerm postTransform(IStrategoTerm term) {
                // keep track of whether we are in a (target) setup block or not
                IStrategoConstructor termCons = Term.tryGetConstructor(term);
                if (termCons == inputSetupCons || termCons == outputSetupCons) {
                    inSetup = false;
                }
                /*
                 *  TODO: SPT used to (re)set all parent attachments here.
                 *  However, I don't see how we could have messed them up,
                 *  other than annotating the Input/Output term.
                 *  I tried copying the ParentAttachment onto that term in preTransform.
                 *  That should mean all ParentAttachments are already correct
                 *  and we can save some time by not setting the ParentAttachment
                 *  for each term in the AST.
                 *  Let's hope it works.
                 */
                return term;
            };
        }.transform(ast);

        // retokenizer: finish up
        retokenizer.copyTokensAfterFragments();
        retokenizer.getTokenizer().setAst(newAst);
        retokenizer.getTokenizer().initAstNodeBinding();

        return newAst;
    }

}