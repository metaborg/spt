package org.metaborg.spt.core.run;

import org.metaborg.mbt.core.run.WhitespaceFragmentParser;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnitService;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

import com.google.inject.Inject;

/**
 * Use this parser to parse fragments with Spoofax languages.
 * 
 * Instantiates the {@link SpoofaxWhitespaceFragmentParser} with Spoofax types.
 */
public class SpoofaxWhitespaceFragmentParser extends WhitespaceFragmentParser<ISpoofaxInputUnit, ISpoofaxParseUnit>
    implements ISpoofaxFragmentParser {

    @Inject public SpoofaxWhitespaceFragmentParser(ISpoofaxInputUnitService inputService,
        ISpoofaxSyntaxService parseService) {
        super(inputService, parseService);
    }

}
