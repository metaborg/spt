package org.strategoxt.imp.testing;

import org.eclipse.imp.parser.IParseController;
import org.strategoxt.imp.runtime.parser.JSGLRI;
import org.strategoxt.imp.runtime.parser.SGLRParseController;

public class SpoofaxTestingParseController extends
		SpoofaxTestingParseControllerGenerated {
	
	@Override
	public IParseController getWrapped() {
		IParseController result = super.getWrapped();
		if (result instanceof SGLRParseController) {
			JSGLRI parser = ((SGLRParseController) result).getParser();
			if (!(parser instanceof SpoofaxTestingJSGLRI)) {
				((SGLRParseController) result).setParser(new SpoofaxTestingJSGLRI(parser));
			}
		}
		return result;
	}
}