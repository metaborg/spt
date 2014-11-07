package org.strategoxt.imp.testing.cmd.strategies;

import org.apache.commons.vfs2.FileSystemException;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class get_service_input_term_0_1 extends Strategy {
    public static final get_service_input_term_0_1 instance = new get_service_input_term_0_1();


    @Override public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm analyzedAst) {
        if(Tools.isTermAppl(analyzedAst) && ((IStrategoAppl) analyzedAst).getName().equals("None"))
            analyzedAst = null;

        try {
            final InputTermBuilder inputBuilder = new InputTermBuilder(context);
            return inputBuilder.makeInputTerm(analyzedAst, current, true, false);
        } catch(FileSystemException e) {
            throw new RuntimeException("Could not create input term", e);
        }
    }
}
