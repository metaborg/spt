package org.metaborg.spt.core.run;

import org.metaborg.core.context.IContext;
import org.metaborg.core.messages.IMessage;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.run.FragmentResult;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

public class SpoofaxFragmentResult extends FragmentResult<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit>
    implements ISpoofaxFragmentResult {

    public SpoofaxFragmentResult(IFragment f, ISpoofaxParseUnit p, ISpoofaxAnalyzeUnit a,
            Iterable<IMessage> messages, IContext c) {
        super(f, p, a, messages, c);
    }

}
