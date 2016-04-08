package org.metaborg.spt.core.util;

import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.Term;

public class SPTUtil {

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
}
