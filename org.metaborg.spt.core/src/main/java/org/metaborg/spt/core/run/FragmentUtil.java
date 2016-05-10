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
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalysisService;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;

import com.google.inject.Inject;

/**
 * A Spoofax specific utility class with some methods to deal with fragments in test expectations.
 * 
 * Obtain an instance through Guice.
 */
public class FragmentUtil {

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
     * Get the AST node of the fragment of the given ToPart.
     */
    public static IStrategoTerm toPartFragment(IStrategoTerm toPart) {
        return toPart.getSubterm(2);
    }

    /**
     * Get the name of the language of the given ToPart.
     */
    public static String toPartLangName(IStrategoTerm toPart) {
        return Term.asJavaString(toPart.getSubterm(0));
    }

    /**
     * Try to get the region of the language name of the given ToPart.
     */
    public @Nullable ISourceRegion toPartLangNameRegion(IStrategoTerm toPart) {
        final IStrategoTerm langNameTerm = toPart.getSubterm(0);
        ISourceLocation loc = traceService.location(langNameTerm);
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
     * 
     * @return the result of parsing the fragment. May be null if parsing failed.
     */
    public @Nullable ISpoofaxParseUnit parseFragment(IFragment fragment, String langName, Collection<IMessage> messages,
        ITestCase test) {
        ILanguage lang = getLanguage(langName, messages, test);
        if(lang == null) {
            return null;
        }
        return parseFragment(fragment, lang.activeImpl(), messages, test);
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
     * 
     * @return the result of parsing the fragment. May be null if parsing failed.
     */
    public @Nullable ISpoofaxParseUnit parseFragment(IFragment fragment, ILanguageImpl lang,
        Collection<IMessage> messages, ITestCase test) {
        // parse the fragment
        final ISpoofaxParseUnit parsedFragment;
        try {
            // TODO: would we ever need to use a dialect?
            parsedFragment = fragmentParser.parse(fragment, lang, null);
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
     * 
     * @return the result of analyzing the fragment.
     */
    public @Nullable ISpoofaxAnalyzeUnit analyzeFragment(IFragment fragment, String langName,
        Collection<IMessage> messages, ITestCase test) {
        ISpoofaxParseUnit p = parseFragment(fragment, langName, messages, test);
        if(p == null) {
            return null;
        }

        ILanguage lang = getLanguage(langName, messages, test);
        if(lang == null) {
            return null;
        }
        try(ITemporaryContext ctx =
            contextService.getTemporary(test.getResource(), test.getProject(), lang.activeImpl())) {
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
}
