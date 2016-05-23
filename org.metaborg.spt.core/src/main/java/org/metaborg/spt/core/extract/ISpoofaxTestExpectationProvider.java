package org.metaborg.spt.core.extract;

import org.metaborg.mbt.core.extract.ITestExpectationProvider;
import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * Type interface for an ITestExpectationProvider that provides the ITestExpectation for a Spoofax AST node from an SPT
 * test suite.
 */
public interface ISpoofaxTestExpectationProvider extends ITestExpectationProvider<IStrategoTerm> {

}
