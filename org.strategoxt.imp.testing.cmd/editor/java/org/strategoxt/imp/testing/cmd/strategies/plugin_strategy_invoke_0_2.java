package org.strategoxt.imp.testing.cmd.strategies;

import static org.spoofax.interpreter.core.Tools.asJavaString;
import static org.spoofax.interpreter.core.Tools.isTermAppl;
import static org.spoofax.interpreter.core.Tools.termAt;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.SpoofaxException;
import org.metaborg.core.context.ContextIdentifier;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.resource.ResourceService;
import org.metaborg.spoofax.core.context.SpoofaxContext;
import org.metaborg.spoofax.core.stratego.StrategoRuntimeService;
import org.metaborg.sunshine.environment.ServiceRegistry;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.core.UndefinedStrategyException;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.HybridInterpreter;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

/**
 * Evaluate a strategy in a stratego instance belonging to a language plugin.
 */
public class plugin_strategy_invoke_0_2 extends Strategy {
    public static final plugin_strategy_invoke_0_2 instance = new plugin_strategy_invoke_0_2();


    /**
     * @return Fail(trace) for strategy failure, Error(message) a string for errors, or Some(term) for success.
     */
    @Override public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm languageName,
        IStrategoTerm strategy) {
        final ITermFactory factory = context.getFactory();
        final ServiceRegistry env = ServiceRegistry.INSTANCE();
        final ILanguage lang = env.getService(ILanguageService.class).get(asJavaString(languageName));
        final FileObject location = env.getService(ResourceService.class).resolve(context.getIOAgent().getWorkingDir());
        final HybridInterpreter runtime;
        try {
            runtime =
                env.getService(StrategoRuntimeService.class).runtime(
                    new SpoofaxContext(env.getService(ResourceService.class), new ContextIdentifier(location, lang)));
        } catch(SpoofaxException e) {
            return factory.makeAppl(factory.makeConstructor("Error", 1), factory.makeString(e.getLocalizedMessage()));
        }

        // strategy should be a String
        if(isTermAppl(strategy) && ((IStrategoAppl) strategy).getName().equals("Strategy"))
            strategy = termAt(strategy, 0);

        runtime.setCurrent(current);
        // TODO catch blocks copied from eclipse-specific code.
        // Why do we need them? Don't we want to fail fast?
        try {
            if(runtime.invoke(asJavaString(strategy))) {
                current = runtime.current();
                current = factory.makeAppl(factory.makeConstructor("Some", 1), current);
                return current;
            }

            final Context foreignContext = runtime.getCompiledContext();
            final String trace = "rewriting failed\n" + (foreignContext != null ? foreignContext.getTraceString() : "");
            return factory.makeAppl(factory.makeConstructor("Fail", 1), factory.makeString(trace));
        } catch(UndefinedStrategyException e) {
            return factory.makeAppl(factory.makeConstructor("Error", 1),
                factory.makeString("Problem executing foreign strategy for testing: " + e.getLocalizedMessage()));
        } catch(InterpreterException e) {
            return factory.makeAppl(factory.makeConstructor("Error", 1), factory.makeString(e.getLocalizedMessage()));
        } catch(RuntimeException e) {
            return factory.makeAppl(factory.makeConstructor("Error", 1),
                factory.makeString(e.getClass().getName() + ": " + e.getLocalizedMessage() + " (see error log)"));
        }
    }
}
