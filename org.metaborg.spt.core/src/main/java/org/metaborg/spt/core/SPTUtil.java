package org.metaborg.spt.core;

import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spoofax.core.syntax.JSGLRSourceRegionFactory;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.terms.Term;

public class SPTUtil {

    public static ISourceRegion getRegion(IStrategoTerm term) {
        ImploderAttachment imploder = ImploderAttachment.get(term);
        IToken left = imploder.getLeftToken();
        IToken right = imploder.getRightToken();
        return JSGLRSourceRegionFactory.fromTokens(left, right);
    }

    public static String getFragmentText(IStrategoTerm fragment) {
        if(Term.isTermAppl(fragment)) {
            IStrategoAppl f = (IStrategoAppl) fragment;
            final String cons = f.getConstructor().getName();
            if("Done".equals(cons)) {
                // it's Done()
                return "";
            } else if("More".equals(cons)) {
                // it's More(Selection(...), "stringpart", <TailPart>)
                final String selection = getFragmentText(f.getSubterm(0));
                final String stringPart = Tools.asJavaString(f.getSubterm(1));
                final String tailPart = getFragmentText(f.getSubterm(2));
                return selection + stringPart + tailPart;
            } else if("Selection2".equals(cons) || "Selection3".equals(cons) || "Selection4".equals(cons)) {
                // it's Selection(marker, "stringpart", marker)
                return Tools.asJavaString(f.getSubterm(1));
            } else if("Fragment2".equals(cons) || "Fragment3".equals(cons) || "Fragment4".equals(cons)) {
                // it's Fragment("stringpart", <TailPart>)
                return Tools.asJavaString(f.getSubterm(0)) + getFragmentText(f.getSubterm(1));
            }
        }
        throw new IllegalArgumentException("Can't get the string out of: " + fragment);
    }
}
