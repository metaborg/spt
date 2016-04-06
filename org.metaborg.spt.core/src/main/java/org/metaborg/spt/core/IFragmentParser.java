package org.metaborg.spt.core;

import javax.annotation.Nullable;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

/**
 * An IFragmentParser can parse an IFragment, and ensures that the offsets of the parse result match with the offsets of
 * the IFragment.
 * 
 * This is important, to ensure that selections resolve to the correct AST nodes in the parse result, and for error
 * reporting.
 */
public interface IFragmentParser {

    /**
     * Parse the fragment, and ensure the offsets of the parse result match with the fragment.
     * 
     * @param fragment
     *            the IFragment to parse.
     * @param fragmentLanguage
     *            the language to parse the fragment's text with.
     * @param dialect
     *            TODO I don't know what it does, but it's cool and needs to be added. For now just pass null if you
     *            also don't know what it does.
     * @return the parse result.
     */
    public ISpoofaxParseUnit parse(IFragment fragment, ILanguageImpl fragmentLanguage, @Nullable ILanguageImpl dialect)
        throws ParseException;
}
