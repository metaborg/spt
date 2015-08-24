package org.metaborg.meta.lang.spt.strategies;

import static java.lang.Math.max;
import static org.spoofax.interpreter.core.Tools.asJavaString;
import static org.spoofax.interpreter.core.Tools.isTermString;
import static org.spoofax.interpreter.core.Tools.listAt;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getTokenizer;
import static org.spoofax.terms.Term.tryGetConstructor;
import static org.spoofax.terms.attachments.ParentAttachment.getParent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.source.ISourceTextService;
import org.metaborg.core.syntax.ISyntaxService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spoofax.core.esv.ESVReader;
import org.metaborg.spoofax.core.syntax.IParserConfig;
import org.metaborg.spoofax.core.syntax.JSGLRI;
import org.metaborg.spoofax.core.syntax.JSGLRSyntaxService;
import org.metaborg.spoofax.core.syntax.ParserConfig;
import org.metaborg.spoofax.core.syntax.SourceAttachment;
import org.metaborg.sunshine.environment.LaunchConfiguration;
import org.metaborg.sunshine.environment.ServiceRegistry;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.jsglr.client.imploder.Token;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.terms.StrategoListIterator;
import org.spoofax.terms.TermVisitor;
import org.strategoxt.lang.WeakValueHashMap;

import com.google.inject.TypeLiteral;

/**
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class FragmentParser {

    private static final boolean ALLOW_CACHING = false; // currently useless; plus it breaks setup sections at
                                                        // end of file

    private static final int FRAGMENT_PARSE_TIMEOUT = 3000;

    private static final ITermFactory factory =
        ServiceRegistry.INSTANCE().getService(LaunchConfiguration.class).termFactory;

    private static final IStrategoConstructor FAILS_PARSING_0 = factory.makeConstructor("FailsParsing", 0);

    private static final IStrategoConstructor SETUP_3 = factory.makeConstructor("Setup", 3);

    private static final IStrategoConstructor TARGET_SETUP_3 = factory.makeConstructor("TargetSetup", 3);

    private static final IStrategoConstructor OUTPUT_4 = factory.makeConstructor("Output", 4);

    private static final IStrategoConstructor QUOTEPART_1 = factory.makeConstructor("QuotePart", 1);

    private static final int EXCLUSIVE = 1;

    private final IStrategoConstructor setup_3;

    private final IStrategoConstructor topsort_1;

    // used to check if the FragmentParser was already configured for this ALanguage
    private ILanguageImpl parseCacheLanguage;

    private final WeakValueHashMap<String, IStrategoTerm> failParseCache =
        new WeakValueHashMap<String, IStrategoTerm>();

    private final WeakValueHashMap<String, IStrategoTerm> successParseCache =
        new WeakValueHashMap<String, IStrategoTerm>();

    private JSGLRI parser;

    private List<OffsetRegion> setupRegions;

    private boolean isLastSyntaxCorrect;

    public FragmentParser(IStrategoConstructor setup_3, IStrategoConstructor topsort_1) {
        assert setup_3.getArity() == 3;
        assert topsort_1.getArity() == 1;
        this.setup_3 = setup_3;
        this.topsort_1 = topsort_1;
        parseCacheLanguage = null;
    }

    public void configure(ILanguageImpl lang, FileObject sptFile, IStrategoTerm ast) throws IOException {
        if(parseCacheLanguage != lang) {
            parseCacheLanguage = lang;
            parser = getParser(lang, sptFile, ast);
            failParseCache.clear();
            successParseCache.clear();
        }
        setupRegions = getSetupRegions(ast);
    }

    public boolean isInitialized() {
        return parser != null;
    }

    /**
     * Creates a JSGLRI parser for the given language and file.
     * 
     * @param lang
     *            the language which we should parse.
     * @param file
     *            the file that this parser should parse.
     * @param ast
     *            TODO I don't know what it should represent, but it is used to determine the start symbol for the
     *            parser.
     * @return the parser, or null if lang was null.
     * @throws IOException
     */
    private JSGLRI getParser(ILanguageImpl lang, FileObject file, IStrategoTerm ast) throws IOException {
        if(lang == null)
            return null;
        IStrategoTerm start = ESVReader.findTerm(ast, topsort_1.getName());
        // start symbol creation is copied from the previous configure method
        String startSymbol = start == null ? null : asJavaString(start.getSubterm(0));
        final ServiceRegistry services = ServiceRegistry.INSTANCE();
        final ISourceTextService sourceTextService = services.getService(ISourceTextService.class);
        final JSGLRSyntaxService syntaxService =
            (JSGLRSyntaxService) services.getService(new TypeLiteral<ISyntaxService<IStrategoTerm>>() {});
        final IParserConfig existingConfig = syntaxService.getParserConfig(lang);
        final IParserConfig config = new ParserConfig(startSymbol, existingConfig.getParseTableProvider());
        final String inputText = sourceTextService.text(file);
        JSGLRI result = new JSGLRI(config, factory, lang, null, file, inputText);
        return result;
    }

    /**
     * Parses an Input or Output fragment.
     *
     * @param oldTokenizer TODO: what is it used for
     * @param fragment the Input or Output term that should be parsed
     * @param ignoreSetup TODO: how is this used
     * @return TODO: describe what is returned
     * @throws ParseException TODO: when does it throw this?
     */
    public IStrategoTerm parse(ITokenizer oldTokenizer, IStrategoTerm fragment, boolean ignoreSetup)
        throws InterruptedException, SGLRException,
        IOException
    {

        // TODO: use context-independent caching key
        // (requires offset adjustments for reuse...)
        String fragmentInput = createTestFragmentString(oldTokenizer, fragment, ignoreSetup, false);
        boolean successExpected = isSuccessExpected(fragment);
        IStrategoTerm parsed = getCache(successExpected).get(fragmentInput);
        if(parsed != null) {
            isLastSyntaxCorrect = successExpected;
        }
        if(parsed == null || !ALLOW_CACHING) {
            parsed = (IStrategoTerm) parser.actuallyParse(fragmentInput, oldTokenizer.getFilename(), null).output;
            isLastSyntaxCorrect = getTokenizer(parsed).isSyntaxCorrect();
            // The parsed fragment will have the same resource as the SPT file it came from
            SourceAttachment.putSource(parsed,
                SourceAttachment.getResource(fragment,
                    ServiceRegistry.INSTANCE().getService(IResourceService.class)
                ));
            if(!successExpected)
                clearTokenErrors(getTokenizer(parsed));
            if(isLastSyntaxCorrect == successExpected)
                getCache(isLastSyntaxCorrect).put(fragmentInput, parsed);
        }
        return parsed;
    }

    private WeakValueHashMap<String, IStrategoTerm> getCache(boolean parseSuccess) {
        return parseSuccess ? successParseCache : failParseCache;
    }

    /**
     * Create a String similar to the input of the given tokenizer,
     * but with only the tokens of the given fragment intact.
     * The other characters will be replaced by whitespace (keeping newlines intact)
     * to preserve the column and line numbers of the input.
     *
     * If ignoreSetup is false, the Setup regions that were recorded by {@link #configure(ILanguageImpl, FileObject, IStrategoTerm)}
     * will also be preserved.
     * @param tokenizer the tokenizer that contains the input you want to work with.
     * @param term the fragment (Input or Output term) you want to preserve.
     * @param ignoreSetup set to true if you don't want to preserve Setup regions.
     * @param compactWhitespace FIXME: does not really seem to be used...
     * @return the output string.
     */
    private String createTestFragmentString(ITokenizer tokenizer, IStrategoTerm term, boolean ignoreSetup,
        boolean compactWhitespace) {

        // get the first string part from the Input or Output term
        IStrategoTerm fragmentHead = term.getSubterm(1);
        // get the rest of the fragment (i.e. More or Done)
        IStrategoTerm fragmentTail = term.getSubterm(2);
        // determine the start and end offsets of the fragment in the tokenizer's input
        int fragmentStart = getLeftToken(fragmentHead).getStartOffset();
        int fragmentEnd = getRightToken(fragmentTail).getEndOffset();
        String input = tokenizer.getInput();
        // TODO: why does 'compact' whitespace require MORE space in the result?
        StringBuilder result = new StringBuilder(compactWhitespace ? input.length() + 16 : input.length());

        // add the setup regions appearing before the current fragment to the new input String
        boolean addedFragment = false;
        int index = 0;
        if(!ignoreSetup) {
            for(OffsetRegion setupRegion : setupRegions) {
                int setupStart = setupRegion.startOffset;
                int setupEnd = setupRegion.endOffset;
                // if the next Setup region is after the current fragment
                // or the current fragment is this Setup region
                if(!addedFragment && setupStart >= fragmentStart) {
                	// add the current fragment
                    addWhitespace(input, index, fragmentStart - 1, result);
                    appendFragment(fragmentHead, input, result);
                    appendFragment(fragmentTail, input, result);
                    index = fragmentEnd + 1;
                    addedFragment = true;
                }
                // add the Setup region (unless the current fragment is the Setuo region)
                if(fragmentStart != setupStart) { // only if fragment != setup region
                    addWhitespace(input, index, setupStart - 1, result);
                    if(setupEnd >= index) {
                        result.append(input, max(setupStart, index), setupEnd + EXCLUSIVE);
                        index = setupEnd + 1;
                    }
                }
            }
        }

        // add the fragment if the processing of setup regions hasn't done so already
        if(!addedFragment) {
            addWhitespace(input, index, fragmentStart - 1, result);
            appendFragment(fragmentHead, input, result);
            appendFragment(fragmentTail, input, result);
            index = fragmentEnd + 1;
        }

        // white out the rest of the input (only required to keep offsets the same)
        addWhitespace(input, index, input.length() - 1, result);

        assert result.length() == input.length();
        return result.toString();
    }

    /**
     * Append the characters of the given term (fragment) to the output.
     *
     * This is a recursive function.
     * QuoteParts will be copied over verbatim.
     * Strings will be replaced by whitespace.
     * For any other term (e.g. More or Done) we recurse on its children.
     * This recursion will find the brackets in More terms and replace those by whitespace,
     * and copy over all QuotePart contents.
     *
     * @param term the term whose characters should be appended a QuotePart, More, or Done term.
     * @param input the input from which the term was parsed.
     * @param output the output to which the characters should be appended.
     */
    private void appendFragment(IStrategoTerm term, String input, StringBuilder output) {
        IToken left = getLeftToken(term);
        IToken right = getRightToken(term);
        if(tryGetConstructor(term) == QUOTEPART_1) {
            output.append(input, left.getStartOffset(), right.getEndOffset() + EXCLUSIVE);
        } else if(isTermString(term)) {
            // Brackets: treat as whitespace
            assert asJavaString(term).length() <= 4 : "Bracket expected: " + term;
            addWhitespace(input, left.getStartOffset(), right.getEndOffset(), output);
        } else {
            // Other: recurse
            for(int i = 0; i < term.getSubtermCount(); i++) {
                appendFragment(term.getSubterm(i), input, output);
            }
        }
    }

    /**
     * Add whitespace to the output for all characters in the input between (inclusive) the given offsets.
     *
     * Newlines are preserved, to maintain the same line and column numbers in the output as in the input.
     * @param input the input String of which parts should not be parsed.
     * @param startOffset the offset of the first character we white out.
     * @param endOffset the offset of the last character we white out.
     * @param output the StringBuilder to which we should append the whitespace characters.
     */
    private static void addWhitespace(String input, int startOffset, int endOffset, StringBuilder output) {
        for(int i = startOffset; i <= endOffset; i++)
            output.append(input.charAt(i) == '\n' ? '\n' : ' ');
    }

    private List<OffsetRegion> getSetupRegions(IStrategoTerm ast) {
        final List<OffsetRegion> results = new ArrayList<OffsetRegion>();
        new TermVisitor() {
            public void preVisit(IStrategoTerm term) {
                if(tryGetConstructor(term) == setup_3) {
                    new TermVisitor() {
                        public final void preVisit(IStrategoTerm term) {
                            if(tryGetConstructor(term) == QUOTEPART_1) {
                                term = term.getSubterm(0);
                                results.add(new OffsetRegion(getLeftToken(term).getStartOffset(), getRightToken(term)
                                    .getEndOffset()));
                            }
                        }
                    }.visit(term);
                }
            }
        }.visit(ast);
        return results;
    }

    /*
     * private boolean isSetupToken(IToken token) { // if (token.getKind() != IToken.TK_STRING) return false; assert
     * token.getKind() == IToken.TK_STRING; IStrategoTerm node = (IStrategoTerm) token.getAstNode(); if (node != null &&
     * "Input".equals(getSort(node))) { IStrategoTerm parent = getParent(node); if (parent != null && isTermAppl(parent)
     * && "Setup".equals(((IStrategoAppl) parent).getName())) return true; } return false; }
     */

    private boolean isSuccessExpected(IStrategoTerm fragment) {
        if(tryGetConstructor(fragment) == OUTPUT_4)
            return true;
        IStrategoAppl test = (IStrategoAppl) getParent(fragment);
        /* TODO: why are setup blocks expected to succeed parsing?
         * I.m.o. they may have to be combined with a test fragment to be syntactically correct
         * It doesn't hurt to leave it like this though,
         * as we can always decide to stop the errors at a later stage
         */
        if(test.getConstructor() == SETUP_3 || test.getConstructor() == TARGET_SETUP_3)
            return true;
        IStrategoList expectations = listAt(test, test.getSubtermCount() - 1);
        for(IStrategoTerm expectation : StrategoListIterator.iterable(expectations)) {
            IStrategoConstructor cons = tryGetConstructor(expectation);
            if(/* cons == FAILS_0 || */cons == FAILS_PARSING_0)
                return false;
        }
        return true;
    }

    public boolean isLastSyntaxCorrect() {
        return isLastSyntaxCorrect;
    }

    private void clearTokenErrors(ITokenizer tokenizer) {
        for(IToken token : tokenizer) {
            ((Token) token).setError(null);
        }
    }

    /**
     * An (inclusive) offset tuple.
     *
     * @author Lennart Kats <lennart add lclnet.nl>
     */
    static class OffsetRegion {
        int startOffset, endOffset;

        OffsetRegion(int startOffset, int endOffset) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }

        @Override public String toString() {
            return "(" + startOffset + "," + endOffset + ")";
        }
    }
}
