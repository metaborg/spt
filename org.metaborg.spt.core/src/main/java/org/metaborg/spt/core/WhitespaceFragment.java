package org.metaborg.spt.core;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.ISyntaxService;
import org.metaborg.core.syntax.ParseResult;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.terms.Term;

import com.google.inject.Inject;

public class WhitespaceFragment extends AbstractFragment {

    private final ISyntaxService<IStrategoTerm> parseService;

    @Inject public WhitespaceFragment(ISyntaxService<IStrategoTerm> parseService, IStrategoTerm fragment) {
        super(fragment);
        this.parseService = parseService;
    }

    @Override public ParseResult<IStrategoTerm> parse(ILanguageImpl language) {
        // get the test suite specification from the fragment's tokenizer
        ITokenizer tokenizer = ImploderAttachment.getTokenizer(fragment);
        String fragmentText = getFragmentTextUsingWhitespace(tokenizer.getInput(), fragment);
    }


    /**
     * Get the text representing the given Fragment. All SPT related characters will be replaced by whitespace.
     * 
     * This keeps the character offsets of the fragment's text in sync with the offset within the test suite.
     */
    private static String getFragmentTextUsingWhitespace(String allText, IStrategoTerm fragmentTerm) {

        // determine the start and end offsets of the fragment in the tokenizer's input
        int fragmentStart = ImploderAttachment.getLeftToken(fragmentTerm).getStartOffset();
        int fragmentEnd = ImploderAttachment.getRightToken(fragmentTerm).getEndOffset();
        StringBuilder result = new StringBuilder(allText.length());

        // whiteout everything before the fragment
        addWhitespace(allText.substring(0, fragmentStart), result);
        // append the fragment with whitespace for selection markers
        appendFragmentUsingWhitespace(fragmentTerm, result);
        // whiteout everything after the fragment
        addWhitespace(allText.substring(fragmentEnd + 1), result);

        return result.toString();
    }

    /**
     * Replace any non-newline character in the given String by whitespace and append that to the StringBuilder.
     */
    private static void addWhitespace(String pieceToWhiteOut, StringBuilder output) {
        for(int i = 0; i < pieceToWhiteOut.length(); i++)
            output.append(pieceToWhiteOut.charAt(i) == '\n' ? '\n' : ' ');
    }

    /**
     * Append the given fragment's text to the given StringBuilder. Replaces Selection's markers by whitespace.
     */
    private static void appendFragmentUsingWhitespace(IStrategoTerm fragment, StringBuilder output) {
        String cons = SPTUtil.consName(fragment);
        if(cons == null) {
            if(Term.isTermString(fragment)) {
                output.append(Tools.asJavaString(fragment));
            } else {
                throw new IllegalArgumentException("Can't get append the fragment piece: " + fragment);
            }
        } else if(SPTUtil.TAILPART_DONE_CONS.equals(cons)) {
            // it's a Done(), we don't have to append anything
        } else if(SPTUtil.TAILPART_MORE_CONS.equals(cons)) {
            // it's More(Selection(...), "stringpart", <TailPart>)
            // append the selection
            appendFragmentUsingWhitespace(fragment.getSubterm(0), output);
            // append the string part
            appendFragmentUsingWhitespace(fragment.getSubterm(1), output);
            // append the TailPart
            appendFragmentUsingWhitespace(fragment.getSubterm(2), output);
        } else if(SPTUtil.SELECTION_CONS.equals(cons)) {
            // it's Selection(marker, "stringpart", marker)
            // whiteout the opening marker
            addWhitespace(Tools.asJavaString(fragment.getSubterm(0)), output);
            // add the string part
            appendFragmentUsingWhitespace(fragment.getSubterm(1), output);
            // whiteout the closing marker
            addWhitespace(Tools.asJavaString(fragment.getSubterm(2)), output);
        } else if(SPTUtil.FRAGMENT_CONS.equals(cons)) {
            // it's Fragment("stringpart", <TailPart>)
            // add the string part
            appendFragmentUsingWhitespace(fragment.getSubterm(0), output);
            // add the tail part
            appendFragmentUsingWhitespace(fragment.getSubterm(1), output);
        } else {
            throw new IllegalArgumentException("Can't append the unknown fragment piece : " + fragment);
        }
    }
}
