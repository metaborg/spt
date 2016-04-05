package org.metaborg.spt.core;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.syntax.ParseResult;
import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * A Fragment represents a piece of code within an SPT test suite, written in another language.
 * 
 * Examples are the fragment of a test case, or the fragment of a 'parse to' test expectation. The former would be
 * written in the language under test, the latter possibly in another language.
 * 
 * The problem with Fragments, is that the text has to be extracted from the original SPT specification, and parsed with
 * another language, while keeping the offsets of the nodes in the returned parse result correct within the larger
 * context of the SPT specification. Therefore the fragments should be parsed using the {@link #parse(ILanguageImpl)}
 * method.
 */
public interface IFragment {

    /**
     * The SPT Fragment node representing the fragment.
     */
    public IStrategoTerm getSPTNode();

    /**
     * The selections of this fragment.
     */
    public Iterable<ISourceRegion> getSelections();

    /**
     * Parse the fragment with the given language.
     * 
     * @param language
     *            the language to parse the fragment with.
     * @return the result of parsing the fragment.
     */
    public ParseResult<IStrategoTerm> parse(ILanguageImpl language);

}
