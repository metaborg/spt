package org.metaborg.spt.core.spoofax;

import org.metaborg.core.context.IContext;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.FragmentResult;
import org.metaborg.spt.core.IFragment;

public class SpoofaxFragmentResult extends FragmentResult<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit>
    implements ISpoofaxFragmentResult {

    public SpoofaxFragmentResult(IFragment f, ISpoofaxParseUnit p, ISpoofaxAnalyzeUnit a, IContext c) {
        super(f, p, a, c);
    }

}
