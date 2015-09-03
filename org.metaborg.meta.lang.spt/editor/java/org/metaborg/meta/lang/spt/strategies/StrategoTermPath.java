package org.metaborg.meta.lang.spt.strategies;

import static org.spoofax.jsglr.client.imploder.ImploderAttachment.hasImploderOrigin;
import static org.spoofax.terms.Term.isTermList;
import static org.spoofax.terms.attachments.OriginAttachment.tryGetOrigin;
import static org.spoofax.terms.attachments.ParentAttachment.getParent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.jsglr.client.imploder.TermTreeFactory;
import org.spoofax.terms.StrategoSubList;
import org.spoofax.terms.TermFactory;
import trans.position_of_term_1_0;
import trans.term_at_position_0_1;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class StrategoTermPath {
	private static int indexOfIdentical(IStrategoTerm parent, IStrategoTerm node) {
		int index = 0;
		for (int size = parent.getSubtermCount(); index < size; index++) {
			if (parent.getSubterm(index) == node)
				break;
		}
		return index;
	}

	public static IStrategoList createPath(ITermFactory factory, IStrategoTerm node) {
		List<Integer> pathInts = createPathList(node);
		return toStrategoPath(factory, pathInts);
	}

	public static IStrategoList toStrategoPath(ITermFactory factory, List<Integer> pathInts) {
		LinkedList<IStrategoTerm> results = new LinkedList<IStrategoTerm>();
		for (int i = 0; i < pathInts.size(); i++) {
			results.add(factory.makeInt(pathInts.get(i)));
		}
		return factory.makeList(results);
	}

	public static List<Integer> createPathList(IStrategoTerm node) {
		if (node instanceof StrategoSubList)
			node = ((StrategoSubList) node).getCompleteList();
		LinkedList<Integer> results = new LinkedList<Integer>();

		while (getParent(node) != null) {
			IStrategoTerm parent = getParent(node);
			int index = indexOfIdentical(parent, node);
			results.addFirst(Integer.valueOf(index));
			node = getParent(node);
		}
		return results;
	}

	public static IStrategoTerm getTermAtPath(Context context,
			IStrategoTerm term, IStrategoList path) {
		return term_at_position_0_1.instance.invoke(context, term, path);
	}

	/**
	 * Determine the path to a term in 'ast' with origin 'origin'.
	 */
	public static IStrategoList getTermPathWithOrigin(Context context,
			IStrategoTerm ast, IStrategoTerm origin) {
		if (ast == null)
			return null;
		if (isTermList(origin)) {
			// Lists have no origin information, try to find the node by its
			// first child.
			if (origin.getSubtermCount() > 0) {
				IStrategoList subtermPath = getTermPathWithOrigin(context, ast,
						origin.getSubterm(0));
				if (subtermPath != null) {
					// Arrays.copyOf is Java 1.6
					// IStrategoTerm[] originPath =
					// Arrays.copyOf(subtermPath.getAllSubterms(),
					// subtermPath.getSubtermCount()-1);
					IStrategoTerm[] allSubterms = subtermPath.getAllSubterms();
					IStrategoTerm[] originPath = new IStrategoTerm[subtermPath
							.getSubtermCount() - 1];
					System.arraycopy(allSubterms, 0, originPath, 0,
							originPath.length);
					TermFactory factory = new TermFactory();
					return factory.makeList(originPath);
				}
			}
			return null;
		}

		final IStrategoTerm originChild = origin.getSubtermCount() == 0 ? null
				: (IStrategoTerm) origin.getSubterm(0);

		class TestOrigin extends Strategy {
			IStrategoTerm origin1;
			IStrategoTerm nextBest;

			@Override
			public IStrategoTerm invoke(Context context, IStrategoTerm current) {
				if (hasImploderOrigin(current)) {
					IStrategoTerm currentOrigin = tryGetOrigin(current);
					if (currentOrigin == origin1)
						return current;
					IStrategoTerm currentImploderOrigin = ImploderAttachment
							.getImploderOrigin(currentOrigin);
					IStrategoTerm imploderOrigin1 = ImploderAttachment
							.getImploderOrigin(origin1);
					if (currentImploderOrigin != null
							&& imploderOrigin1 != null
							&& ImploderAttachment.getLeftToken(
									currentImploderOrigin).getStartOffset() == ImploderAttachment
									.getLeftToken(imploderOrigin1)
									.getStartOffset()
							&& ImploderAttachment.getRightToken(
									currentImploderOrigin).getEndOffset() == ImploderAttachment
									.getRightToken(imploderOrigin1)
									.getEndOffset()) {
						if (currentOrigin.equals(origin1))
							return current;
						if (current.getTermType() == origin1.getTermType()) {
							if (current.getTermType() == IStrategoTerm.APPL) {
								IStrategoAppl currentAppl = (IStrategoAppl) current;
								IStrategoAppl origin1Appl = (IStrategoAppl) origin1;
								if (currentAppl.getName().equals(
										origin1Appl.getName())
										&& currentAppl.getSubtermCount() == origin1Appl
												.getSubtermCount())
									return current;
							}
							nextBest = current;
						}
					}
					// sets a term as 'nextBest' if one of the subterms of its
					// origin-term is the originChild
					if (nextBest == null && originChild != null) {
						for (int i = 0, max = currentOrigin.getSubtermCount(); i < max; i++)
							if (currentOrigin.getSubterm(i) == originChild)
								nextBest = currentOrigin;
					}
				} else { // sets a term as 'nextBest' in case no origin term
							// exists, but one of its subterms is origin-related
							// to the originChild
					if (current == origin1)
						return current;
					if (nextBest == null && originChild != null) {
						for (int i = 0, max = current.getSubtermCount(); i < max; i++)
							if (tryGetOrigin(current.getSubterm(i)) == originChild)
								nextBest = current;
					}
				}
				return null;
			}
		}
		TestOrigin testOrigin = new TestOrigin();
		testOrigin.origin1 = origin;

		IStrategoTerm perfectMatch = position_of_term_1_0.instance.invoke(
				context, ast, testOrigin);

		if (perfectMatch != null) {
			return (IStrategoList) perfectMatch;
		} else if (testOrigin.nextBest != null) {
			testOrigin.origin1 = testOrigin.nextBest;
			return (IStrategoList) position_of_term_1_0.instance.invoke(
					context, ast, testOrigin);
		} else {
			return null;
		}
	}

	public static IStrategoTerm findCommonAncestor(IStrategoTerm node1,
			IStrategoTerm node2) {
		if (node1 == null)
			return node2;
		if (node2 == null)
			return node1;

		List<IStrategoTerm> node1Ancestors = new ArrayList<IStrategoTerm>();
		for (IStrategoTerm n = node1; n != null; n = getParent(n))
			node1Ancestors.add(n);

		for (IStrategoTerm n = node2, n2Child = node2; n != null; n2Child = n, n = getParent(n)) {
			int node1Index = node1Ancestors.indexOf(n);
			if (node1Index != -1 && node1Ancestors.get(node1Index) == n)
				return tryCreateListCommonAncestor(n, node1Ancestors, n2Child);
		}

		assert false : "Could not find common ancestor for nodes: " + node1
				+ "," + node2;
		return getRoot(node1);
	}

	private static IStrategoTerm tryCreateListCommonAncestor(
			IStrategoTerm commonAncestor, List<IStrategoTerm> ancestors1List,
			IStrategoTerm child2) {
		if (commonAncestor != child2 && commonAncestor.isList()) {
			int i = ancestors1List.indexOf(commonAncestor);
			if (i == 0)
				return commonAncestor;
			IStrategoTerm child1 = ancestors1List.get(i - 1);
			return new TermTreeFactory().createSublist(
					(IStrategoList) commonAncestor, child1, child2);
		} else {
			return commonAncestor;
		}
	}

	private static IStrategoTerm getRoot(IStrategoTerm selection) {
		IStrategoTerm result = selection;
		while (getParent(result) != null)
			result = getParent(result);
		return result;
	}
}
