package org.strategoxt.imp.testing.cmd.strategies;

import java.util.List;

import org.strategoxt.lang.Context;
import org.strategoxt.lang.RegisteringStrategy;

public class LibraryInitializer extends org.strategoxt.lang.LibraryInitializer {

	@Override
	protected List<RegisteringStrategy> getLibraryStrategies() {
		throw new UnsupportedOperationException("Need to adapt org.strategoxt.imp.testing to separate compilation");
	}

	@Override
	protected void initializeLibrary(Context context) {
		throw new UnsupportedOperationException("Need to adapt org.strategoxt.imp.testing to separate compilation");
	}

}
