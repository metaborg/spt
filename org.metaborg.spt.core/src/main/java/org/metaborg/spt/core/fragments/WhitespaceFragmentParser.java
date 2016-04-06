package org.metaborg.spt.core.fragments;

import javax.annotation.Nullable;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnitService;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.IFragment;
import org.metaborg.spt.core.IFragment.FragmentPiece;
import org.metaborg.spt.core.IFragmentParser;

import com.google.inject.Inject;

/**
 * Parser for fragments of non-layout sensitive languages.
 * 
 * Ensures the correct offsets of the parse result, by adding whitespace to fill in the blanks.
 */
public class WhitespaceFragmentParser implements IFragmentParser {

    private final ISpoofaxSyntaxService parseService;
    private final ISpoofaxInputUnitService inputService;

    @Inject public WhitespaceFragmentParser(ISpoofaxSyntaxService parseService, ISpoofaxInputUnitService inputService) {
        this.parseService = parseService;
        this.inputService = inputService;
    }

    public ISpoofaxParseUnit parse(IFragment fragment, ILanguageImpl language, @Nullable ILanguageImpl dialect)
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
        // TODO: support dialects
        ISpoofaxInputUnit input = inputService.inputUnit(fragment.getResource(), fragmentText, language, dialect);
        return parseService.parse(input);
    }
}
