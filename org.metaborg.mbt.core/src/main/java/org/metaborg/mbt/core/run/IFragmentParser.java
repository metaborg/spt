package org.metaborg.mbt.core.run;

import jakarta.annotation.Nullable;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.mbt.core.model.IFragment;

/**
 * An IFragmentParser can parse an IFragment, and ensures that the offsets of the parse result match with the offsets of
 * the IFragment.
 * 
 * This is important, to ensure that selections resolve to the correct AST nodes in the parse result, and for error
 * reporting.
 */
public interface IFragmentParser<P extends IParseUnit> {

    /**
     * Parses the given fragment using the specified language, dialect, and parser configuration.
     *
     * This ensures the offsets of the parse result match with the fragment.
     *
     * @param fragment the fragment to parse
     * @param language the language of the fragment
     * @param dialect the dialect of the language; or {@code null}
     * @param config the parser configuration; or {@code null}
     * @return the parse result
     * @throws ParseException an exception occurred during parsing
     */
    P parse(IFragment fragment, ILanguageImpl language, @Nullable ILanguageImpl dialect,
            @Nullable IFragmentParserConfig config) throws ParseException;
}
