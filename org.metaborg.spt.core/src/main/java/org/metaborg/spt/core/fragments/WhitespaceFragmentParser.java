package org.metaborg.spt.core.fragments;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.ISyntaxService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.core.syntax.ParseResult;
import org.metaborg.spt.core.IFragment;
import org.metaborg.spt.core.IFragmentParser;
import org.metaborg.spt.core.IFragment.FragmentPiece;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Inject;

/**
 * Parser for fragments of non-layout sensitive languages.
 * 
 * Ensures the correct offsets of the parse result, by adding whitespace to fill in the blanks.
 */
public class WhitespaceFragmentParser implements IFragmentParser {

    private final ISyntaxService<IStrategoTerm> parseService;

    @Inject public WhitespaceFragmentParser(ISyntaxService<IStrategoTerm> parseService) {
        this.parseService = parseService;
    }

    public ParseResult<IStrategoTerm> parse(IFragment fragment, ILanguageImpl language) throws ParseException {
        StringBuilder fragmentTextBuilder = new StringBuilder();
        for(FragmentPiece piece : fragment.getText()) {
            // add whitespace to get the character offset of this piece right
            for(int i = fragmentTextBuilder.length(); i < piece.startOffset; i++) {
                fragmentTextBuilder.append(" ");
            }
            // add the actual piece of program text from the fragment
            fragmentTextBuilder.append(piece.text);
        }
        String fragmentText = fragmentTextBuilder.toString();

        // now we can parse the fragment
        return parseService.parse(fragmentText, fragment.getResource(), language, null);
    }
}
