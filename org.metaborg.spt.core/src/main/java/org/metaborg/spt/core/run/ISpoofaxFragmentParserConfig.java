package org.metaborg.spt.core.run;

import javax.annotation.Nullable;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.mbt.core.run.IFragmentParserConfig;
import org.metaborg.spoofax.core.syntax.JSGLRParserConfiguration;

/**
 * Allows us to pass a parse configuration to the parser in our fragment parser.
 */
public interface ISpoofaxFragmentParserConfig extends IFragmentParserConfig {

    @Nullable JSGLRParserConfiguration getParserConfigForLanguage(ILanguageImpl lang);

    void putConfig(ILanguageImpl lang, JSGLRParserConfiguration config);

    void removeConfig(ILanguageImpl lang);
}
