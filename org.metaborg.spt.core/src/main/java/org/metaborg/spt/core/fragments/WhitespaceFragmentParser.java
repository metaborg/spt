package org.metaborg.spt.core.fragments;

import javax.annotation.Nullable;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.IInputUnit;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.core.syntax.ISyntaxService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.core.unit.IInputUnitService;
import org.metaborg.spt.core.IFragment;
import org.metaborg.spt.core.IFragment.FragmentPiece;
import org.metaborg.spt.core.IFragmentParser;

import com.google.inject.Inject;

/**
 * Parser for fragments of non-layout sensitive languages.
 * 
 * Ensures the correct offsets of the parse result, by adding whitespace to fill in the blanks.
 */
public class WhitespaceFragmentParser<I extends IInputUnit, P extends IParseUnit> implements IFragmentParser<I, P> {

    private final IInputUnitService<I> inputService;
    private final ISyntaxService<I, P> parseService;

    @Inject public WhitespaceFragmentParser(IInputUnitService<I> inputService, ISyntaxService<I, P> parseService) {
        this.inputService = inputService;
        this.parseService = parseService;
    }

    @Override public P parse(IFragment fragment, ILanguageImpl language, @Nullable ILanguageImpl dialect)
        throws ParseException {

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
        I input = inputService.inputUnit(fragment.getResource(), fragmentText, language, dialect);

        return parseService.parse(input);
    }
}
