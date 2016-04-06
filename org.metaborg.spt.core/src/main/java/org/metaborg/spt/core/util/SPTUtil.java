package org.metaborg.spt.core.util;

import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.MessageBuilder;
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
     * Create a new message with the same information, but the given region.
     * 
     * @param m
     *            the message to copy.
     * @param r
     *            the region to set on the new message.
     * @return the new message with the new region.
     */
    public static IMessage setRegion(IMessage m, ISourceRegion r) {
        MessageBuilder b = MessageBuilder.create();
        b.withMessage(m.message());
        b.withSeverity(m.severity());
        b.withType(m.type());
        b.withRegion(r);
        if(m.source() != null) {
            b.withSource(m.source());
        }
        if(m.exception() != null) {
            b.withException(m.exception());
        }
        return b.build();
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
