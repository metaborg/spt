package org.metaborg.spt.core;

import org.metaborg.spt.core.expectations.AnalyzeExpectation;
import org.metaborg.spt.core.expectations.FragmentUtil;
import org.metaborg.spt.core.expectations.ParseExpectation;
import org.metaborg.spt.core.expectations.ResolveExpectation;
import org.metaborg.spt.core.expectations.RunStrategoExpectation;
import org.metaborg.spt.core.fragments.TracingFragmentBuilder;
import org.metaborg.spt.core.fragments.WhitespaceFragmentParser;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

/**
 * The module that registers an ITestCaseExtractor and ITestCaseRunner, as well as the core expectations.
 */
public class SPTModule extends AbstractModule {
    @Override protected void configure() {
        bind(TestCaseExtractor.class).in(Singleton.class);
        bind(ITestCaseExtractor.class).to(TestCaseExtractor.class);
        bind(TestCaseRunner.class).in(Singleton.class);
        bind(ITestCaseRunner.class).to(TestCaseRunner.class);
        bind(ITestCaseBuilder.class).to(TestCaseBuilder.class);

        // we rely on ImploderAttachments to be present
        bind(IFragmentBuilder.class).to(TracingFragmentBuilder.class);

        // for now, we keep using the whitespace hack
        bind(WhitespaceFragmentParser.class).in(Singleton.class);
        bind(IFragmentParser.class).to(WhitespaceFragmentParser.class);

        // bindings for expectations
        Multibinder<ITestExpectation> expectationBinder = Multibinder.newSetBinder(binder(), ITestExpectation.class);
        expectationBinder.addBinding().to(ParseExpectation.class);
        expectationBinder.addBinding().to(AnalyzeExpectation.class);
        expectationBinder.addBinding().to(ResolveExpectation.class);
        expectationBinder.addBinding().to(RunStrategoExpectation.class);

        bind(FragmentUtil.class);
    }
}
