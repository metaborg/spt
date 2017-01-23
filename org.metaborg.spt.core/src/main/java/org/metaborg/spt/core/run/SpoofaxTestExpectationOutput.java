package org.metaborg.spt.core.run;

import java.util.List;

import org.metaborg.core.messages.IMessage;
import org.metaborg.mbt.core.run.IFragmentResult;
import org.metaborg.mbt.core.run.ITestExpectationOutput;
import org.metaborg.mbt.core.run.TestExpectationOutput;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

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

    public SpoofaxTestExpectationOutput(ITestExpectationOutput<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> result) {
        super(result.isSuccessful(), result.getMessages(), result.getFragmentResults());
        final List<ISpoofaxFragmentResult> fragmentResults = Lists.newArrayList();
        for(IFragmentResult<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> fragmentResult : result.getFragmentResults()) {
            fragmentResults.add((ISpoofaxFragmentResult) fragmentResult);
        }
        this.fragmentResults = fragmentResults;
    }


    @Override public Iterable<ISpoofaxFragmentResult> getFragmentResults() {
        return fragmentResults;
    }
}
