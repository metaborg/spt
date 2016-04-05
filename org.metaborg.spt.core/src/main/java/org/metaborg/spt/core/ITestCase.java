package org.metaborg.spt.core;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;
import org.spoofax.interpreter.terms.IStrategoTerm;

public interface ITestCase {

    /**
     * The description or name of the test case.
     */
    public String getDescription();

    /**
     * The fragment of this test case. I.e., the piece of code written in the language under test that is being tested.
     */
    public IFragment getFragment();

    /**
     * The source file of the test suite from which this test case was extracted. May be null.
     */
    public @Nullable FileObject getResource();

    /**
     * A list of tuples of an SPT AST term of a test expectation, and the ITestExpectation that can be used to evaluate
     * it. One for each expectation on this test case.
     */
    public List<ExpectationPair> getExpectations();

    /**
     * A tuple of a test expectation and an ITestExpectation that claims to be able to evaluate it.
     * 
     * Note that the evaluator can be null if no such ITestExpectation was found. Whether you consider that to be an
     * error is up to you.
     */
    public static class ExpectationPair {
        @Nullable public final ITestExpectation evaluator;
        public final IStrategoTerm expectation;

        public ExpectationPair(@Nullable ITestExpectation evaluator, IStrategoTerm expectation) {
            this.evaluator = evaluator;
            this.expectation = expectation;
        }
    }
}
