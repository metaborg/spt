package org.metaborg.spt.core.extract;

import org.metaborg.mbt.core.extract.ITestCaseBuilder;
import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * Type interface for a builder that can create an ITestCase from the AST nodes of a Spoofax SPT test case
 * specification.
 */
public interface ISpoofaxTestCaseBuilder extends ITestCaseBuilder<IStrategoTerm, IStrategoTerm> {

}
