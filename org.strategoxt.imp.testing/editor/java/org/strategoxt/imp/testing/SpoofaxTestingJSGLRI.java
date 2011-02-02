package org.strategoxt.imp.testing;

import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getTokenizer;
import static org.spoofax.terms.Term.termAt;
import static org.spoofax.terms.Term.tryGetConstructor;

import java.io.IOException;

import org.eclipse.imp.language.Language;
import org.eclipse.imp.language.LanguageRegistry;
import org.eclipse.imp.parser.IParseController;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.jsglr.client.imploder.Tokenizer;
import org.spoofax.jsglr.shared.BadTokenException;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.jsglr.shared.TokenExpectedException;
import org.spoofax.terms.TermTransformer;
import org.spoofax.terms.attachments.ParentTermFactory;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
import org.strategoxt.imp.runtime.dynamicloading.DynamicParseController;
import org.strategoxt.imp.runtime.parser.JSGLRI;
import org.strategoxt.imp.runtime.parser.SGLRParseController;

public class SpoofaxTestingJSGLRI extends JSGLRI {
	
	private static final int PARSE_TIMEOUT = 20 * 1000;
	
	private static final IStrategoConstructor STRING_3 =
		Environment.getTermFactory().makeConstructor("string", 3);

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
		final JSGLRI testedParser = tryGetTestedParser();
		
		IStrategoTerm result = new TermTransformer(factory, true) {
			@Override
			public IStrategoTerm preTransform(IStrategoTerm term) {
				if (tryGetConstructor(term) == STRING_3) {
					IStrategoTerm fragment = termAt(term, 1);
					retokenizer.copyTokensUpToIndex(getLeftToken(fragment).getIndex() - 1);
					retokenizer.skipTokensUpToIndex(getRightToken(fragment).getIndex());
					String fragmentInput = createTestFragmentString(oldTokenizer, fragment);
					try {
						IStrategoTerm parsed = testedParser.parse(fragmentInput, oldTokenizer.getFilename());
						retokenizer.copyTokensFromFragment(fragment, parsed, getLeftToken(fragment).getStartOffset());
						// term = factory.makeAppl(STRING_3, termAt(term, 0), termAt(term, 1), parsed);
						term = factory.annotateTerm(term, factory.makeList(parsed));
					} catch (Exception e) {
						// Forget it, don't parse then
						// Environment.logWarning("Failure parsing tested fragment", e);
						// TODO: handle failure?
					}
				}
				return term;
			}
		}.transform(root);
		retokenizer.copyTokensAfterFragments();
		retokenizer.getTokenizer().setAst(root);
		return result;
	}
	
	private JSGLRI tryGetTestedParser() {
		// TODO: find name of currently tested language
		String languageName = "TestingTesting";
		
		return tryGetParser(languageName);
	}

	private JSGLRI tryGetParser(String languageName) {
		try {
			Language language = LanguageRegistry.findLanguage(languageName);
			Descriptor descriptor = Environment.getDescriptor(language);
			if (descriptor == null) return null;
			IParseController controller;
			controller = descriptor.createService(IParseController.class, null);
			if (controller instanceof DynamicParseController)
				controller = ((DynamicParseController) controller).getWrapped();
			if (controller instanceof SGLRParseController) {
				JSGLRI parser = ((SGLRParseController) controller).getParser(); 
				JSGLRI result = new JSGLRI(parser.getParseTable(), parser.getStartSymbol());
				return result;
			}
		} catch (BadDescriptorException e) {
			Environment.logWarning("Could not load parser for " + languageName);
		} catch (RuntimeException e) {
			Environment.logWarning("Could not load parser for " + languageName);
		}
		return null;
	}

	private String createTestFragmentString(ITokenizer tokenizer, IStrategoTerm term) {
		int fragmentOffset = getLeftToken(term).getStartOffset();
		IToken endToken = getRightToken(term);
		StringBuilder result = new StringBuilder(tokenizer.toString(tokenizer.getTokenAt(0), endToken));
		for (int i = 0; i < fragmentOffset; i++) {
			switch (result.charAt(i)) {
				case ' ': case '\t': case '\r': case '\n':
					break;
				default:
					result.setCharAt(i, ' ');
			}
		}
		return result.toString();
	}

}
