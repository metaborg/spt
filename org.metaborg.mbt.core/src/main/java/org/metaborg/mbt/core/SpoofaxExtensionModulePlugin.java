package org.metaborg.mbt.core;

import org.metaborg.core.plugin.IServiceModulePlugin;
import org.metaborg.util.iterators.Iterables2;

import com.google.inject.Module;

public class SpoofaxExtensionModulePlugin implements IServiceModulePlugin {
    @Override public Iterable<Module> modules() {
        return Iterables2.from(new SpoofaxExtensionModule());
    }
}
