package org.metaborg.spt.core.run;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.mbt.core.model.IFragment;
import org.metaborg.mbt.core.run.IFragmentParserConfig;
import org.metaborg.mbt.core.run.WhitespaceFragmentParser;
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService;
import org.metaborg.spoofax.core.syntax.JSGLRParserConfiguration;
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

    private final ISpoofaxInputUnitService inputService;

    @Inject public SpoofaxWhitespaceFragmentParser(ISpoofaxInputUnitService inputService,
        ISpoofaxSyntaxService parseService) {
        super(inputService, parseService);
        this.inputService = inputService;
    }

    @Override public ISpoofaxParseUnit parse(IFragment fragment, ILanguageImpl language, ILanguageImpl dialect,
        IFragmentParserConfig config) throws ParseException {
        if(!(config instanceof ISpoofaxFragmentParserConfig)) {
            return super.parse(fragment, language, dialect, config);
        } else {
            return parse(fragment, language, dialect, (ISpoofaxFragmentParserConfig) config);
        }
    }

    @Override public ISpoofaxParseUnit parse(IFragment fragment, ILanguageImpl language, ILanguageImpl dialect,
        ISpoofaxFragmentParserConfig config) throws ParseException {
        JSGLRParserConfiguration parseConfig = config == null ? null : config.getParserConfigForLanguage(language);
        if(parseConfig == null) {
            return super.parse(fragment, language, dialect, config);
        } else {
            String text = super.getWhitespacedFragmentText(fragment);
            ISpoofaxInputUnit input = inputService.inputUnit(text, language, dialect, parseConfig);
            return super.parse(input);
        }
    }

}
