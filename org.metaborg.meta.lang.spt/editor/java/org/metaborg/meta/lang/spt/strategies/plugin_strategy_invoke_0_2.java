package org.metaborg.meta.lang.spt.strategies;

import static org.spoofax.interpreter.core.Tools.asJavaString;
import static org.spoofax.interpreter.core.Tools.isTermAppl;
import static org.spoofax.interpreter.core.Tools.termAt;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.context.ContextIdentifier;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.resource.ResourceService;
import org.metaborg.spoofax.core.context.SpoofaxContext;
import org.metaborg.spoofax.core.stratego.IStrategoRuntimeService;
import org.metaborg.spoofax.core.stratego.StrategoRuntimeService;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.core.UndefinedStrategyException;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.HybridInterpreter;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.common.collect.Iterables;
import com.google.inject.Injector;

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
    	final Injector injector = ((IContext)context.contextObject()).injector();
    	final IResourceService resourceService = injector.getInstance(IResourceService.class);
    	// TODO: is this factory ok? Or do we need to use the injector to get one?
        final ITermFactory factory = context.getFactory();
        final ILanguage lang = injector.getInstance(ILanguageService.class).getLanguage(asJavaString(languageName));
        final ILanguageImpl impl = lang.activeImpl();
        final FileObject location = resourceService.resolve(context.getIOAgent().getWorkingDir());
        final HybridInterpreter runtime;
        try {
            runtime =
                injector.getInstance(IStrategoRuntimeService.class).runtime(
                    Iterables.get(impl.components(), 0),
                    new SpoofaxContext(resourceService, new ContextIdentifier(location, impl), injector)
                );
        } catch(MetaborgException e) {
            return factory.makeAppl(factory.makeConstructor("Error", 1), factory.makeString(e.getLocalizedMessage()));
        }

        // strategy should be a String
        if(isTermAppl(strategy) && ((IStrategoAppl) strategy).getName().equals("Strategy"))
            strategy = termAt(strategy, 0);

        runtime.setCurrent(current);
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
