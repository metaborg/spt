package org.metaborg.mbt.core.model.expectations;

import org.metaborg.core.source.ISourceRegion;

/**
 * During test case extraction, ITestExpectationProviders create an ITestExpectation for each expectation AST node.
 * 
 * When the test is being run, we look for an IExpectationEvaluator that can handle the ITestExpectation. These
 * evaluators should be registered in Guice with the class of the subclass.
 */
public interface ITestExpectation {

    ISourceRegion region();
}
