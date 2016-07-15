package org.metaborg.spt.core.run;

import org.metaborg.mbt.core.run.IFragmentResult;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

/**
 * Type interface for an IFragmentResult of applying an action (i.e. parse or analyze) on an IFragment, using a Spoofax
 * language.
 */
public interface ISpoofaxFragmentResult extends IFragmentResult<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit> {

}
