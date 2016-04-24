package org.metaborg.spt.core.spoofax;

import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnitService;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spt.core.fragments.WhitespaceFragmentParser;

import com.google.inject.Inject;

public class SpoofaxWhitespaceFragmentParser extends WhitespaceFragmentParser<ISpoofaxInputUnit, ISpoofaxParseUnit>
    implements ISpoofaxFragmentParser {

    @Inject public SpoofaxWhitespaceFragmentParser(ISpoofaxInputUnitService inputService,
        ISpoofaxSyntaxService parseService) {
        super(inputService, parseService);
    }

}
