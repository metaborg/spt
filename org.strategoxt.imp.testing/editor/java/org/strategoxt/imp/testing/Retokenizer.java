package org.strategoxt.imp.testing;

import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getSort;
import static org.spoofax.jsglr.client.imploder.IToken.*;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.jsglr.client.imploder.Token;
import org.spoofax.jsglr.client.imploder.Tokenizer;

/** 
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class Retokenizer {
	
	private final Tokenizer oldTokenizer;
	
	private final Tokenizer newTokenizer;
	
	int oldTokenizerCopiedIndex;
	
	public Retokenizer(Tokenizer oldTokenizer) {
		this.oldTokenizer = oldTokenizer;
		newTokenizer = new Tokenizer(oldTokenizer.getInput(), oldTokenizer.getFilename(), null);
	}
	
	/*
	public void copyTokensUpToOffset(int endOffset) {
		int i = oldTokenizerCopiedIndex;
		for(;;) {
			Token token = oldTokenizer.getTokenAt(i);
			if (token.getEndOffset() > endOffset)
				break;
			if (token.getTokenizer() != newTokenizer)
				newTokenizer.reassignToken(token);
			i++;
		}
		oldTokenizerCopiedIndex = i;
	}
	*/
	
	public void copyTokensUpToIndex(int index) {
		copyTokenRange(oldTokenizer, oldTokenizerCopiedIndex, index);
		oldTokenizerCopiedIndex = index + 1;
	}
	
	public void skipTokensUpToIndex(int index) {
		oldTokenizerCopiedIndex = index + 1;
	}
	
	public void copyTokensAfterFragments() {
		copyTokensUpToIndex(oldTokenizer.getTokenCount() - 1);
	}
	
	public void copyTokensFromFragment(IStrategoTerm fragment, IStrategoTerm parsedFragment, int startOffset) {
		Tokenizer fragmentTokenizer = (Tokenizer) ImploderAttachment.getTokenizer(parsedFragment);
		IToken startToken = fragmentTokenizer.getTokenAtOffset(startOffset);
		int startIndex = startToken.getIndex();
		int endIndex = fragmentTokenizer.getTokenCount() - 1;
		Token endToken = fragmentTokenizer.getTokenAt(endIndex);
		
		// Reassign new starting token to parsed fragment (skipping whitespace)
		if (startToken.getKind() == TK_LAYOUT)
			startToken = newTokenizer.getTokenAt(startToken.getIndex() + 1);
		ImploderAttachment old = ImploderAttachment.get(parsedFragment);
		ImploderAttachment.putImploderAttachment(parsedFragment, parsedFragment.isList(), old.getSort(), startToken, old.getRightToken());
		copyTokenRange(fragmentTokenizer, startIndex, endIndex);
		
		// Reassign new tokens to unparsed fragment
		ImploderAttachment.putImploderAttachment(fragment, fragment.isList(), getSort(fragment), startToken, endToken);
	}
	
	private void copyTokenRange(Tokenizer fromTokenizer, int startIndex, int endIndex) {
		for (int i = startIndex; i <= endIndex; i++) {
			Token token = fromTokenizer.getTokenAt(i);
			/*Token newToken = newTokenizer.makeToken(token.getEndOffset(), token.getKind(), true);
			newToken.setAstNode(token.getAstNode());
			newToken.setError(token.getError());*/
			newTokenizer.reassignToken(token);
		}
	}

	public ITokenizer getTokenizer() {
		return newTokenizer;
	}
	
	@Override
	public String toString() {
		return newTokenizer.toString();
	}
}
