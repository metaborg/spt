package org.metaborg.meta.lang.spt.strategies;

import static org.spoofax.interpreter.core.Tools.asJavaString;
import static org.spoofax.interpreter.core.Tools.isTermList;
import static org.spoofax.interpreter.core.Tools.termAt;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getTokenizer;
import static org.spoofax.terms.Term.tryGetConstructor;

import java.io.IOException;
import java.util.Iterator;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spoofax.core.syntax.JSGLRI;
import org.metaborg.spoofax.core.syntax.JSGLRParserConfiguration;
import org.metaborg.spoofax.core.syntax.ParserConfig;
import org.metaborg.sunshine.environment.LaunchConfiguration;
import org.metaborg.sunshine.environment.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.SGLRParseResult;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.jsglr.client.imploder.Tokenizer;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.terms.StrategoListIterator;
import org.spoofax.terms.TermTransformer;
import org.spoofax.terms.TermVisitor;
import org.spoofax.terms.attachments.ParentAttachment;
import org.spoofax.terms.attachments.ParentTermFactory;

public class SpoofaxTestingJSGLRI extends JSGLRI {

    private static final int PARSE_TIMEOUT = 20 * 1000;

    private static ITermFactory factory = ServiceRegistry.INSTANCE().getService(LaunchConfiguration.class).termFactory;
    private static final IStrategoConstructor INPUT_4 = factory.makeConstructor("Input", 4);

    private static final IStrategoConstructor OUTPUT_4 = factory.makeConstructor("Output", 4);

    private static final IStrategoConstructor ERROR_1 = factory.makeConstructor("Error", 1);

    private static final IStrategoConstructor LANGUAGE_1 = factory.makeConstructor("Language", 1);

    private static final IStrategoConstructor TARGET_LANGUAGE_1 = factory.makeConstructor("TargetLanguage", 1);

    private static final IStrategoConstructor SETUP_3 = factory.makeConstructor("Setup", 3);

    private static final IStrategoConstructor TARGET_SETUP_3 = factory.makeConstructor("TargetSetup", 3);

    private static final IStrategoConstructor TOPSORT_1 = factory.makeConstructor("TopSort", 1);

    private static final IStrategoConstructor TARGET_TOPSORT_1 = factory.makeConstructor("TargetTopSort", 1);

    private static final Logger LOG = LoggerFactory.getLogger(SpoofaxTestingJSGLRI.class);

    // FIXME: the null as injector will break SPT, but this class shouldn't be used anymore anyway
    private final FragmentParser fragmentParser = new FragmentParser(null, SETUP_3, TOPSORT_1);
    private final FragmentParser outputFragmentParser = new FragmentParser(null, TARGET_SETUP_3, TARGET_TOPSORT_1);

    // TODO: passing null as injector will break stuff, but this code should not be executed anyway.
    private final SelectionFetcher selections = new SelectionFetcher(null);

    public SpoofaxTestingJSGLRI(JSGLRI template) throws IOException {
        super(new ParserConfig(template.getConfig().getStartSymbol(), template.getConfig().getParseTableProvider()),
            factory, template.getLanguage(), template.getDialect(), template.getResource(), template.getInput());
    }

    @Override public SGLRParseResult
        actuallyParse(String input, String filename, JSGLRParserConfiguration parserConfig)
            throws InterruptedException, SGLRException {
        IStrategoTerm ast = (IStrategoTerm) super.actuallyParse(input, filename, parserConfig).output;
        try {
            return new SGLRParseResult(null, parseTestedFragments(ast));
        } catch(IOException e) {
            throw new SGLRException(getParser(), "Cannot parse", e);
        }
    }

    private IStrategoTerm parseTestedFragments(final IStrategoTerm root) throws IOException {
        final Tokenizer oldTokenizer = (Tokenizer) getTokenizer(root);
        // FIXME: null as injector will break stuff, but this code should never be executed anyway
        final Retokenizer retokenizer = new Retokenizer(null, oldTokenizer);
        final ITermFactory nonParentFactory = factory;
        final ITermFactory factory = new ParentTermFactory(nonParentFactory);
        final FragmentParser testedParser = configureFragmentParser(root, getLanguage(root), fragmentParser);
        // FIXME: using testedParser as outputParser is incorrect, as it is initialized to use Setup blocks instead of TargetSetup blocks!
        final FragmentParser outputParser =
            getTargetLanguage(root) == null ? testedParser : configureFragmentParser(root, getTargetLanguage(root),
                outputFragmentParser);
        assert !(nonParentFactory instanceof ParentTermFactory);

        if(testedParser == null || !testedParser.isInitialized() || outputParser == null
            || !outputParser.isInitialized()) {
            return root;
        }

        IStrategoTerm result = new TermTransformer(factory, true) {
            @Override public IStrategoTerm preTransform(IStrategoTerm term) {
                IStrategoConstructor cons = tryGetConstructor(term);
                FragmentParser parser = null;
                if(cons == INPUT_4) {
                    parser = testedParser;
                } else if(cons == OUTPUT_4) {
                    parser = outputParser;
                }
                if(parser != null) {
                    IStrategoTerm fragmentHead = termAt(term, 1);
                    IStrategoTerm fragmentTail = termAt(term, 2);
                    // copy the tokens up to (not including)
                    // the Input/Output fragment to the new tokenizer
                    retokenizer.copyTokensUpToIndex(getLeftToken(fragmentHead).getIndex() - 1);
                    try {
                        // parse the fragment
                        IStrategoTerm parsed = parser.parse(oldTokenizer.getInput(), term, /* cons == OUTPUT_4 */false);
                        // copy the tokens of the parsed fragment to the new tokenizer
                        int oldFragmentEndIndex = getRightToken(fragmentTail).getIndex();
                        retokenizer.copyTokensFromFragment(fragmentHead, fragmentTail, parsed,
                            getLeftToken(fragmentHead).getStartOffset(), getRightToken(fragmentTail).getEndOffset());
                        if(!parser.isLastSyntaxCorrect())
                            parsed = nonParentFactory.makeAppl(ERROR_1, parsed);
                        ImploderAttachment implodement = ImploderAttachment.get(term);
                        // TODO: passing null as SPT fragment will break stuff
                        // get the marked selections from the parse result
                        IStrategoList selected = selections.fetch(null, parsed);
                        // annotate the Input/Output fragment with the parse result and the marked selections
                        term = factory.annotateTerm(term, nonParentFactory.makeListCons(parsed, selected));
                        term.putAttachment(implodement.clone());
                        // skip the Input/Output fragment's tokens, they won't be part of the new tokenizer
                        // TODO: what are the results of this?
                        retokenizer.skipTokensUpToIndex(oldFragmentEndIndex);
                    } catch(CloneNotSupportedException e) {
                        // e.printStackTrace();
                        LOG.error("Could not parse tested code fragment(CNSE)", e);
                    } catch(RuntimeException e) {
                        // e.printStackTrace();
                        LOG.error("Could not parse tested code fragment(RE)", e);
                    } catch (ParseException e) {
                        LOG.error("Could not parse tested code fragment(PE)", e);
                    }
                }
                return term;
            }

            @Override public IStrategoTerm postTransform(IStrategoTerm term) {
                Iterator<IStrategoTerm> iterator = TermVisitor.tryGetListIterator(term);
                for(int i = 0, max = term.getSubtermCount(); i < max; i++) {
                    IStrategoTerm kid = iterator == null ? term.getSubterm(i) : iterator.next();
                    ParentAttachment.putParent(kid, term, null);
                }
                return term;
            }
        }.transform(root);
        retokenizer.copyTokensAfterFragments();
        retokenizer.getTokenizer().setAst(result);
        retokenizer.getTokenizer().initAstNodeBinding();
        return result;
    }

    private FragmentParser configureFragmentParser(IStrategoTerm root, ILanguageImpl language,
        FragmentParser fragmentParser) throws IOException {
        if(language == null)
            return null;
        fragmentParser.configure(language, super.getResource(), root);
        // FIXME: I have no clue how commenting this will affect SPT
        // it's probably only editor related, so no worries for the Sunshine version I think
        // attachToLanguage(language);
        return fragmentParser;
    }

    // /**
    // * Add our language service to the descriptor of a fragment language,
    // * so our service gets reinitialized once the fragment language changes.
    // */
    // private void attachToLanguage(Language theirLanguage) {
    // SGLRParseController myController = getController();
    // EditorState myEditor = myController.getEditor();
    // if (myEditor == null)
    // return;
    // ILanguageService myWrapper = myEditor.getEditor().getParseController();
    // if (myWrapper instanceof IDynamicLanguageService) {
    // Descriptor theirDescriptor = Environment.getDescriptor(theirLanguage);
    // theirDescriptor.addActiveService((IDynamicLanguageService) myWrapper);
    // } else {
    // Environment.logException("SpoofaxTestingParseController wrapper is not IDynamicLanguageService");
    // }
    // }

    private String getLanguageName(IStrategoTerm root, IStrategoConstructor which) {
        if(root.getSubtermCount() < 1 || !isTermList(termAt(root, 0)))
            return null;
        IStrategoList headers = termAt(root, 0);
        for(IStrategoTerm header : StrategoListIterator.iterable(headers)) {
            if(tryGetConstructor(header) == which) {
                IStrategoString name = termAt(header, 0);
                return asJavaString(name);
            }
        }
        return null;
    }

    private ILanguageImpl getLanguage(IStrategoTerm root) {
        final String languageName = getLanguageName(root, LANGUAGE_1);
        if(languageName == null)
            return null;
        return ServiceRegistry.INSTANCE().getService(ILanguageService.class).getLanguage(languageName).activeImpl();
    }

    private ILanguageImpl getTargetLanguage(IStrategoTerm root) {
        String languageName = getLanguageName(root, TARGET_LANGUAGE_1);
        if(languageName == null)
            return null;
        return ServiceRegistry.INSTANCE().getService(ILanguageService.class).getLanguage(languageName).activeImpl();
    }
}
