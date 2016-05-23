package org.metaborg.mbt.core;

import com.google.inject.AbstractModule;

/**
 * The module that registers an ITestCaseExtractor and ITestCaseRunner, as well as the core expectations.
 */
public abstract class MBTModule extends AbstractModule {

    @Override protected void configure() {
        configureExpectationProviders();
        configureExpectationEvaluators();
        configureExtractor();
        configureRunner();
        configureBuilders();
        configureFragmentParser();
    }

    protected void configureExpectationProviders() {
    }

    protected void configureExpectationEvaluators() {
    }

    protected abstract void configureExtractor();

    protected abstract void configureRunner();

    protected abstract void configureBuilders();

    protected abstract void configureFragmentParser();

    protected abstract void configureUtil();
}
