package org.metaborg.spt.core.run;

import java.util.Collection;

import javax.annotation.Nullable;

import org.metaborg.core.analysis.AnalysisException;
import org.metaborg.core.context.ContextException;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.context.ITemporaryContext;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.ITestCase;
import org.metaborg.mbt.core.model.expectations.MessageUtil;
import org.metaborg.mbt.core.run.IFragmentParserConfig;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalysisService;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.SPTUtil;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;

import com.google.inject.Inject;

/**
 * A Spoofax specific utility class with some methods to deal with fragments in test expectations.
 * 
 * Obtain an instance through Guice.
 */
public class FragmentUtil {

    public static final String TO_PART_CONS = "ToPart";

    private final ISpoofaxFragmentParser fragmentParser;
    private final ILanguageService langService;
    private final ISpoofaxTracingService traceService;
    private final ISpoofaxAnalysisService analysisService;
    private final IContextService contextService;

    @Inject public FragmentUtil(ISpoofaxFragmentParser fragmentParser, ILanguageService langService,
        ISpoofaxTracingService traceService, ISpoofaxAnalysisService analysisService, IContextService contextService) {
        this.fragmentParser = fragmentParser;
        this.langService = langService;
        this.traceService = traceService;
        this.analysisService = analysisService;
        this.contextService = contextService;
    }

    /**
     * Check if this term is a ToPart.
     * 
     * @param toPart
     *            the term to check.
     * @return true iff this is a ToPart term that we know how to process.
     */
    public static boolean checkToPart(IStrategoTerm toPart) {
        final String cons = SPTUtil.consName(toPart);
        if(TO_PART_CONS.equals(cons)) {
            // ToPart(optional_lang_name, open_marker, fragment, close_marker)
            if(toPart.getSubtermCount() != 4) {
                return false;
            }

            // check the Option for the language name
            final IStrategoTerm optLang = getToPartOptLangTerm(toPart);
            if(!SPTUtil.checkOption(optLang)) {
                return false;
            }
            final IStrategoTerm langName = SPTUtil.getOptionValue(optLang);
            if(langName != null && !Term.isTermString(langName)) {
                return false;
            }

            // check the open marker
            if(!Term.isTermString(getToPartOpenMarkerTerm(toPart))) {
                return false;
            }

            // check the fragment
            if(!checkFragment(getToPartFragmentTerm(toPart))) {
                return false;
            }

            // check the close marker
            if(!Term.isTermString(getToPartCloseMarkerTerm(toPart))) {
                return false;
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if the given term is a proper Framgent term.
     * 
     * @param fragment
     *            the term to check.
     * @return true iff this is a Fragment term that we can handle.
     */
    public static boolean checkFragment(IStrategoTerm fragment) {
        // Fragment(<String>, TailPart)
        final String cons = SPTUtil.consName(fragment);
        if(!SPTUtil.FRAGMENT_CONS.equals(cons) || fragment.getSubtermCount() != 2) {
            return false;
        }
        if(!Term.isTermString(fragment.getSubterm(0))) {
            return false;
        }
        if(!checkTailPart(fragment.getSubterm(1))) {
            return false;
        }
        return true;
    }

    /**
     * Check if the given term is a proper TailPart term.
     * 
     * @param tailPart
     *            the term to check.
     * @return true iff this is a TailPart term that we can handle.
     */
    public static boolean checkTailPart(IStrategoTerm tailPart) {
        // Done or More(Selection(open_bracket, <String>, close_bracket), <String>, TailPart)
        final String cons = SPTUtil.consName(tailPart);
        switch(cons) {
            case SPTUtil.TAILPART_DONE_CONS:
                // Done()
                return tailPart.getSubtermCount() == 0;
            case SPTUtil.TAILPART_MORE_CONS:
                // More(Selection(marker, string, marker), string, TailPart)
                if(tailPart.getSubtermCount() != 3) {
                    return false;
                }

                // check Selection(open marker, string, close marker)
                final IStrategoTerm selection = tailPart.getSubterm(0);
                if(!SPTUtil.SELECTION_CONS.equals(SPTUtil.consName(selection)) || selection.getSubtermCount() != 3) {
                    return false;
                }
                if(!Term.isTermString(selection.getSubterm(0)) || !Term.isTermString(selection.getSubterm(1))
                    || !Term.isTermString(selection.getSubterm(2))) {
                    return false;
                }

                // check string part
                if(!Term.isTermString(tailPart.getSubterm(1))) {
                    return false;
                }

                // check recursive tail part
                if(!checkTailPart(tailPart.getSubterm(2))) {
                    return false;
                }

                return true;
            default:
                return false;
        }
    }

    // Get the AST node corresponding to the optional language name of a ToPart.
    private static IStrategoTerm getToPartOptLangTerm(IStrategoTerm toPart) {
        return toPart.getSubterm(0);
    }

    // Get the AST node corresponding to the open marker of a ToPart.
    private static IStrategoTerm getToPartOpenMarkerTerm(IStrategoTerm toPart) {
        return toPart.getSubterm(1);
    }

    // Get the AST node corresponding to the fragment of a ToPart.
    private static IStrategoTerm getToPartFragmentTerm(IStrategoTerm toPart) {
        return toPart.getSubterm(2);
    }

    // Get the AST node corresponding to the close marker of a ToPart.
    private static IStrategoTerm getToPartCloseMarkerTerm(IStrategoTerm toPart) {
        return toPart.getSubterm(3);
    }

    /**
     * Get the name of the language of the given ToPart.
     */
    public static @Nullable String toPartLangName(IStrategoTerm toPart) {
        final IStrategoTerm langOption = SPTUtil.getOptionValue(getToPartOptLangTerm(toPart));

        return langOption == null ? null : Term.asJavaString(langOption);
    }

    /**
     * Get the AST node of the fragment of the given ToPart.
     */
    public static IStrategoTerm toPartFragment(IStrategoTerm toPart) {
        return getToPartFragmentTerm(toPart);
    }

    /**
     * Try to get the region of the language name of the given ToPart.
     */
    public @Nullable ISourceRegion toPartLangNameRegion(IStrategoTerm toPart) {
        final IStrategoTerm langNameTerm = getToPartOptLangTerm(toPart);
        final ISourceLocation loc = traceService.location(langNameTerm);
        return loc == null ? null : loc.region();
    }

    /**
     * Get the language with the given name.
     * 
     * Collects messages if things go wrong.
     * 
     * @param langName
     *            the name of the language to get.
     * @param messages
     *            where we collect messages.
     * @param test
     *            the test for which you want the language (for error message locations).
     * @return the language, or null if things went wrong.
     */
    public @Nullable ILanguage getLanguage(String langName, Collection<IMessage> messages, ITestCase test) {
        ILanguage lang = langName == null ? null : langService.getLanguage(langName);
        if(lang == null) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), test.getDescriptionRegion(),
                "Could not find the language " + langName, null));
            return null;
        }
        return lang;
    }

    /**
     * Tries to parse the given fragment with the language registered for the given name.
     * 
     * Will collect messages if things go wrong.
     * 
     * @param fragment
     *            the fragment to parse.
     * @param langName
     *            the name of the language to parse it with.
     * @param messages
     *            where we collect messages.
     * @param test
     *            the test that contained the fragment.
     * @param fragmentConfig
     *            the config for the fragment parser.
     * 
     * @return the result of parsing the fragment. May be null if parsing failed.
     */
    public @Nullable ISpoofaxParseUnit parseFragment(IFragment fragment, String langName, Collection<IMessage> messages,
        ITestCase test, @Nullable IFragmentParserConfig fragmentConfig) {
        ILanguage lang = getLanguage(langName, messages, test);
        if(lang == null) {
            return null;
        }
        return parseFragment(fragment, lang.activeImpl(), messages, test, fragmentConfig);
    }

    /**
     * Tries to parse the given fragment with the given language.
     * 
     * Will collect messages if things go wrong.
     * 
     * @param fragment
     *            the fragment to parse.
     * @param lang
     *            the language to parse it with.
     * @param messages
     *            where we collect messages.
     * @param test
     *            the test that contained the fragment.
     * @param fragmentConfig
     *            the config for the fragment parser.
     * 
     * @return the result of parsing the fragment. May be null if parsing failed.
     */
    public @Nullable ISpoofaxParseUnit parseFragment(IFragment fragment, ILanguageImpl lang,
        Collection<IMessage> messages, ITestCase test, @Nullable IFragmentParserConfig fragmentConfig) {
        // parse the fragment
        final ISpoofaxParseUnit parsedFragment;
        try {
            // TODO: would we ever need to use a dialect?
            parsedFragment = fragmentParser.parse(fragment, lang, null, fragmentConfig);
        } catch(ParseException e) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), fragment.getRegion(),
                "Unable to parse the fragment due to an exception", e));
            return null;
        }
        if(!parsedFragment.success()) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), fragment.getRegion(),
                "Expected the fragment to parse", null));
            // propagate messages
            MessageUtil.propagateMessages(parsedFragment.messages(), messages, fragment.getRegion(),
                fragment.getRegion());
            return null;
        }
        return parsedFragment;
    }

    /**
     * Tries to analyze the given fragment with the given name.
     * 
     * Will collect messages if things go wrong.
     * 
     * @param fragment
     *            the fragment to parse.
     * @param langName
     *            the language to analyze it with.
     * @param messages
     *            where we collect messages.
     * @param test
     *            the test that contained the fragment.
     * @param fragmentConfig
     *            the config for the fragment parser.
     * 
     * @return the result of analyzing the fragment.
     */
    public @Nullable ISpoofaxAnalyzeUnit analyzeFragment(IFragment fragment, ILanguageImpl lang,
        Collection<IMessage> messages, ITestCase test, @Nullable IFragmentParserConfig fragmentConfig) {
        ISpoofaxParseUnit p = parseFragment(fragment, lang, messages, test, fragmentConfig);
        if(p == null) {
            return null;
        }

        try(ITemporaryContext ctx = contextService.getTemporary(test.getResource(), test.getProject(), lang)) {
            ISpoofaxAnalyzeUnit a = analysisService.analyze(p, ctx).result();
            if(a.success() && a.hasAst()) {
                return a;
            } else if(!a.success()) {
                messages.add(MessageFactory.newAnalysisError(test.getResource(), fragment.getRegion(),
                    "Analysis of the fragment failed.", null));
            } else if(!a.hasAst()) {
                messages.add(MessageFactory.newAnalysisError(test.getResource(), fragment.getRegion(),
                    "Analysis did not return an AST.", null));
            }
        } catch(ContextException e) {
            // not much we can do without a context
            messages.add(MessageFactory.newAnalysisError(test.getResource(), fragment.getRegion(),
                "Failed to create a context to analyze the fragment.", e));
        } catch(AnalysisException e) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), fragment.getRegion(),
                "Analysis of the fragment failed with an unexpected exception.", e));
        }
        return null;
    }

    /**
     * Tries to analyze the given fragment with the given name.
     * 
     * Will collect messages if things go wrong.
     * 
     * @param fragment
     *            the fragment to parse.
     * @param langName
     *            the language to analyze it with.
     * @param messages
     *            where we collect messages.
     * @param test
     *            the test that contained the fragment.
     * @param fragmentConfig
     *            the config for the fragment parser.
     * 
     * @return the result of analyzing the fragment.
     */
    public @Nullable ISpoofaxAnalyzeUnit analyzeFragment(IFragment fragment, String langName,
        Collection<IMessage> messages, ITestCase test, @Nullable IFragmentParserConfig fragmentConfig) {
        ILanguage lang = getLanguage(langName, messages, test);
        if(lang == null) {
            return null;
        } else {
            return analyzeFragment(fragment, lang.activeImpl(), messages, test, fragmentConfig);
        }
    }
}
