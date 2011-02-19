package org.strategoxt.imp.testing;

import static org.spoofax.interpreter.core.Tools.asJavaString;
import static org.spoofax.interpreter.core.Tools.isTermAppl;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getTokenizer;
import static org.spoofax.terms.Term.termAt;
import static org.spoofax.terms.Term.tryGetConstructor;

import java.io.IOException;

import org.eclipse.imp.language.Language;
import org.eclipse.imp.language.LanguageRegistry;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.Tokenizer;
import org.spoofax.jsglr.shared.BadTokenException;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.jsglr.shared.TokenExpectedException;
import org.spoofax.terms.StrategoListIterator;
import org.spoofax.terms.TermTransformer;
import org.spoofax.terms.attachments.ParentTermFactory;
import org.strategoxt.imp.runtime.Debug;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
import org.strategoxt.imp.runtime.parser.JSGLRI;

public class SpoofaxTestingJSGLRI extends JSGLRI {
	
	private static final int PARSE_TIMEOUT = 20 * 1000;
	
	private static final IStrategoConstructor STRING_3 =
		Environment.getTermFactory().makeConstructor("string", 3);
	
	private static final IStrategoConstructor ERROR_1 =
		Environment.getTermFactory().makeConstructor("Error", 1);
	
	private static final IStrategoConstructor LANGUAGE_1 =
		Environment.getTermFactory().makeConstructor("Language", 1);
	
	private final CachedFragmentParser fragmentParser = new CachedFragmentParser();

	public SpoofaxTestingJSGLRI(JSGLRI template) {
		super(template.getParseTable(), template.getStartSymbol(), template.getController());
		setTimeout(PARSE_TIMEOUT);
		setUseRecovery(true);
	}
	
	@Override
	protected IStrategoTerm doParse(String input, String filename)
			throws TokenExpectedException, BadTokenException, SGLRException,
			IOException {

		IStrategoTerm ast = super.doParse(input, filename);
		return parseTestedFragments(ast);
	}

	private IStrategoTerm parseTestedFragments(final IStrategoTerm root) {
		final Tokenizer oldTokenizer = (Tokenizer) getTokenizer(root);
		final Retokenizer retokenizer = new Retokenizer(oldTokenizer);
		final ITermFactory nonParentFactory = Environment.getTermFactory();
		final ITermFactory factory = new ParentTermFactory(nonParentFactory);
		final CachedFragmentParser testedParser = getFragmentParser(root);
		assert !(nonParentFactory instanceof ParentTermFactory);
		if (testedParser == null || !testedParser.isInitialized())
			return root;
		
		IStrategoTerm result = new TermTransformer(factory, true) {
			@Override
			public IStrategoTerm preTransform(IStrategoTerm term) {
				if (tryGetConstructor(term) == STRING_3) {
					IStrategoTerm fragment = termAt(term, 1);
					retokenizer.copyTokensUpToIndex(getLeftToken(fragment).getIndex() - 1);
					try {
						IStrategoTerm parsed = testedParser.parseCached(oldTokenizer, fragment);
						int oldFragmentEndIndex = getRightToken(fragment).getIndex();
						retokenizer.copyTokensFromFragment(fragment, parsed,
								getLeftToken(fragment).getStartOffset(), getRightToken(fragment).getEndOffset());
						if (!testedParser.isLastSyntaxCorrect())
							parsed = factory.makeAppl(ERROR_1, parsed);
						term = factory.annotateTerm(term, nonParentFactory.makeList(parsed));
						retokenizer.skipTokensUpToIndex(oldFragmentEndIndex);
					} catch (IOException e) {
						Debug.log("Could not parse tested code fragment", e);
					} catch (SGLRException e) {
						Debug.log("Could not parse tested code fragment", e);
					} catch (RuntimeException e) {
						Environment.logException("Could not parse tested code fragment", e);
					}
				}
				return term;
			}
		}.transform(root);
		retokenizer.copyTokensAfterFragments();
		retokenizer.getTokenizer().setAst(root);
		retokenizer.getTokenizer().initAstNodeBinding();
		return result;
	}
	
	private CachedFragmentParser getFragmentParser(IStrategoTerm root) {
		Language language = getLanguage(root);
		if (language == null) return null;
		Descriptor descriptor = Environment.getDescriptor(language);
		fragmentParser.configure(descriptor, getController().getRelativePath(), getController().getProject());
		return fragmentParser;
	}

	private Language getLanguage(IStrategoTerm root) {
		if (isTermAppl(root) && "EmptyFile".equals(((IStrategoAppl) root).getName()))
			return null;
		IStrategoList headers = termAt(root, 0);
		for (IStrategoTerm header : StrategoListIterator.iterable(headers)) {
			if (tryGetConstructor(header) == LANGUAGE_1) {
				IStrategoString name = termAt(header, 0);
				return LanguageRegistry.findLanguage(asJavaString(name));
			}
		}
		return null;
	}

}
