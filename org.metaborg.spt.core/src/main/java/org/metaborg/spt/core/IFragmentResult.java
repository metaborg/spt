package org.metaborg.spt.core;

import javax.annotation.Nullable;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.context.IContext;
import org.metaborg.core.syntax.IParseUnit;

/**
 * The result of the actions performed on a fragment.
 * 
 * Right now the set of actions consists only of parsing and analysis.
 */
public interface IFragmentResult<P extends IParseUnit, A extends IAnalyzeUnit> {

    /**
     * The fragment on which the actions were performed.
     */
    public IFragment getFragment();

    /**
     * The result of parsing the fragment with the language under test.
     */
    public P getParseResult();

    /**
     * The result of analyzing the fragment with the language under test.
     * 
     * May be null if the expectation only requires the {@link TestPhase#PARSING} phase, or if the parsing failed.
     */
    public @Nullable A getAnalysisResult();

    /**
     * The context that was used to analyze the input fragment.
     * 
     * May be null.
     */
    public @Nullable IContext getContext();
}
