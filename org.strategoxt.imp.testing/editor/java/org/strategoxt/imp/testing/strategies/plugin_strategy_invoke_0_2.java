package org.strategoxt.imp.testing.strategies;

import static org.spoofax.interpreter.core.Tools.asJavaString;

import org.eclipse.imp.language.LanguageRegistry;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.IOAgent;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
import org.strategoxt.imp.runtime.services.StrategoObserver;
import org.strategoxt.imp.runtime.stratego.EditorIOAgent;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.MissingStrategyException;
import org.strategoxt.lang.Strategy;

/**
 * Evaluate a strategy in a stratego instance belonging to a language plugin.
 * 
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class plugin_strategy_invoke_0_2 extends Strategy {

	public static plugin_strategy_invoke_0_2 instance = new plugin_strategy_invoke_0_2();

	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm languageName, IStrategoTerm strategy) {
		try {
			Descriptor descriptor = Environment.getDescriptor(LanguageRegistry.findLanguage(asJavaString(languageName)));
			if (descriptor == null) throw new BadDescriptorException("No language known with the name " + languageName);
	        StrategoObserver observer = descriptor.createService(StrategoObserver.class, null);
	        IOAgent ioAgent = context.getIOAgent();
	        if (ioAgent instanceof EditorIOAgent) {
	        	// Make the console visible to users
	        	((EditorIOAgent) ioAgent).getDescriptor().setDynamicallyLoaded(true);
	        }
			observer.getRuntime().setIOAgent(ioAgent);
			observer.getRuntime().setCurrent(current);
			if (observer.getRuntime().invoke(asJavaString(strategy))) {
				return observer.getRuntime().current();
			} else {
				observer.reportRewritingFailed();
				return null;
			}
		} catch (MissingStrategyException e) {
			return null;
		} catch (BadDescriptorException e) {
			Environment.logException("Problem loading descriptor for testing", e);
			return null;
		} catch (InterpreterException e) {
			Environment.logException("Problem executing strategy for testing: " + strategy, e);
			return null;
		} catch (RuntimeException e) {
			Environment.logException("Problem executing strategy for testing: " + strategy, e);
			return null;
		}
	}

}
