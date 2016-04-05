package org.metaborg.spt.core.util;

import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spoofax.core.syntax.JSGLRSourceRegionFactory;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.terms.Term;

public class SPTUtil {

    public static final String TEST_CONS = "Test";
    public static final String SELECTION_CONS = "Selection";
    public static final String FRAGMENT_CONS = "Fragment";
    public static final String TAILPART_DONE_CONS = "Done";
    public static final String TAILPART_MORE_CONS = "More";

    public static ISourceRegion getRegion(IStrategoTerm term) {
        ImploderAttachment imploder = ImploderAttachment.get(term);
        IToken left = imploder.getLeftToken();
        IToken right = imploder.getRightToken();
        return JSGLRSourceRegionFactory.fromTokens(left, right);
    }

    /**
     * Get the text representing the given Fragment. All SPT related characters (Selection's markers) will be removed.
     */
    public static String getFragmentText(IStrategoTerm fragment) {
        if(Term.isTermAppl(fragment)) {
            IStrategoAppl f = (IStrategoAppl) fragment;
            final String cons = f.getConstructor().getName();
            if(TAILPART_DONE_CONS.equals(cons)) {
                // it's Done()
                return "";
            } else if(TAILPART_MORE_CONS.equals(cons)) {
                // it's More(Selection(...), "stringpart", <TailPart>)
                final String selection = getFragmentText(f.getSubterm(0));
                final String stringPart = Tools.asJavaString(f.getSubterm(1));
                final String tailPart = getFragmentText(f.getSubterm(2));
                return selection + stringPart + tailPart;
            } else if(SELECTION_CONS.equals(cons)) {
                // it's Selection(marker, "stringpart", marker)
                return Tools.asJavaString(f.getSubterm(1));
            } else if(FRAGMENT_CONS.equals(cons)) {
                // it's Fragment("stringpart", <TailPart>)
                return Tools.asJavaString(f.getSubterm(0)) + getFragmentText(f.getSubterm(1));
            }
        }
        throw new IllegalArgumentException("Can't get the string out of: " + fragment);
    }

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
