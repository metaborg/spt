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
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.syntax.ParseResult;
import org.metaborg.spoofax.core.tracing.ISpoofaxTracingService;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.common.collect.Iterables;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * Analyzes the given language's ast node in a new context.
 * Returns (analyzed ast, errors, warnings, notes)
 */
public class analyze_fragment_0_2 extends Strategy {
	private static final ILogger logger = LoggerUtils.logger(analyze_fragment_0_2.class);
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
		final IProject project = injector.getInstance(IProjectService.class).get(srcfile);
		
		try {
		    final IContextService contextService = injector.getInstance(IContextService.class);
		    
			// let Spoofax Core analyze the AST
			final IAnalysisService<IStrategoTerm, IStrategoTerm> analyzer = 
				injector.getInstance(Key.get(new TypeLiteral<IAnalysisService<IStrategoTerm, IStrategoTerm>>(){}));
			// FIXME: this is a rather hacky way to get the parsed AST into a ParseResult
			final ISpoofaxParseUnit parseResult = new ISpoofaxParseUnit("", ast, srcfile, Iterables2.<IMessage>empty(), -1, impl, null, null);
			final ITemporaryContext targetLanguageContext = contextService.getTemporary(metaborgContext.location(), project, impl);
            // HACK: setting the temporary context as context object, so that subsequent steps such as reference resolution can use the context.
            context.setContextObject(targetLanguageContext);
            final AnalysisResult<IStrategoTerm, IStrategoTerm> analysisResult = analyzer.analyze(Iterables2.singleton(parseResult), targetLanguageContext);

			// record the resulting AST and the errors, warnings, and notes
			final ISpoofaxAnalyzeUnit result = analysisResult.fileResults.iterator().next();
			analyzedAst = result.result;
			if (analyzedAst == null) {
				logger.debug("The analysis failed: {}", result);
				throw new MetaborgRuntimeException("The analysis of the fragment failed. Check the error log.");
			}
			final ISpoofaxTracingService tracing = injector.getInstance(ISpoofaxTracingService.class);
			for (IMessage message : result.messages) {
				// turn message into a (term, message) tuple
				final IStrategoTerm messageTerm;
				if (message.region() == null) {
					logger.debug("The analysis produced a message we can't pin to an AST node: \n{}", message.message());
					messageTerm = termFactory.makeTuple(analyzedAst, termFactory.makeString(message.message()));
				} else {
				    final Iterable<IStrategoTerm> markedTerms = tracing.fragments(result, message.region());
					if (Iterables.isEmpty(markedTerms)) {
						logger.debug("The analysis produced a message on region ({},{}) which can not be resolved.", message.region().startOffset(), message.region().endOffset());
						messageTerm = termFactory.makeTuple(analyzedAst, termFactory.makeString(message.message()));
					} else {
					    final IStrategoTerm markedTerm = Iterables.get(markedTerms, 0);
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
