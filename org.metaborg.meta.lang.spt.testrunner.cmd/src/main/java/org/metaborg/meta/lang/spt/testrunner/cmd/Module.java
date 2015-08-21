package org.metaborg.meta.lang.spt.testrunner.cmd;

import org.metaborg.spoofax.core.SpoofaxModule;
import org.metaborg.spoofax.meta.spt.testrunner.core.TestRunner;

import com.google.inject.Singleton;

public class Module extends SpoofaxModule {
    @Override protected void configure() {
        super.configure();

        bind(TestRunner.class).in(Singleton.class);
        bind(Runner.class).in(Singleton.class);
    }
}
