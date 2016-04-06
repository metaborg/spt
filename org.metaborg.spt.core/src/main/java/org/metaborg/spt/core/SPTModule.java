package org.metaborg.spt.core;

import org.metaborg.spoofax.core.SpoofaxModule;
import org.metaborg.spt.core.expectations.ParseExpectation;
import org.metaborg.spt.core.fragments.ImploderFragmentBuilder;
import org.metaborg.spt.core.fragments.WhitespaceFragmentParser;

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
        bind(TestCaseRunner.class).in(Singleton.class);
        bind(ITestCaseRunner.class).to(TestCaseRunner.class);
        bind(ITestCaseBuilder.class).to(TestCaseBuilder.class);

        // we rely on ImploderAttachments to be present
        bind(IFragmentBuilder.class).to(ImploderFragmentBuilder.class);

        // for now, we keep using the whitespace hack
        bind(WhitespaceFragmentParser.class).in(Singleton.class);
        bind(IFragmentParser.class).to(WhitespaceFragmentParser.class);

        Multibinder<ITestExpectation> expectationBinder = Multibinder.newSetBinder(binder(), ITestExpectation.class);
        expectationBinder.addBinding().to(ParseExpectation.class);
    }
}
