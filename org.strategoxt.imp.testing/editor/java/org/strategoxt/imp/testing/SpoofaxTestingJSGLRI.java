package org.strategoxt.imp.testing;

import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getTokenizer;
import static org.spoofax.terms.Term.termAt;
import static org.spoofax.terms.Term.tryGetConstructor;

import java.io.IOException;

import org.eclipse.imp.language.Language;
import org.eclipse.imp.language.LanguageRegistry;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.Tokenizer;
import org.spoofax.jsglr.shared.BadTokenException;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.jsglr.shared.TokenExpectedException;
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
	
	private final CachedFragmentParser fragmentParser = new CachedFragmentParser();

	public SpoofaxTestingJSGLRI(JSGLRI template) {
		super(template.getParseTable(), template.getStartSymbol(), template.getController());
		setTimeout(PARSE_TIMEOUT);
		setUseRecovery(true);
	}
	
	protected IStrategoTerm doParse(String input, String filename)
			throws TokenExpectedException, BadTokenException, SGLRException,
			IOException {

		IStrategoTerm ast = super.doParse(input, filename);
		return parseTestedFragments(ast);
	}

	private IStrategoTerm parseTestedFragments(final IStrategoTerm root) {
		final Tokenizer oldTokenizer = (Tokenizer) getTokenizer(root);
		final Retokenizer retokenizer = new Retokenizer(oldTokenizer);
		final ITermFactory factory = new ParentTermFactory(Environment.getTermFactory());
		final CachedFragmentParser testedParser = getFragmentParser();
		if (!testedParser.isInitialized())
			return root;
		
		IStrategoTerm result = new TermTransformer(factory, true) {
			@Override
			public IStrategoTerm preTransform(IStrategoTerm term) {
				if (tryGetConstructor(term) == STRING_3) {
					IStrategoTerm fragment = termAt(term, 1);
					retokenizer.copyTokensUpToIndex(getLeftToken(fragment).getIndex() - 1);
					retokenizer.skipTokensUpToIndex(getRightToken(fragment).getIndex());
					try {
						IStrategoTerm parsed = testedParser.parseCached(oldTokenizer, fragment);
						retokenizer.copyTokensFromFragment(fragment, parsed,
								getLeftToken(fragment).getStartOffset(), getRightToken(fragment).getEndOffset());
						// term = factory.makeAppl(STRING_3, termAt(term, 0), termAt(term, 1), parsed);
						if (!getTokenizer(parsed).isSyntaxCorrect())
							parsed = factory.makeAppl(ERROR_1, parsed);
						term = factory.annotateTerm(term, factory.makeList(parsed));
					} catch (Exception e) {
						// Forget it, don't parse then
						// Environment.logWarning("Failure parsing tested fragment", e);
						// TODO: handle failure?
						Debug.log("Could not parse tested code fragment", e);
					}
				}
				return term;
			}
		}.transform(root);
		retokenizer.copyTokensAfterFragments();
		retokenizer.getTokenizer().setAst(root);
		return result;
	}
	
	private CachedFragmentParser getFragmentParser() {
		// XXX: find name of currently tested language
		String languageName = "TestingTesting";
		Language language = LanguageRegistry.findLanguage(languageName);
		Descriptor descriptor = Environment.getDescriptor(language);
		fragmentParser.setDescriptor(descriptor);
		return fragmentParser;
	}

}
