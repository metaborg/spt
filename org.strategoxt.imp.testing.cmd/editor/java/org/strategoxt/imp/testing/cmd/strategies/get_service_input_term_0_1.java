package org.strategoxt.imp.testing.cmd.strategies;

import org.apache.commons.vfs2.FileSystemException;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

/**
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class get_service_input_term_0_1 extends Strategy {
    public static get_service_input_term_0_1 instance = new get_service_input_term_0_1();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm analyzedAst) {
        // TODO: adapt to latest strategy of StrategoReferenceResolver?
        if(Tools.isTermAppl(analyzedAst) && ((IStrategoAppl) analyzedAst).getName().equals("None"))
            analyzedAst = null;
        // if (!"COMPLETION".equals(tryGetName(current))
        // && !"NOCONTEXT".equals(tryGetName(current)))
        // current = InputTermBuilder.getMatchingAncestor(current,
        // // StrategoReferenceResolver.ALLOW_MULTI_CHILD_PARENT);
        // // FIXME hardcoded constant due to dirty porting to Sunshine
        // false);
        InputTermBuilder inputBuilder = new InputTermBuilder(context);
        try {
            return inputBuilder.makeInputTerm(analyzedAst, current, true, false);
        } catch(FileSystemException e) {
            throw new RuntimeException("Could not create input term", e);
        }
    }
}
