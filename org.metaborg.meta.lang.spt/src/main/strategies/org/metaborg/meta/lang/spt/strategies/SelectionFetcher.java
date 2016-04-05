package org.metaborg.meta.lang.spt.strategies;

import java.util.ArrayList;
import java.util.List;

import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.source.SourceRegion;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.terms.Term;
import org.spoofax.terms.TermVisitor;

import com.google.inject.Injector;

/**
 * Fetches Selections (i.e. the marked terms) from an SPT test fragment.
 */
public class SelectionFetcher {

	private static final ILogger logger = LoggerUtils.logger(SelectionFetcher.class);
	private final ITermFactory termFactory;
	private final IStrategoConstructor MARKED;
	
	public SelectionFetcher(Injector injector) {
		termFactory = injector.getInstance(ITermFactoryService.class).getGeneric();
		MARKED = termFactory.makeConstructor("Marked", 3);
	}
	
	/**
	 * Return all Marked terms from the SPT AST.
	 * @return the Marked terms. See {@link SelectionFetcher#MARKED}.
	 */
	public List<IStrategoTerm> fetchMarked(IStrategoTerm sptFragment) {
		// the Marked terms
		final List<IStrategoTerm> marked = new ArrayList<IStrategoTerm>();

		// collect the terms from the given AST
		new TermVisitor() {
			@Override
			public void preVisit(IStrategoTerm term) {
				// Look for Marked("[[", QuotePart("selection"), "]]")
				if (MARKED == Term.tryGetConstructor(term)) {
					// get the string from the QuotePart
					marked.add(term);
				}
			}
		}.visit(sptFragment);
		
		return marked;
	}
	/**
	 * Get the start and end offset of the given term.
	 * @param marked the term of which you want the offsets.
	 * @return the start and end offset of the term.
	 */
	public static ISourceRegion getOffsets(IStrategoTerm marked) {
		IStrategoTerm sptSelection = Term.termAt(Term.termAt(marked, 1), 0);
		IToken leftToken = ImploderAttachment.getLeftToken(sptSelection);
		IToken rightToken = ImploderAttachment.getRightToken(sptSelection);
		return new SourceRegion(
				leftToken.getStartOffset(),
				leftToken.getLine(),
				leftToken.getColumn(),
				rightToken.getEndOffset(),
				rightToken.getEndLine(),
				rightToken.getEndColumn());
	}
	
	/**
	 * Get the first term (order determined by how a TermVisitor visits children)
	 * that has the exact same start and end offsets as the given region.
	 * @param region the region with the start and end offset you are looking for.
	 * NOTE: the row and col can be bogus values, we don't look at them.
	 * @param parsedFragment the AST from which to select the term.
	 * @return the term with the same offsets as the region.
	 * Or null if no term had the same offsets.
	 */
	public static IStrategoTerm fetchOne(final ISourceRegion region, IStrategoTerm parsedFragment) {
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
				if (start != region.startOffset()) {
					return;
				}
				// start offset matches, now compare end offset
				int end = ImploderAttachment.getRightToken(term).getEndOffset();
				if (end != region.endOffset()) {
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
		final List<ISourceRegion> selectionRegions = new ArrayList<ISourceRegion>(sptSelections.size());
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
					ISourceRegion current = selectionRegions.get(i);
					// compare start offset
					int start = ImploderAttachment.getLeftToken(term).getStartOffset();
					// NOTE: this assumes the regions are ordered by ascending start offset!
					if (start < current.startOffset()) {
						// this term is before the ones we're looking for. Off to the next term.
						return;
					} else if (start > current.startOffset()) {
						// this term comes after this section. Off to the next section.
						continue;
					}
					// start offset matches, now compare end offset
					// NOTE: this assumes that ties in the start offset ordering are broken by smallest end offset first!
					int end = ImploderAttachment.getRightToken(term).getEndOffset();
					if (end < current.endOffset()) {
						// this term is before the ones we are looking for. Off to the next term.
						return;
					} else if (end > current.endOffset()) {
						// this term comes after this section. Off to the next section.
						continue;
					}
					// found a match
					results[i] = term;
				}
			}
		}.visit(parsedFragment);

		// if a selection couldn't be found, we don't return an empty list
		for (int i = 0; i < results.length; i++) {
			if (results[i] == null) {
				return termFactory.makeList();
			}
		}
		return termFactory.makeList(results);
	}
}
