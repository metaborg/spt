package org.metaborg.spt.core;

import javax.annotation.Nullable;

import org.metaborg.core.analysis.IAnalyzeUnit;
import org.metaborg.core.context.IContext;
import org.metaborg.core.syntax.IParseUnit;

public class FragmentResult<P extends IParseUnit, A extends IAnalyzeUnit> implements IFragmentResult<P, A> {

    private final IFragment f;
    private final P p;
    private final A a;
    private final @Nullable IContext c;

    public FragmentResult(IFragment f, P p, A a, IContext c) {
        this.f = f;
        this.p = p;
        this.a = a;
        this.c = c;
    }

    @Override public IFragment getFragment() {
        return f;
    }

    @Override public P getParseResult() {
        return p;
    }

    @Override public A getAnalysisResult() {
        return a;
    }

    @Override public IContext getContext() {
        return c;
    }

}
