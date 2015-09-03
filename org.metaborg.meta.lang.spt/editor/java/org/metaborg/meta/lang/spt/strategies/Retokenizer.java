package org.metaborg.meta.lang.spt.strategies;

import static org.spoofax.jsglr.client.imploder.AbstractTokenizer.findRightMostLayoutToken;
import static org.spoofax.jsglr.client.imploder.AbstractTokenizer.getTokenAfter;
import static org.spoofax.jsglr.client.imploder.IToken.TK_ESCAPE_OPERATOR;
import static org.spoofax.jsglr.client.imploder.IToken.TK_LAYOUT;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getLeftToken;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.getSort;
import static org.spoofax.terms.Term.isTermString;
import static org.spoofax.terms.Term.tryGetConstructor;
import static org.spoofax.terms.attachments.ParentAttachment.getParent;

import java.util.ArrayList;
import java.util.List;

import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.jsglr.client.imploder.Token;
import org.spoofax.jsglr.client.imploder.Tokenizer;
import org.spoofax.terms.TermVisitor;

import com.google.inject.Injector;

/**
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class Retokenizer {
	
	private static final Logger logger = LoggerFactory.getLogger(Retokenizer.class);
	
	private static IStrategoConstructor QUOTEPART_1;
	
	private final Tokenizer oldTokenizer;
	
	private final Tokenizer newTokenizer;
	
	int oldTokenizerCopiedIndex;
	
	public Retokenizer(Injector injector, Tokenizer oldTokenizer) {
	    if (QUOTEPART_1 == null)
	        QUOTEPART_1 = injector.getInstance(ITermFactoryService.class).getGeneric().makeConstructor("QuotePart", 1);
		this.oldTokenizer = oldTokenizer;
		newTokenizer = new Tokenizer(oldTokenizer.getInput(), oldTokenizer.getFilename(), null);
		newTokenizer.setSyntaxCorrect(oldTokenizer.isSyntaxCorrect());
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
	
	/**
	 * Skip tokens up to and including the given index.
	 */
	public void skipTokensUpToIndex(int index) {
		oldTokenizerCopiedIndex = index + 1;
	}
	
	public void copyTokensAfterFragments() {
		copyTokensUpToIndex(oldTokenizer.getTokenCount() - 1);
	}
	
	/**
	 * Copy tokens from the tokenizer of the parsedFragment.
	 *
	 * TODO: properly describe what it does. It's kind of complicated.
	 *
	 * @param fragmentHead
	 * @param fragmentTail
	 * @param parsedFragment
	 * @param startOffset
	 * @param endOffset
	 */
	public void copyTokensFromFragment(IStrategoTerm fragmentHead, IStrategoTerm fragmentTail, IStrategoTerm parsedFragment, int startOffset, int endOffset) {
		Tokenizer fragmentTokenizer = (Tokenizer) ImploderAttachment.getTokenizer(parsedFragment);
		IToken startToken, endToken;
		if (fragmentTokenizer.getStartOffset() < startOffset)
			logger.warn("The parsed fragment ends before the end offset of the textual SPT fragment. That's super weird!!!");
		if (fragmentTokenizer.getStartOffset() <= startOffset) {
			/* if the fragment's tokens don't extend beyond the given startOffset,
			 * we copy all tokens from the fragment that have the same offset
			 * as the last token of the fragment.
			 * TODO: doesn't that mean that if the fragment is strictly
			 * before the given startOffset we copy anyway?
			 */
			endToken = fragmentTokenizer.currentToken();
			startToken = Tokenizer.getFirstTokenWithSameOffset(endToken);
		} else {
			// otherwise we copy every token from the start until the end offset
			startToken = Tokenizer.getFirstTokenWithSameOffset(
					fragmentTokenizer.getTokenAtOffset(startOffset));
			endToken = Tokenizer.getLastTokenWithSameEndOffset(
					fragmentTokenizer.getTokenAtOffset(endOffset));
		}
		((Token) endToken).setEndOffset(endOffset); // cut off if too long
		int startIndex = startToken.getIndex();
		int endIndex = endToken.getIndex();
		
		// Reassign new starting token to parsed fragment (skipping first leading LAYOUT)
		if (startToken.getKind() == TK_LAYOUT && startIndex + 1 < fragmentTokenizer.getTokenCount()
				&& startIndex < endIndex)
			startToken = fragmentTokenizer.getTokenAt(++startIndex);
		// set errors on previous setup blocks on the first and last token of the fragment
		moveTokenErrorsToRange(fragmentTokenizer, startIndex, endIndex);
		// assign the tokens of the fragment to the new tokenizer
		reassignTokenRange(fragmentTokenizer, startIndex, endIndex);
		ImploderAttachment old = ImploderAttachment.get(parsedFragment);
		ImploderAttachment.putImploderAttachment(parsedFragment, parsedFragment.isList(), old.getSort(), startToken, endToken);
		
		// mark the token of the parsed fragment at the offset of the opening bracket of a selection.
		// this token is a layout token (a space), because of the way we whiteout fragments in FragmentParser
		recolorMarkingBrackets(fragmentTail, fragmentTokenizer);

		/* FIXME:
		 * I don't see why would want to lose the position information of these elements in the AST.
		 * It's true that the AST will be associated with a tokenizer with different tokens,
		 * but I don't see how that will be a problem.
		 * The token offsets are still valuable, so for now I'm disabling this stuff.
		 */
		// all nodes in the original fragment (QuotePart etc.) will get the same start and end token
//		assignTokens(fragmentHead, startToken, endToken);
//		assignTokens(fragmentTail, startToken, endToken);
	}

	/**
	 * Assign all nodes in the given tree to the given token range.
	 * This means all nodes have the same tokens assigned to them.
	 *
	 * @param tree the AST whose nodes will be reassigned to the token range.
	 * @param startToken the new start token for all nodes in the tree.
	 * @param endToken the new end token for all nodes in the tree.
	 */
	private void assignTokens(IStrategoTerm tree, final IToken startToken, final IToken endToken) {
		// HACK: asssign the same tokens to all tree nodes in fragments
		//       (breaks some editor services)
		new TermVisitor() {
			public void preVisit(IStrategoTerm term) {
				ImploderAttachment.putImploderAttachment(term, false, getSort(term), startToken, endToken);
			}
		}.visit(tree);
	}

	/**
	 * Set the kind of the left token of any string in the given term that is not in a QuotePart 
	 * (i.e. the left bracket of Marked and MarkedPlaceholder) to {@link IToken#TK_ESCAPE_OPERATOR}.
	 * If the given tokenizer was not used to create the given term,
	 * the kind of the corresponding token (same index as the bracket token) of the given tokenizer
	 * will be changed instead.
	 * @param term the term in which to look for the left brackets.
	 * @param tokenizer the tokenizer in which to set the token kind.
	 */
	private void recolorMarkingBrackets(IStrategoTerm term, final ITokenizer tokenizer) {
		new TermVisitor() {
			public void preVisit(IStrategoTerm term) {
			    /* TODO: why use != QuotePart instead of == Marked?
			     * Maybe because we used to support [...] MarkedPlaceholder,
			     * but that doesn't work nowadays (right?).
			     */
				if (isTermString(term) && tryGetConstructor(getParent(term)) != QUOTEPART_1) {
					IToken token1 = getLeftToken(term);
					IToken token2 = tokenizer.getTokenAtOffset(token1.getStartOffset());
					token2.setKind(TK_ESCAPE_OPERATOR);
				}
			}
		}.visit(term);
	}
	
	/**
	 * Combine all errors of the tokens before startIndex
	 * and COPY (not move) this new error to the tokens at startIndex and endIndex.
	 * Thereby OVERRRIDING any current errors on these two tokens.
	 * @param tokenizer the tokenizer whose tokens to use.
	 * @param startIndex the index of a token on which we set the error.
	 * The error is built by combining all errors of all tokens before this index.
	 * @param endIndex the index of a token on which we set the error.
	 */
	private void moveTokenErrorsToRange(Tokenizer tokenizer, int startIndex, int endIndex) {
	    // copy all errors before startIndex to the token at startIndex
		List<IToken> prefixErrors = collectErrorTokens(tokenizer, 0, startIndex);
		Token startToken = tokenizer.getTokenAt(startIndex);
		// FIXME: as collectErrorTokens is NOT inclusive on endIndex, this check should trivially be true!
		if (prefixErrors.size() != 1 || prefixErrors.get(0) != startToken)
			startToken.setError(getErrorsAsString(prefixErrors));

		/* FIXME: what is the point of this?
		 * collectErrorTokens and getErrorsAsString don't influence the tokens,
		 * so this should always be the same as prefixErrors
		 */
		List<IToken> postfixErrors = collectErrorTokens(tokenizer, 0, startIndex);
		Token endToken = tokenizer.getTokenAt(endIndex);
		if (postfixErrors.size() != 1 || postfixErrors.get(0) != endToken)
			endToken.setError(getErrorsAsString(postfixErrors));
	}
	
	/**
	 * Get a list of all tokens from the given tokenizer,
	 * starting at the token at startIndex, 
	 * until, not including, the token at endIndex.
	 * @param tokenizer the tokenizer whose tokens you want.
	 * @param startIndex the index of the first token you want if it has an error.
	 * @param endIndex the index of the last token you want if it has an error.
	 * @return a list of tokens with getError != null.
	 */
	private List<IToken> collectErrorTokens(Tokenizer tokenizer, int startIndex, int endIndex) {
		List<IToken> results = new ArrayList<IToken>();
		for (int i = 0; i < endIndex; i++) {
			Token token = tokenizer.internalGetTokenAt(i);
			if (token.getError() != null)
				results.add(token);
		}
		return results;
	}
	
	/**
	 * Get all distinct errors on the tokens and combine them into a String.
	 *
	 * @param tokens the tokens from which you want the errors.
	 * @return null if there were no errors.
	 * Otherwise a String with each distinct error on a new line.
	 */
	private String getErrorsAsString(List<IToken> tokens) {
		String lastError = null;
		StringBuilder result = new StringBuilder();
		for (IToken token : tokens) {
			String error = token.getError();
			if (error != lastError) {
				if (error.startsWith(ITokenizer.ERROR_SKIPPED_REGION)) {
					token = getTokenAfter(findRightMostLayoutToken(token));
					error = "unexpected construct(s)";
				}
				result.append("line " + token.getLine() + ": " + error + ", \n");
				lastError = error;
			}
		}
		if (result.length() == 0)
			return null;
		result.delete(result.length() - 3, result.length());
		return result.toString();
	}

	/**
	 * Reassign all tokens between, and including, the startIndex and endIndex to the new tokenizer.
	 * @param fromTokenizer the tokenizer from which we reassign the tokens.
	 * @param startIndex the index of the first token to reassign.
	 * @param endIndex the index of the last token to reassign.
	 */
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
