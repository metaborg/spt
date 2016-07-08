package org.metaborg.mbt.core;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class SpoofaxExtensionModule extends AbstractModule {
    @Override protected void configure() {
        // bind our classloader to strategoRuntimeClassloaderBinder
        Multibinder.newSetBinder(binder(), ClassLoader.class).addBinding().toInstance(getClass().getClassLoader());
    }
}
