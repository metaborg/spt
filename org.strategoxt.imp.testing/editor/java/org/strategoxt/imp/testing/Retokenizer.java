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
		reassignTokenRange(oldTokenizer, oldTokenizerCopiedIndex, index);
		oldTokenizerCopiedIndex = index + 1;
	}
	
	public void skipTokensUpToIndex(int index) {
		oldTokenizerCopiedIndex = index + 1;
	}
	
	public void copyTokensAfterFragments() {
		copyTokensUpToIndex(oldTokenizer.getTokenCount() - 1);
	}
	
	public void copyTokensFromFragment(IStrategoTerm fragment, IStrategoTerm parsedFragment, int startOffset, int endOffset) {
		Tokenizer fragmentTokenizer = (Tokenizer) ImploderAttachment.getTokenizer(parsedFragment);
		IToken startToken = fragmentTokenizer.getTokenAtOffset(startOffset);
		IToken endToken = fragmentTokenizer.getTokenAtOffset(endOffset);
		int startIndex = startToken.getIndex();
		int endIndex = endToken.getIndex();
		
		// Reassign new starting token to parsed fragment (skipping whitespace)
		if (startToken.getKind() == TK_LAYOUT && startIndex + 1 < newTokenizer.getTokenCount())
			startToken = newTokenizer.getTokenAt(++startIndex);
		reassignTokenRange(fragmentTokenizer, startIndex, endIndex);
		ImploderAttachment old = ImploderAttachment.get(parsedFragment);
		ImploderAttachment.putImploderAttachment(parsedFragment, parsedFragment.isList(), old.getSort(), startToken, endToken);
		
		// Reassign new tokens to unparsed fragment
		ImploderAttachment.putImploderAttachment(fragment, fragment.isList(), getSort(fragment), startToken, endToken);
	}
	
	private void reassignTokenRange(Tokenizer fromTokenizer, int startIndex, int endIndex) {
		for (int i = startIndex; i <= endIndex; i++) {
			Token token = fromTokenizer.getTokenAt(i);
			/*Token newToken = newTokenizer.makeToken(token.getEndOffset(), token.getKind(), true);
			newToken.setAstNode(token.getAstNode());
			newToken.setError(token.getError());*/
			// Since we case, we first clone before changing the token
			if (token.getTokenizer() != newTokenizer) // can happen w/ ambs
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
