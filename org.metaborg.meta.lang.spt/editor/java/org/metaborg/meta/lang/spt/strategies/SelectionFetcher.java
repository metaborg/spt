package org.metaborg.meta.lang.spt.strategies;

import java.util.ArrayList;
import java.util.List;

import org.metaborg.meta.lang.spt.strategies.FragmentParser.OffsetRegion;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.terms.Term;
import org.spoofax.terms.TermVisitor;

import com.google.inject.Injector;

/**
 * Fetches Selections (i.e. the marked terms) from an SPT test fragment.
 */
public class SelectionFetcher {

	private static final Logger logger = LoggerFactory.getLogger(SelectionFetcher.class);
	private final ITermFactory termFactory;
	private final IStrategoConstructor MARKED;
	
	public SelectionFetcher(Injector injector) {
		termFactory = injector.getInstance(ITermFactoryService.class).getGeneric();
		MARKED = termFactory.makeConstructor("Marked", 3);
	}
	
	public List<IStrategoTerm> fetchMarked(IStrategoTerm sptFragment) {
		// the regions of the marked strings in the SPT fragment
		final List<IStrategoTerm> selectionRegions = new ArrayList<IStrategoTerm>();

		// collect the offsets of SPT selections (i.e. the strings in marked quoteparts)
		new TermVisitor() {
			@Override
			public void preVisit(IStrategoTerm term) {
				// Look for Marked("[[", QuotePart("selection"), "]]")
				if (MARKED == Term.tryGetConstructor(term)) {
					// get the string from the QuotePart
					selectionRegions.add(term);
				}
			}
		}.visit(sptFragment);
		
		return selectionRegions;
	}
	
	public OffsetRegion getOffsets(IStrategoTerm marked) {
		assert MARKED == Term.tryGetConstructor(marked);
		IStrategoTerm sptSelection = Term.termAt(Term.termAt(marked, 1), 0);
		return new OffsetRegion(
				ImploderAttachment.getLeftToken(sptSelection).getStartOffset(),
				ImploderAttachment.getRightToken(sptSelection).getEndOffset());
	}
	
	public IStrategoTerm fetchOne(IStrategoTerm marked, IStrategoTerm parsedFragment) {
		final OffsetRegion region = getOffsets(marked);
		final List<IStrategoTerm> resultContainer = new ArrayList<IStrategoTerm>();
		new TermVisitor() {
			protected IStrategoTerm result = null;
			@Override
			public boolean isDone(IStrategoTerm term) {
				return result != null;
			};
			@Override
			public void preVisit(IStrategoTerm term) {
				// We can't check the offsets of this term without an imploder!
				if (ImploderAttachment.get(term) == null || isDone(null)) {
					return;
				}
				// compare start offset
				int start = ImploderAttachment.getLeftToken(term).getStartOffset();
				if (start != region.startOffset) {
					return;
				}
				// start offset matches, now compare end offset
				int end = ImploderAttachment.getRightToken(term).getEndOffset();
				if (end != region.endOffset) {
					return;
				}
				// found a match
				result = term;
				resultContainer.add(result);
			}
		}.visit(parsedFragment);
		return resultContainer.isEmpty() ? null : resultContainer.get(0);
	}
	
	/**
	 * Fetch the innermost nodes in the parsedFragment that correspond to the marked nodes of the sptFragment.
	 *
	 * We use the character start and end offsets of the tokens associated to both ASTs
	 * using the ImploderAttachment.
	 * @param sptFragment the Input/Output fragment term, containing the Marked nodes that were selected.
	 * @param parsedFragment the parsed fragment (see {@link FragmentParser}).
	 * @return a stratego list with the selected nodes from the parsedFragment.
	 */
	public IStrategoList fetch(IStrategoTerm sptFragment, IStrategoTerm parsedFragment) {
		// get the offset regions
		final List<IStrategoTerm> sptSelections = fetchMarked(sptFragment);
		final List<OffsetRegion> selectionRegions = new ArrayList<OffsetRegion>(sptSelections.size());
		for (IStrategoTerm marked : sptSelections) {
			selectionRegions.add(getOffsets(marked));
		}

		if (selectionRegions.isEmpty()) {
			return termFactory.makeList();
		}

		final IStrategoTerm[] results = new IStrategoTerm[selectionRegions.size()];

		/* Get the innermost terms with the same offset as the regions selected above.
		 * To get the innermost term, 
		 * we simply keep overriding the result for each child that has the correct offset.
		 */
		new TermVisitor() {
			@Override
			public void preVisit(IStrategoTerm term) {
				// We can't check the offsets of this term without an imploder!
				if (ImploderAttachment.get(term) == null || isDone(null)) {
					return;
				}
				for (int i = 0; i < selectionRegions.size(); i++) {
					OffsetRegion current = selectionRegions.get(i);
					// compare start offset
					int start = ImploderAttachment.getLeftToken(term).getStartOffset();
					if (start < current.startOffset) {
						// this term is before the ones we're looking for. Off to the next term.
						return;
					} else if (start > current.startOffset) {
						// this term comes after this section. Off to the next section.
						continue;
					}
					// start offset matches, now compare end offset
					int end = ImploderAttachment.getRightToken(term).getEndOffset();
					if (end < current.endOffset) {
						// this term is before the ones we are looking for. Off to the next term.
						return;
					} else if (end > current.endOffset) {
						// this term comes after this section. Off to the next section.
						continue;
					}
					// found a match
					results[i] = term;
				}
			}
		}.visit(parsedFragment);

		return termFactory.makeList(results);

//		new TermVisitor() {
//			IStrategoTerm unclosedChild;
//			IToken unclosedLeft;
//			IToken lastCloseQuote;
//
//			public void preVisit(IStrategoTerm term) {
//				IToken left = getTokenBefore(getLeftToken(term));
//				IToken right = getTokenAfter(getRightToken(term));
//				if (isOpenQuote(left) && isNoQuoteBetween(left, right)) {
//					if (isCloseQuote(right) && isNoQuoteBetween(left, right)) {
//						if (right != lastCloseQuote) {
//							lastCloseQuote = right;
//							results.add(getMatchingDescendant(term));
//						}
//					} else if (unclosedChild == null) {
//						unclosedChild = term;
//						unclosedLeft = left;
//					}
//				}
//			}
//
//			@Override
//			public void postVisit(IStrategoTerm term) {
//				IToken right = getTokenAfter(getRightToken(term));
//				if (unclosedChild != null && isCloseQuote(right)
//						&& isNoQuoteBetween(unclosedLeft, right)) {
//					results.add(StrategoTermPath.findCommonAncestor(
//							unclosedChild, term));
//					unclosedChild = null;
//				}
//			}
//		}.visit(parsedFragment);
//		return termFactory.makeList(results);
	}


//	private static IStrategoTerm getMatchingDescendant(IStrategoTerm term) {
//		IToken left = getLeftToken(term);
//		IToken right = getRightToken(term);
//		for (int i = 0; i < term.getSubtermCount(); i++) {
//			IStrategoTerm child = termAt(term, i);
//			if (getLeftToken(child) == left && getRightToken(child) == right)
//				return getMatchingDescendant(child);
//		}
//		return term;
//	}
//
//	private boolean isOpenQuote(IToken left) {
//		return left != null && left.getKind() == TK_ESCAPE_OPERATOR
//				&& isQuoteOpenText(left.toString());
//	}
//
//	private boolean isCloseQuote(IToken right) {
//		return right != null && right.getKind() == TK_ESCAPE_OPERATOR
//				&& !isQuoteOpenText(right.toString());
//	}
//
//	private boolean isNoQuoteBetween(IToken left, IToken right) {
//		ITokenizer tokenizer = left.getTokenizer();
//		for (int i = left.getIndex() + 1, end = right.getIndex(); i < end; i++) {
//			IToken token = tokenizer.getTokenAt(i);
//			if (token.getKind() == TK_ESCAPE_OPERATOR) {
//				return false;
//			}
//		}
//		return true;
//	}
//
//	private boolean isQuoteOpenText(String contents) {
//		// HACK: inspect string contents to find out if it's an open or close
//		// quote
//		if (contents.contains("[")) {
//			return true;
//		} else {
//			assert contents.contains("]");
//			return false;
//		}
//	}

}
