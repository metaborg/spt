package org.metaborg.spt.core.expectations;

import java.util.Collection;

import javax.annotation.Nullable;

import org.metaborg.core.analysis.AnalysisException;
import org.metaborg.core.context.ContextException;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.context.ITemporaryContext;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageFactory;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalysisService;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.IFragment;
import org.metaborg.spt.core.IFragmentBuilder;
import org.metaborg.spt.core.IFragmentParser;
import org.metaborg.spt.core.ITestCase;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;

import com.google.inject.Inject;

public class FragmentUtil {

    private final IFragmentParser fragmentParser;
    private final IFragmentBuilder fragmentBuilder;
    private final ILanguageService langService;
    private final ISpoofaxTracingService traceService;
    private final ISpoofaxAnalysisService analysisService;
    private final IContextService contextService;

    @Inject public FragmentUtil(IFragmentParser fragmentParser, IFragmentBuilder fragmentBuilder,
        ILanguageService langService, ISpoofaxTracingService traceService, ISpoofaxAnalysisService analysisService,
        IContextService contextService) {
        this.fragmentParser = fragmentParser;
        this.fragmentBuilder = fragmentBuilder;
        this.langService = langService;
        this.traceService = traceService;
        this.analysisService = analysisService;
        this.contextService = contextService;
    }

    /**
     * Parse a ToPart(languageName, openMarker, fragment, closeMarker).
     * 
     * @param toPart
     *            the ToPart to parse.
     * @param messages
     *            we add any error messages to this collection.
     * @param test
     *            required to create any error messages.
     * @return the result of parsing the fragment of the ToPart, or null if the parsing failed for whatever reason.
     */
    public ISpoofaxParseUnit parseToPart(IStrategoTerm toPart, Collection<IMessage> messages, ITestCase test) {
        // It's a ToPart(languageName, openMarker, fragment, closeMarker)
        IStrategoTerm langTerm = toPart.getSubterm(0);
        IStrategoTerm fragmentTerm = toPart.getSubterm(2);
        String langName = Term.asJavaString(langTerm);
        ILanguage lang = langService.getLanguage(langName);
        if(lang == null) {
            ISourceLocation loc = traceService.location(langTerm);
            ISourceRegion region = loc == null ? test.getDescriptionRegion() : loc.region();
            messages.add(MessageFactory.newAnalysisError(test.getResource(), region,
                "Could not find the language " + langName, null));
            return null;
        }

        // parse the fragment
        IFragment fragment = fragmentBuilder.withProject(test.getProject()).withResource(test.getResource())
            .withFragment(fragmentTerm).build();
        final ISpoofaxParseUnit parsedFragment;
        try {
            // TODO: would we ever need to use a dialect?
            parsedFragment = fragmentParser.parse(fragment, lang.activeImpl(), null);
        } catch(ParseException e) {
            ISourceLocation loc = traceService.location(fragmentTerm);
            ISourceRegion region = loc == null ? test.getDescriptionRegion() : loc.region();
            messages.add(MessageFactory.newAnalysisError(test.getResource(), region,
                "Unable to parse the fragment due to an exception", e));
            return null;
        }
        if(!parsedFragment.success()) {
            ISourceLocation loc = traceService.location(fragmentTerm);
            ISourceRegion region = loc == null ? test.getDescriptionRegion() : loc.region();
            messages.add(
                MessageFactory.newAnalysisError(test.getResource(), region, "Expected the fragment to parse", null));
            // propagate messages
            MessageUtil.propagateMessages(parsedFragment.messages(), messages, region);
            return null;
        }
        return parsedFragment;
    }

    /**
     * Analyze a ToPart(languageName, openMarker, fragment, closeMarker).
     * 
     * @param toPart
     *            the ToPart to parse and analyze.
     * @param messages
     *            we add any error messages to this collection.
     * @param test
     *            required to create any error messages.
     * @return the result of analyzing the fragment of the ToPart, or null if the parsing or analysis failed for
     *         whatever reason.
     */
    public ISpoofaxAnalyzeUnit analyzeToPart(IStrategoTerm toPart, Collection<IMessage> messages, ITestCase test) {
        ISpoofaxParseUnit p = parseToPart(toPart, messages, test);
        if(p == null) {
            return null;
        }
        // It's a ToPart(languageName, openMarker, fragment, closeMarker)
        IStrategoTerm langTerm = toPart.getSubterm(0);
        IStrategoTerm fragmentTerm = toPart.getSubterm(2);
        ISourceLocation loc = traceService.location(fragmentTerm);
        ISourceRegion region = loc == null ? test.getDescriptionRegion() : loc.region();

        String langName = Term.asJavaString(langTerm);
        ILanguage lang = langService.getLanguage(langName);
        try(ITemporaryContext ctx =
            contextService.getTemporary(test.getResource(), test.getProject(), lang.activeImpl())) {
            ISpoofaxAnalyzeUnit a = analysisService.analyze(p, ctx).result();
            if(a.success() && a.hasAst()) {
                return a;
            } else if(!a.success()) {
                messages.add(MessageFactory.newAnalysisError(test.getResource(), region,
                    "Analysis of the fragment failed.", null));
            } else if(!a.hasAst()) {
                messages.add(MessageFactory.newAnalysisError(test.getResource(), region,
                    "Analysis did not return an AST.", null));
            }
        } catch(ContextException e) {
            // not much we can do without a context
            messages.add(MessageFactory.newAnalysisError(test.getResource(), region,
                "Failed to create a context to analyze the fragment.", e));
        } catch(AnalysisException e) {
            messages.add(MessageFactory.newAnalysisError(test.getResource(), region,
                "Analysis of the fragment failed with an unexpected exception.", e));
        }
        return null;
    }

    /**
     * Return the ILanguage that was indicated to be used by this ToPart(languageName, openMarker, fragment,
     * closeMarker) term.
     * 
     * @param toPart
     *            the ToPart term.
     * @return the ILanguage, or null if there was none.
     */
    public @Nullable ILanguage langFromToPart(IStrategoTerm toPart) {
        // It's a ToPart(languageName, openMarker, fragment, closeMarker)
        IStrategoTerm langTerm = toPart.getSubterm(0);
        String langName = Term.asJavaString(langTerm);
        return langService.getLanguage(langName);
    }
}
