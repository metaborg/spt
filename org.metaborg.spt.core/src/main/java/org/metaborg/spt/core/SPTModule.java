package org.metaborg.spt.core;

import org.metaborg.spoofax.core.SpoofaxModule;
import org.metaborg.spt.core.expectations.ParseExpectation;

import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

/**
 * The module that registers an ITestCaseExtractor and ITestCaseRunner, as well as the core expectations.
 */
public class SPTModule extends SpoofaxModule {

    @Override protected void configure() {
        super.configure();

        bind(TestCaseExtractor.class).in(Singleton.class);
        bind(ITestCaseExtractor.class).to(TestCaseExtractor.class);
        bind(WhitespacingTestCaseRunner.class).in(Singleton.class);
        bind(ITestCaseRunner.class).to(WhitespacingTestCaseRunner.class);
        bind(TestCaseBuilder.class).in(Singleton.class);
        bind(ITestCaseBuilder.class).to(TestCaseBuilder.class);

        Multibinder<ITestExpectation> expectationBinder = Multibinder.newSetBinder(binder(), ITestExpectation.class);
        expectationBinder.addBinding().to(ParseExpectation.class);
    }
}
