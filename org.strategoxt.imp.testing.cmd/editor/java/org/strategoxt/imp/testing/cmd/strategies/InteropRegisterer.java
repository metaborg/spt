package org.strategoxt.imp.testing.cmd.strategies;

import org.strategoxt.lang.JavaInteropRegisterer;

public class InteropRegisterer extends JavaInteropRegisterer {

    public InteropRegisterer() {
        super(new LibraryInitializer());
    }
}
