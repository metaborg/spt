package org.metaborg.spt.core.run;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.run.IFragmentParser;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

import jakarta.annotation.Nullable;

/**
 * Type interface for an IFragmentParser that will parse the fragment with a Spoofax language.
 */
public interface ISpoofaxFragmentParser extends IFragmentParser<ISpoofaxParseUnit> {

    /**
     * Parses the given fragment using the specified language, dialect, and parser configuration.
     *
     * @param fragment the fragment to parse
     * @param language the language of the fragment
     * @param dialect the dialect of the language; or {@code null}
     * @param config the parser configuration; or {@code null}
     * @return the parse result
     * @throws ParseException an exception occurred during parsing
     */
    ISpoofaxParseUnit parse(
            IFragment fragment,
            ILanguageImpl language,
            @Nullable ILanguageImpl dialect,
            @Nullable ISpoofaxFragmentParserConfig config) throws ParseException;
}
