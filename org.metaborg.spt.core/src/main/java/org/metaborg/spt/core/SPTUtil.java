package org.metaborg.spt.core;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.StrategoAnnotation;
import org.spoofax.terms.Term;

public class SPTUtil {

    private static final ILogger logger = LoggerUtils.logger(SPTUtil.class);

    public static final String START_SYMBOL_CONS = "StartSymbol";
    public static final String TEST_CONS = "Test";
    public static final String SELECTION_CONS = "Selection";
    public static final String FRAGMENT_CONS = "Fragment";
    public static final String TAILPART_DONE_CONS = "Done";
    public static final String TAILPART_MORE_CONS = "More";

    /**
     * Get the name of the constructor of this term if it has one.
     * 
     * @return the constructor name, or null if this term has no constructor.
     */
    public static String consName(IStrategoTerm term) {
        if(Term.isTermAppl(term)) {
            return ((IStrategoAppl) term).getConstructor().getName();
        } else {
            return null;
        }
    }

    public static String noAnnosString(IStrategoTerm term) {
        return buildNoAnnosString(term, new StringBuilder()).toString();
    }

    public static StringBuilder buildNoAnnosString(IStrategoTerm term, StringBuilder b) {
        while(term instanceof StrategoAnnotation) {
            term = ((StrategoAnnotation) term).getWrapped();
        }
        if(Term.isTermAppl(term) || Term.isTermList(term) || Term.isTermTuple(term)) {
            // print constructor name
            if(Term.isTermAppl(term)) {
                b.append(((IStrategoAppl) term).getConstructor().getName());
            }

            // print opening brace
            if(Term.isTermAppl(term) || Term.isTermTuple(term)) {
                b.append('(');
            } else if(Term.isTermList(term)) {
                b.append('[');
            }

            // print children
            for(IStrategoTerm child : term.getAllSubterms()) {
                buildNoAnnosString(child, b);
                b.append(',');
            }
            if(term.getSubtermCount() > 0) {
                b.delete(b.length() - 1, b.length());
            }

            // print closing brace
            if(Term.isTermAppl(term) || Term.isTermTuple(term)) {
                b.append(')');
            } else if(Term.isTermList(term)) {
                b.append(']');
            }
        } else if(Term.isTermString(term)) {
            b.append(Term.asJavaString(term));
        } else if(Term.isTermInt(term)) {
            b.append(Term.asJavaInt(term));
        } else {
            logger.debug("Term {} is not a String or Int or thing with kids.", term.getClass());
            b.append(term.toString());
        }
        return b;
    }
}
