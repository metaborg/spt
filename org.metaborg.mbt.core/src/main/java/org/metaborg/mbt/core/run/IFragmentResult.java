package org.metaborg.mbt.core.run;

import jakarta.annotation.Nullable;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.context.IContext;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.syntax.IParseUnit;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.model.TestPhase;

/**
 * The result of the actions performed on a fragment.
 *
 * Right now the set of actions consists only of parsing and analysis.
 */
public interface IFragmentResult<P extends IParseUnit, A extends IAnalyzeUnit> {

    /**
     * The fragment on which the actions were performed.
     */
    IFragment getFragment();

    /**
     * The result of parsing the fragment with the language under test.
     */
    P getParseResult();

    /**
     * The result of analyzing the fragment with the language under test.
     *
     * May be null if the expectation only requires the {@link TestPhase#PARSING} phase, or if the parsing failed.
     */
    @Nullable A getAnalysisResult();

    /**
     * Messages produced by the analysis.
     *
     * May be null if the expectation only requires the {@link TestPhase#PARSING} phase, or if the parsing failed.
     */
    @Nullable Iterable<IMessage> getMessages();

    /**
     * The context that was used to analyze the input fragment.
     *
     * May be null.
     */
    @Nullable IContext getContext();
}
