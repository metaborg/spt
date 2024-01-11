package org.metaborg.spt.core.run;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Nullable;

import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spoofax.core.syntax.JSGLRParserConfiguration;

public class SpoofaxFragmentParserConfig implements ISpoofaxFragmentParserConfig {

    private final Map<ILanguageImpl, JSGLRParserConfiguration> configMap = new HashMap<>();

    @Override public void putConfig(ILanguageImpl lang, JSGLRParserConfiguration config) {
        configMap.put(lang, config);
    }

    @Override public void removeConfig(ILanguageImpl lang) {
        configMap.remove(lang);
    }

    @Override public @Nullable JSGLRParserConfiguration getParserConfigForLanguage(ILanguageImpl lang) {
        return configMap.get(lang);
    }

}
