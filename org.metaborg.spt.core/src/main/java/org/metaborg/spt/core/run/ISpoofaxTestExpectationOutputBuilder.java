package org.metaborg.spt.core.run;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.mbt.core.run.ITestExpectationOutputBuilder;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

import jakarta.annotation.Nullable;

/**
 * Type interface for the output of evaluating a test expectation when running the test on Spoofax languages.
 */
public interface ISpoofaxTestExpectationOutputBuilder extends ITestExpectationOutputBuilder<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> {

    /**
     * Builds a test expectation output.
     *
     * @param success whether the test expectation was met
     * @return the built test expectation output
     */
    @Override ISpoofaxTestExpectationOutput build(boolean success);

    /**
     * Returns an output builder whose messages have the specified resource.
     *
     * Any operations on the returned output builder affect this output builder too.
     *
     * @param resource the new resource; or {@code null}
     * @return the output builder
     */
    @Override ISpoofaxTestExpectationOutputBuilder withResource(@Nullable FileObject resource);

    /**
     * Returns an output builder whose messages have the specified region.
     *
     * Any operations on the returned output builder affect this output builder too.
     *
     * @param region the new region; or {@code null}
     * @return the output builder
     */
    @Override ISpoofaxTestExpectationOutputBuilder withRegion(@Nullable ISourceRegion region);

    /**
     * Adds a fragment result.
     *
     * @param result the fragment result
     */
    void addFragmentResult(ISpoofaxFragmentResult result);

}
