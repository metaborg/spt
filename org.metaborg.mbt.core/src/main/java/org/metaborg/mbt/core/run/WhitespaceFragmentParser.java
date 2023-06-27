package org.metaborg.mbt.core.run;

import javax.annotation.Nullable;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.IInputUnit;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.core.syntax.ISyntaxService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.core.unit.IInputUnitService;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.IFragment.FragmentPiece;

import javax.inject.Inject;

/**
 * Parser for fragments of non-layout sensitive languages.
 * 
 * Ensures the correct offsets of the parse result by adding whitespace to replace the SPT specific characters.
 */
public class WhitespaceFragmentParser<I extends IInputUnit, P extends IParseUnit> implements IFragmentParser<P> {

    private final IInputUnitService<I> inputService;
    private final ISyntaxService<I, P> parseService;

    @Inject public WhitespaceFragmentParser(IInputUnitService<I> inputService, ISyntaxService<I, P> parseService) {
        this.inputService = inputService;
        this.parseService = parseService;
    }

    @Override public P parse(IFragment fragment, ILanguageImpl language, @Nullable ILanguageImpl dialect,
        @Nullable IFragmentParserConfig config) throws ParseException {

        String fragmentText = getWhitespacedFragmentText(fragment);

        // now we can parse the fragment
        I input = inputService.inputUnit(fragment.getResource(), fragmentText, language, dialect);

        return parse(input);
    }

    protected String getWhitespacedFragmentText(IFragment fragment) {
        StringBuilder fragmentTextBuilder = new StringBuilder();
        for(FragmentPiece piece : fragment.getText()) {
            // add whitespace to get the character offset of this piece right
            for(int i = fragmentTextBuilder.length(); i < piece.startOffset; i++) {
                fragmentTextBuilder.append(" ");
            }
            // add the actual piece of program text from the fragment
            fragmentTextBuilder.append(piece.text);
        }
        return fragmentTextBuilder.toString();
    }

    protected P parse(I input) throws ParseException {
        return parseService.parse(input);
    }
}
