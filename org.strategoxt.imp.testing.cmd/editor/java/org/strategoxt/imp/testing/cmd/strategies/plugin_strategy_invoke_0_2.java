package org.strategoxt.imp.testing.cmd.strategies;

import static org.spoofax.interpreter.core.Tools.*;

import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.language.ILanguageService;
import org.metaborg.sunshine.environment.ServiceRegistry;
import org.metaborg.sunshine.services.RuntimeService;
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
     * @return Fail(trace) for strategy failure, Error(message) a string for errors, or Some(term) for
     *         success.
     */
    @Override public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm languageName,
        IStrategoTerm strategy) {
        ITermFactory factory = context.getFactory();

        ServiceRegistry env = ServiceRegistry.INSTANCE();
        ILanguage lang = env.getService(ILanguageService.class).get(asJavaString(languageName));
        HybridInterpreter runtime = env.getService(RuntimeService.class).getRuntime(lang);

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
            } else {
                Context foreignContext = runtime.getCompiledContext();
                String trace =
                    "rewriting failed\n" + (foreignContext != null ? foreignContext.getTraceString() : "");
                return factory.makeAppl(factory.makeConstructor("Fail", 1), factory.makeString(trace));
            }
        } catch(UndefinedStrategyException e) {
            return factory.makeAppl(
                factory.makeConstructor("Error", 1),
                factory.makeString("Problem executing foreign strategy for testing: "
                    + e.getLocalizedMessage()));
        } catch(InterpreterException e) {
            return factory.makeAppl(factory.makeConstructor("Error", 1),
                factory.makeString(e.getLocalizedMessage()));
        } catch(RuntimeException e) {
            return factory.makeAppl(
                factory.makeConstructor("Error", 1),
                factory.makeString(e.getClass().getName() + ": " + e.getLocalizedMessage()
                    + " (see error log)"));
        }
    }
}
