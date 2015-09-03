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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.syntax.ISyntaxService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.core.syntax.ParseResult;
import org.metaborg.spoofax.core.syntax.JSGLRParserConfiguration;
import org.metaborg.spoofax.core.syntax.SourceAttachment;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.jsglr.client.imploder.Token;
import org.spoofax.terms.StrategoListIterator;
import org.spoofax.terms.TermVisitor;
import org.strategoxt.lang.WeakValueHashMap;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * A parser for the fragments (Input and Output terms) of an SPT test suite specification.
 *
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class FragmentParser {

    private static final boolean ALLOW_CACHING = false; // currently useless; plus it breaks setup sections at
                                                        // end of file
    private static final int EXCLUSIVE = 1;

    private static final int FRAGMENT_PARSE_TIMEOUT = 3000;

    private final ITermFactoryService factoryService;
    private final ITermFactory termFactory;
    private final ISyntaxService<IStrategoTerm> syntaxService;
    private final IResourceService resourceService;

    // required for whiting out the test input
    private static IStrategoConstructor QUOTEPART_1;

    // required to tell if an Output fragment is expected to parse successfully.
    private static IStrategoConstructor OUTPUT_4;

    // required to tell if an Input fragment inside a Setup block is expected to parse successfully.
    private static IStrategoConstructor SETUP_3;
    private static IStrategoConstructor TARGET_SETUP_3;

    // required to tell if a fragment inside a Test is expected to parse successfully.
    private static IStrategoConstructor FAILS_PARSING_0;

    /* The constructor we will use to locate setup blocks
     * Can be either Setup(_, name, fragment) for parsing Input fragments,
     * or TargetSetup(_, name, fragment) for parsing Output fragments
     */
    private final IStrategoConstructor setup_3;

    // The constructor we will use to locate the start symbol
    // Used to be configurable, now statically set to Setup(_)
    // TODO: at the moment this is not used
    private final IStrategoConstructor topsort_1;

    // CACHING TODO: unused at the moment, see ALLOW_CACHING
    private final WeakValueHashMap<String, IStrategoTerm> failParseCache =
        new WeakValueHashMap<String, IStrategoTerm>();

    private final WeakValueHashMap<String, IStrategoTerm> successParseCache =
        new WeakValueHashMap<String, IStrategoTerm>();


    // Both the fragment language and the specification's resource are required for parsing.
    private ILanguageImpl language;
    private FileObject sptFile;

    // The regions (character offsets in the input) representing the Setup blocks
    private List<OffsetRegion> setupRegions;

    private boolean isLastSyntaxCorrect;

    /**
     * Create a FragmentParser that can parse fragments, 
     * also taking into account setup blocks of the given constructor.
     *
     * @param injector the injector we should use to get the required services.
     * @param setup_3 the constructor for the type of Setup blocks we care about 
     *  (Setup for input fragments, or TargetSetup for output fragments).
     * @param topsort_1 TODO: unused until we can figure out a way to set start symbols for the ISyntaxService.
     */
    public FragmentParser(Injector injector, IStrategoConstructor setup_3, IStrategoConstructor topsort_1) {
        this.factoryService = injector.getInstance(ITermFactoryService.class);
        this.syntaxService = injector.getInstance(Key.get(new TypeLiteral<ISyntaxService<IStrategoTerm>>(){}));
        this.resourceService = injector.getInstance(IResourceService.class);
        this.termFactory = factoryService.getGeneric();
        if (QUOTEPART_1 == null)
            QUOTEPART_1 = termFactory.makeConstructor("QuotePart", 1);
        if (OUTPUT_4 == null)
            OUTPUT_4 = termFactory.makeConstructor("Output", 4);
        if (SETUP_3 == null)
            SETUP_3 = termFactory.makeConstructor("Setup", 3);
        if (TARGET_SETUP_3 == null)
            TARGET_SETUP_3 = termFactory.makeConstructor("TargetSetup", 3);
        if (FAILS_PARSING_0 == null)
            FAILS_PARSING_0 = termFactory.makeConstructor("FailsParsing", 0);
        this.setup_3 = setup_3;
        this.topsort_1 = topsort_1;
        language = null;
        sptFile = null;
    }

    /**
     * Configure this FragmentParser to parse fragments of the given language under test.
     * @param lang the language of the fragments.
     * @param sptFile the resource with which the SPT testsuite specification is associated.
     *  Used for parsing with the ISyntaxService (I think it's only used for incremental parsing).
     * @param ast the AST of the SPT testsuite specification
     */
    public void configure(ILanguageImpl lang, FileObject sptFile, IStrategoTerm ast) {
        if(this.language != lang || this.sptFile != sptFile) {
            this.language = lang;
            this.sptFile = sptFile;
            failParseCache.clear();
            successParseCache.clear();
        }
        // record all the offsets of the setup blocks
        setupRegions = getSetupRegions(ast);
        // TODO: find a way to set the start symbol for the syntaxService
    }

    /**
     * Checks if this FragmentParser has been configured with a fragment language.
     * @return true iff this FragmentParser has been configured.
     * @see #configure(ILanguageImpl, FileObject, IStrategoTerm)
     */
    public boolean isInitialized() {
        return language != null;
    }

    /**
     * Parses an Input or Output fragment.
     *
     * @param input the textual SPT testsuite specification (obtain through ITokenizer).
     * @param fragment the Input or Output term that should be parsed.
     * @param ignoreSetup If set to true, we will not parse the setup blocks of the SPT testsuite specification.
     *  In that case we will only parse the given fragment (TODO: why would we want that?).
     * @return the AST obtained by parsing the fragment.
     *  Note that this AST may include the parsed setup blocks too (see the ignoreSetup parameter).
     * @throws ParseException when parsing fails unexpectedly.
     *  TODO: is this exception always raised when parsing fails? Or only under extraordinary circumstances?
     */
    public IStrategoTerm parse(String input, IStrategoTerm fragment, boolean ignoreSetup) throws ParseException
    {
        // TODO: use context-independent caching key
        // (requires offset adjustments for reuse...)
        String fragmentInput = createTestFragmentString(input, fragment, ignoreSetup);
        boolean successExpected = isSuccessExpected(fragment);
        IStrategoTerm parsed = getCache(successExpected).get(fragmentInput);
        if(parsed != null) {
            isLastSyntaxCorrect = successExpected;
        }
        // parse the fragment
        if(parsed == null || !ALLOW_CACHING) {
            final ParseResult<IStrategoTerm> parseResult = syntaxService.parse(fragmentInput, sptFile, language, new JSGLRParserConfiguration(true, true, false, FRAGMENT_PARSE_TIMEOUT));
            parsed = parseResult.result;
            // TODO: can we rely on this? or should we now use parsed != null? or check parseResult.messages?
            isLastSyntaxCorrect = getTokenizer(parsed).isSyntaxCorrect();
            // The parsed fragment will have the same resource as the SPT file it came from
            SourceAttachment.putSource(parsed, SourceAttachment.getResource(fragment, resourceService));
            // clear any errors from parsing if we don't expect it to succeed
            if(!successExpected)
                clearTokenErrors(getTokenizer(parsed));
            // if the testcase is successful, put it in the cache
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
     * @param originalInput the original textual SPT specification you want to parse frgaments from.
     * @param term the fragment (Input or Output term) you want to preserve.
     * @param ignoreSetup set to true if you don't want to preserve Setup regions.
     * @return the output string.
     */
    private String createTestFragmentString(String originalInput, IStrategoTerm term, boolean ignoreSetup) {

        // get the first StringPart from the Input or Output term
        IStrategoTerm fragmentHead = term.getSubterm(1);
        // get the rest of the fragment (i.e. More or Done)
        IStrategoTerm fragmentTail = term.getSubterm(2);
        // determine the start and end offsets of the fragment in the tokenizer's input
        int fragmentStart = getLeftToken(fragmentHead).getStartOffset();
        int fragmentEnd = getRightToken(fragmentTail).getEndOffset();
        // TODO: why does 'compact' whitespace require MORE space in the result?
        StringBuilder result = new StringBuilder(originalInput.length());

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
                    addWhitespace(originalInput, index, fragmentStart - 1, result);
                    appendFragment(fragmentHead, originalInput, result);
                    appendFragment(fragmentTail, originalInput, result);
                    index = fragmentEnd + 1;
                    addedFragment = true;
                }
                // add the Setup region (unless the current fragment is the Setup region)
                if(fragmentStart != setupStart) { // only if fragment != setup region
                    addWhitespace(originalInput, index, setupStart - 1, result);
                    if(setupEnd >= index) {
                        result.append(originalInput, max(setupStart, index), setupEnd + EXCLUSIVE);
                        index = setupEnd + 1;
                    }
                }
            }
        }

        // add the fragment if the processing of setup regions hasn't done so already
        if(!addedFragment) {
            addWhitespace(originalInput, index, fragmentStart - 1, result);
            appendFragment(fragmentHead, originalInput, result);
            appendFragment(fragmentTail, originalInput, result);
            index = fragmentEnd + 1;
        }

        // white out the rest of the input (only required to keep offsets the same)
        addWhitespace(originalInput, index, originalInput.length() - 1, result);

        assert result.length() == originalInput.length();
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
     * @param term the term whose characters should be appended. A QuotePart, More, or Done term.
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
     *
     * @param input the input String of which parts should not be parsed.
     * @param startOffset the offset of the first character we white out.
     * @param endOffset the offset of the last character we white out.
     * @param output the StringBuilder to which we should append the whitespace characters.
     */
    private static void addWhitespace(String input, int startOffset, int endOffset, StringBuilder output) {
        for(int i = startOffset; i <= endOffset; i++)
            output.append(input.charAt(i) == '\n' ? '\n' : ' ');
    }

    /**
     * Record the start and end character offsets of all QuoteParts in setup blocks int the AST.
     *
     * We use {@link #setup_3} to find setup blocks, and {@link #QUOTEPART_1} to find QuoteParts.
     *
     * @param ast the AST in which to look for setup blocks.
     * @return a list of OffsetRegions, each representing a (part of) a Setup block's contents.
     */
    private List<OffsetRegion> getSetupRegions(IStrategoTerm ast) {
        final List<OffsetRegion> results = new ArrayList<OffsetRegion>();
        new TermVisitor() {
            public void preVisit(IStrategoTerm term) {
                // look for setup blocks
                if(tryGetConstructor(term) == setup_3) {
                    new TermVisitor() {
                        public final void preVisit(IStrategoTerm term) {
                            // look for QuoteParts
                            if(tryGetConstructor(term) == QUOTEPART_1) {
                                term = term.getSubterm(0);
                                // record the offsets of the QuotePart's content
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

    // TODO: can this go? What was it for?
    /*
     * private boolean isSetupToken(IToken token) { // if (token.getKind() != IToken.TK_STRING) return false; assert
     * token.getKind() == IToken.TK_STRING; IStrategoTerm node = (IStrategoTerm) token.getAstNode(); if (node != null &&
     * "Input".equals(getSort(node))) { IStrategoTerm parent = getParent(node); if (parent != null && isTermAppl(parent)
     * && "Setup".equals(((IStrategoAppl) parent).getName())) return true; } return false; }
     */

    /**
     * 
     * @param fragment
     * @return
     */
    private boolean isSuccessExpected(IStrategoTerm fragment) {
        // Output fragments are expected to parse
        if(tryGetConstructor(fragment) == OUTPUT_4)
            return true;

        // get the enclosing Setup or Test term
        IStrategoAppl test = (IStrategoAppl) getParent(fragment);

        // Setup blocks are expected to parse.
        /* TODO: why are setup blocks expected to succeed parsing?
         * I.m.o. they may have to be combined with a test fragment to be syntactically correct
         * It doesn't hurt to leave it like this though,
         * as we can always decide to stop the errors at a later stage
         */
        if(test.getConstructor() == SETUP_3 || test.getConstructor() == TARGET_SETUP_3)
            return true;

        // Tests are expected to parse unless the expectation says otherwise
        IStrategoList expectations = listAt(test, test.getSubtermCount() - 1);
        for(IStrategoTerm expectation : StrategoListIterator.iterable(expectations)) {
            IStrategoConstructor cons = tryGetConstructor(expectation);
            if(cons == FAILS_PARSING_0)
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
