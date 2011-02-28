package org.strategoxt.imp.testing;

import static org.spoofax.interpreter.core.Tools.listAt;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getRightToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getTokenizer;
import static org.spoofax.terms.Term.tryGetConstructor;
import static org.spoofax.terms.attachments.ParentAttachment.getParent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.imp.model.ISourceProject;
import org.eclipse.imp.parser.IParseController;
import org.spoofax.interpreter.terms.IStrategoAppl;
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
import org.spoofax.terms.TermVisitor;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
import org.strategoxt.imp.runtime.dynamicloading.DynamicParseController;
import org.strategoxt.imp.runtime.parser.JSGLRI;
import org.strategoxt.imp.runtime.parser.SGLRParseController;
import org.strategoxt.imp.runtime.stratego.SourceAttachment;
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
	
	private static final IStrategoConstructor SETUP_3 =
		Environment.getTermFactory().makeConstructor("Setup", 3);
	
	private static final int EXCLUSIVE = 1;
	
	private final WeakValueHashMap<String, IStrategoTerm> failParseCache =
		new WeakValueHashMap<String, IStrategoTerm>();
	
	private final WeakValueHashMap<String, IStrategoTerm> successParseCache =
		new WeakValueHashMap<String, IStrategoTerm>();
	
	private Descriptor parseCacheDescriptor;
	
	private JSGLRI parser;

	private List<int[]> setupRegions;
	
	private boolean isLastSyntaxCorrect;

	public void configure(Descriptor descriptor, IPath path, ISourceProject project, IStrategoTerm ast) {
		if (parseCacheDescriptor != descriptor) {
			parseCacheDescriptor = descriptor;
			parser = getParser(descriptor, path, project);
			failParseCache.clear();
			successParseCache.clear();
		}
		setupRegions = getSetupRegions(ast);
	}
	
	public boolean isInitialized() {
		return parser != null;
	}

	private JSGLRI getParser(Descriptor descriptor, IPath path, ISourceProject project) {
		try {
			if (descriptor == null) return null;
			
			IParseController controller;
			controller = descriptor.createService(IParseController.class, null);
			if (controller instanceof DynamicParseController)
				controller = ((DynamicParseController) controller).getWrapped();
			if (controller instanceof SGLRParseController) {
				SGLRParseController sglrController = (SGLRParseController) controller;
				controller.initialize(path, project, null);
				JSGLRI parser = sglrController.getParser(); 
				JSGLRI result = new JSGLRI(parser.getParseTable(), parser.getStartSymbol(), (SGLRParseController) controller);
				result.setTimeout(FRAGMENT_PARSE_TIMEOUT);
				result.setUseRecovery(true);
				return result;
			} else {
				throw new IllegalStateException("SGLRParseController expected");
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
		
		// TODO: use context-independent caching key
		//       (requires offset adjustments for reuse...)
		// String fragmentInputCompact = createTestFragmentString(oldTokenizer, fragment, true);
		String fragmentInput = createTestFragmentString(oldTokenizer, fragment, false);
		boolean successExpected = isSuccessExpected(fragment);
		IStrategoTerm parsed = getCache(successExpected).get(fragmentInput/*Compact*/);
		if (parsed != null) {
			isLastSyntaxCorrect = successExpected;
		} else {
			//String fragmentInput = createTestFragmentString(oldTokenizer, fragment, false);
			SGLRParseController controller = parser.getController();
			controller.getParseLock().lock();
			try {
				parsed = parser.parse(fragmentInput, oldTokenizer.getFilename());
			} finally {
				controller.getParseLock().unlock();
			}
			isLastSyntaxCorrect = getTokenizer(parsed).isSyntaxCorrect();
			IResource resource = controller.getResource();
			SourceAttachment.putSource(parsed, resource, controller);
			if (!successExpected)
				clearTokenErrors(getTokenizer(parsed));
			if (isLastSyntaxCorrect == successExpected)
				getCache(isLastSyntaxCorrect).put(fragmentInput/*Compact*/, parsed);
		}
		return parsed;
	}

	private WeakValueHashMap<String, IStrategoTerm> getCache(
			boolean parseSuccess) {
		return parseSuccess ? successParseCache : failParseCache;
	}

	private String createTestFragmentString(ITokenizer tokenizer, IStrategoTerm term,
			boolean compactWhitespace) {
		
		int fragmentStart = getLeftToken(term).getStartOffset();
		int fragmentEnd = getRightToken(term).getEndOffset();
		/*
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
		*/
		String input = tokenizer.getInput();
		StringBuilder result = new StringBuilder(
			compactWhitespace ? input.length() + 16
			                  : input.length());
		
		boolean addedFragment = false;
		int index = 0;
		
		for (int[] setupRegion : setupRegions) {
			int setupStart = setupRegion[0];
			int setupEnd = setupRegion[1];
			if (!addedFragment && setupStart >= fragmentStart) {
				addWhitespace(input, index, fragmentStart - 1, result);
				result.append(input, fragmentStart, fragmentEnd + EXCLUSIVE);
				index = fragmentEnd + 1;
				addedFragment = true;
			}
			if (setupStart != fragmentStart) { // fragment is setup region
				addWhitespace(input, index, setupStart - 1, result);
				result.append(input, setupStart, setupEnd + EXCLUSIVE);
				index = setupEnd + 1;
			}
		}
		
		if (!addedFragment) {
			addWhitespace(input, index, fragmentStart - 1, result);
			result.append(input, fragmentStart, fragmentEnd + EXCLUSIVE);
			index = fragmentEnd + 1;
		}
		
		addWhitespace(input, index, input.length() - 1, result);
		
		/*
		for (IToken token : tokenizer) {
			int length = token.getLength();
			if (length == 0) {
				// skip
			} else if (token.getKind() == IToken.TK_STRING
				&& ((token.getStartOffset() >= fragmentStart && token.getEndOffset() <= fragmentEnd)
					|| isSetupToken(token))) {
				result.append(token);
			} else if (!compactWhitespace) {
				for (int c = 0; c < length; c++)
					result.append(token.charAt(c) == '\n' ? '\n' : ' ');
			}
		}
		*/
		assert result.length() == input.length();
		return result.toString(); 
	}

	private static void addWhitespace(String input, int startOffset, int endOffset, StringBuilder output) {
		for (int i = startOffset; i <= endOffset; i++)
			output.append(input.charAt(i) == '\n' ? '\n' : ' ');
	}
	
	private List<int[]> getSetupRegions(IStrategoTerm ast) {
		final List<int[]> results = new ArrayList<int[]>();
		new TermVisitor() {
			public void preVisit(IStrategoTerm term) {
				if (tryGetConstructor(term) == SETUP_3) {
					IStrategoTerm input = term.getSubterm(2).getSubterm(1);
					int[] region = { getLeftToken(input).getStartOffset(), getRightToken(input).getEndOffset() };
					results.add(region);
				}
			}
		}.visit(ast);
		return results;
	}
	
	/*
	private boolean isSetupToken(IToken token) {
		// if (token.getKind() != IToken.TK_STRING) return false;
		assert token.getKind() == IToken.TK_STRING;
		IStrategoTerm node = (IStrategoTerm) token.getAstNode();
		if (node != null && "Input".equals(getSort(node))) {
			IStrategoTerm parent = getParent(node);
			if (parent != null && isTermAppl(parent) && "Setup".equals(((IStrategoAppl) parent).getName()))
				return true;
		}
		return false;
	}
	*/
	
	private boolean isSuccessExpected(IStrategoTerm fragment) {
		IStrategoAppl test = (IStrategoAppl) getParent(getParent(fragment));
		if (test.getConstructor() == SETUP_3) return true;
		IStrategoList expectations = listAt(test, test.getSubtermCount() - 1);
		for (IStrategoTerm expectation : StrategoListIterator.iterable(expectations)) {
			IStrategoConstructor cons = tryGetConstructor(expectation);
			if (cons == FAILS_0 || cons == FAILS_PARSING_0)
				return false;
		}
		return true;
	}
	
	public boolean isLastSyntaxCorrect() {
		return isLastSyntaxCorrect;
	}
	
	private void clearTokenErrors(ITokenizer tokenizer) {
		for (IToken token : tokenizer) {
			((Token) token).setError(null);
		}
	}
}
