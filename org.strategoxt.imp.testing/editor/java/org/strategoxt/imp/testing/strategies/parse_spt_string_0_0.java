package org.strategoxt.imp.testing.strategies;

import static org.spoofax.interpreter.core.Tools.isTermString;

import java.io.IOException;

import org.eclipse.imp.language.Language;
import org.eclipse.imp.language.LanguageRegistry;
import org.eclipse.imp.parser.IParseController;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.shared.BadTokenException;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.jsglr.shared.TokenExpectedException;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
import org.strategoxt.imp.runtime.dynamicloading.DynamicParseController;
import org.strategoxt.imp.runtime.parser.JSGLRI;
import org.strategoxt.imp.runtime.parser.SGLRParseController;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

/**
 * parse-spt-string strategy to get AST of Spoofax-Testing testsuite, 
 * where the input fragments have been annotated with the AST of the input.
 */
public class parse_spt_string_0_0 extends Strategy {

	public static parse_spt_string_0_0 instance = new parse_spt_string_0_0();
	
	@Override
	public IStrategoTerm invoke(Context context, IStrategoTerm current) {
		if (!isTermString(current)) return null;
		String input = ((IStrategoString)current ).stringValue();
		
		Language l = LanguageRegistry.findLanguage("Spoofax-Testing");
		Descriptor d = Environment.getDescriptor(l);
		IStrategoTerm result = null;
		try {				
			IParseController ip = d.createParseController();
			if (ip instanceof DynamicParseController)
				ip = ((DynamicParseController) ip).getWrapped();
			if (ip instanceof SGLRParseController) {
				SGLRParseController sglrController = (SGLRParseController) ip;
				JSGLRI parser = sglrController.getParser(); 
				parser.setUseRecovery(false);
			    result = parser.parse(input, "string");
			}
		} catch (BadDescriptorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TokenExpectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadTokenException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SGLRException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}

}
