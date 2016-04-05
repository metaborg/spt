package org.metaborg.spt.core;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.core.syntax.ParseResult;
import org.spoofax.interpreter.terms.IStrategoTerm;

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
     * @return the parse result.
     */
    public ParseResult<IStrategoTerm> parse(IFragment fragment, ILanguageImpl fragmentLanguage) throws ParseException;
}
