package org.strategoxt.imp.testing;

import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getTokenizer;
import static org.spoofax.terms.Term.termAt;
import static org.spoofax.terms.Term.tryGetConstructor;
import static org.spoofax.terms.attachments.ParentAttachment.getParent;

import java.io.IOException;
import java.util.Map;

import org.eclipse.imp.parser.IParseController;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.jsglr.client.imploder.Token;
import org.spoofax.jsglr.shared.BadTokenException;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.jsglr.shared.TokenExpectedException;
import org.spoofax.terms.StrategoListIterator;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
import org.strategoxt.imp.runtime.dynamicloading.DynamicParseController;
import org.strategoxt.imp.runtime.parser.JSGLRI;
import org.strategoxt.imp.runtime.parser.SGLRParseController;
import org.strategoxt.lang.WeakValueHashMap;

/** 
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class CachedFragmentParser {

	private static final int FRAGMENT_PARSE_TIMEOUT = 3000;
	
	private static final IStrategoConstructor FAILS_0 =
		Environment.getTermFactory().makeConstructor("Fails", 0);
	
	private static final IStrategoConstructor FAILS_PARSING_0 =
		Environment.getTermFactory().makeConstructor("FailsParsing", 0);
	
	private final WeakValueHashMap<String, IStrategoTerm> failParseCache =
		new WeakValueHashMap<String, IStrategoTerm>();
	
	private final WeakValueHashMap<String, IStrategoTerm> successParseCache =
		new WeakValueHashMap<String, IStrategoTerm>();
	
	private Descriptor parseCacheDescriptor;
	
	private JSGLRI parser;

	public void setDescriptor(Descriptor descriptor) {
		if (parseCacheDescriptor != descriptor) {
			parseCacheDescriptor = descriptor;
			this.parser = getParser(descriptor);
			failParseCache.clear();
			successParseCache.clear();
		}
	}
	
	public boolean isInitialized() {
		return parser != null;
	}

	private JSGLRI getParser(Descriptor descriptor) {
		try {
			if (descriptor == null) return null;
			
			IParseController controller;
			controller = descriptor.createService(IParseController.class, null);
			if (controller instanceof DynamicParseController)
				controller = ((DynamicParseController) controller).getWrapped();
			if (controller instanceof SGLRParseController) {
				JSGLRI parser = ((SGLRParseController) controller).getParser(); 
				JSGLRI result = new JSGLRI(parser.getParseTable(), parser.getStartSymbol());
				result.setTimeout(FRAGMENT_PARSE_TIMEOUT);
				result.setUseRecovery(true);
				return result;
			}
		} catch (BadDescriptorException e) {
			Environment.logWarning("Could not load parser for testing language");
		} catch (RuntimeException e) {
			Environment.logWarning("Could not load parser for testing language");
		}
		return null;
	}

	public IStrategoTerm parseCached(ITokenizer oldTokenizer, IStrategoTerm fragment)
			throws TokenExpectedException, BadTokenException, SGLRException, IOException {
		String fragmentInput = createTestFragmentString(oldTokenizer, fragment);
		boolean successExpected = isSuccessExpected(fragment);
		Map<String, IStrategoTerm> cache = successExpected ? successParseCache : failParseCache;
		IStrategoTerm parsed = cache.get(fragmentInput);
		if (parsed == null) {
			parsed = parser.parse(fragmentInput, oldTokenizer.getFilename());
			cache.put(fragmentInput, parsed);
			if (!successExpected)
				clearTokenErrors(getTokenizer(parsed));
		}
		return parsed;
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
	
	private boolean isSuccessExpected(IStrategoTerm fragment) {
		IStrategoTerm test = getParent(getParent(fragment));
		IStrategoList expectations = termAt(test, test.getSubtermCount() - 1);
		for (IStrategoTerm expectation : StrategoListIterator.iterable(expectations)) {
			IStrategoConstructor cons = tryGetConstructor(expectation);
			if (cons == FAILS_0 || cons == FAILS_PARSING_0)
				return false;
		}
		return true;
	}
	
	private void clearTokenErrors(ITokenizer tokenizer) {
		for (IToken token : tokenizer) {
			((Token) token).setError(null);
		}
	}
}
