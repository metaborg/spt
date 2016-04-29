package org.metaborg.spt.core.spoofax;

import org.metaborg.core.messages.IMessage;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.IFragmentResult;
import org.metaborg.spt.core.TestExpectationOutput;

import com.google.common.collect.Lists;

public class SpoofaxTestExpectationOutput extends TestExpectationOutput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit>
    implements ISpoofaxTestExpectationOutput {

    private final Iterable<ISpoofaxFragmentResult> fragmentResults;

    public SpoofaxTestExpectationOutput(boolean success, Iterable<IMessage> messages,
        Iterable<ISpoofaxFragmentResult> fragmentResults) {
        super(success, messages,
            Lists.<IFragmentResult<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit>>newLinkedList(fragmentResults));
        this.fragmentResults = fragmentResults;
    }

    @Override public Iterable<ISpoofaxFragmentResult> getFragmentResults() {
        return fragmentResults;
    }
}
