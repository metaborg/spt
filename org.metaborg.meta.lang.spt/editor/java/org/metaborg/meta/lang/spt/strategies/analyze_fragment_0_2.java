package org.metaborg.meta.lang.spt.strategies;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.analysis.AnalysisFileResult;
import org.metaborg.core.analysis.AnalysisResult;
import org.metaborg.core.analysis.IAnalysisService;
import org.metaborg.core.context.IContext;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.context.ITemporaryContext;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.syntax.ParseResult;
import org.metaborg.util.iterators.Iterables2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * Analyzes the given language's ast node in a new context.
 * Returns (analyzed ast, errors, warnings, notes)
 */
public class analyze_fragment_0_2 extends Strategy {
	private static final Logger logger = LoggerFactory.getLogger(analyze_fragment_0_2.class);
	public static final analyze_fragment_0_2 instance = new analyze_fragment_0_2();

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm ast,
			IStrategoTerm filePath, IStrategoTerm languageName) {
		// the elements to return
		final IStrategoTerm analyzedAst;
		final Collection<IStrategoTerm> errors = new ArrayList<IStrategoTerm>();
		final Collection<IStrategoTerm> warnings = new ArrayList<IStrategoTerm>();
		final Collection<IStrategoTerm> notes = new ArrayList<IStrategoTerm>();

		final IContext metaborgContext = (IContext) context.contextObject();
		final Injector injector = metaborgContext.injector();
		final ITermFactory termFactory = context.getFactory();

		// the language under test
		final ILanguage lang = injector.getInstance(ILanguageService.class)
				.getLanguage(Tools.asJavaString(languageName));
		final ILanguageImpl impl = lang.activeImpl();

		// the resource corresponding to the fragments (i.e. the .spt file of this test case's suite)
		final FileObject srcfile = injector.getInstance(IResourceService.class).resolve(Tools.asJavaString(filePath));
		
		try {
		    final IContextService contextService = injector.getInstance(IContextService.class);
		    
			// let Spoofax Core analyze the AST
			final IAnalysisService<IStrategoTerm, IStrategoTerm> analyzer = 
				injector.getInstance(Key.get(new TypeLiteral<IAnalysisService<IStrategoTerm, IStrategoTerm>>(){}));
			// FIXME: this is a rather hacky way to get the parsed AST into a ParseResult
			final ParseResult<IStrategoTerm> parseResult = new ParseResult<IStrategoTerm>("", ast, srcfile, Iterables2.<IMessage>empty(), -1, impl, null, null);
			final AnalysisResult<IStrategoTerm, IStrategoTerm> analysisResult;
            try(final ITemporaryContext targetLanguageContext = contextService.getTemporary(metaborgContext, impl)) {
                analysisResult = analyzer.analyze(Iterables2.singleton(parseResult), targetLanguageContext);
            }

			// record the resulting AST and the errors, warnings, and notes
			final AnalysisFileResult<IStrategoTerm, IStrategoTerm> result = analysisResult.fileResults.iterator().next();
			analyzedAst = result.result;
			if (analyzedAst == null) {
				logger.error("The analysis failed: {}", result);
				throw new MetaborgRuntimeException("The analysis of the fragment failed. Check the error log.");
			}
			for (IMessage message : result.messages) {
				// turn message into a (term, message) tuple
				final IStrategoTerm messageTerm;
				if (message.region() == null) {
					logger.error("The analysis produced a message we can't pin to an AST node: \n{}", message.message());
					messageTerm = termFactory.makeTuple(analyzedAst, termFactory.makeString(message.message()));
				} else {
					final IStrategoTerm markedTerm = SelectionFetcher.fetchOne(message.region(), analyzedAst);
					if (markedTerm == null) {
						logger.error("The analysis produced a message on region ({},{}) which can not be resolved.", message.region().startOffset(), message.region().endOffset());
						messageTerm = termFactory.makeTuple(analyzedAst, termFactory.makeString(message.message()));
					} else {
						messageTerm = termFactory.makeTuple(markedTerm, termFactory.makeString(message.message()));
					}
				}
				// add the message to the right list
				switch (message.severity()) {
				case ERROR :
					errors.add(messageTerm);
					break;
				case WARNING :
					warnings.add(messageTerm);
					break;
				case NOTE :
					notes.add(messageTerm);
					break;
				}
			}
		} catch (Exception e) {
			throw new MetaborgRuntimeException(e);
		}
		

		return termFactory.makeTuple(analyzedAst, termFactory.makeList(errors), termFactory.makeList(warnings), termFactory.makeList(notes));
	}
}
