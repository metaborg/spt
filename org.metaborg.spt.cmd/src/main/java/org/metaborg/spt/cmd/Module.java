package org.metaborg.spt.cmd;

import org.metaborg.core.editor.IEditorRegistry;
import org.metaborg.core.editor.NullEditorRegistry;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.core.project.SimpleProjectService;
import org.metaborg.core.testing.ITestReporterService;
import org.metaborg.spoofax.core.SpoofaxModule;

import com.google.inject.Singleton;

import jakarta.annotation.Nullable;

public class Module extends SpoofaxModule {

    @Nullable
    private final Class<? extends ITestReporterService> customReporterClass;

    public Module(@Nullable Class<? extends ITestReporterService> customReporterClass) {
        this.customReporterClass = customReporterClass;
    }

    @Override protected void configure() {
        super.configure();

        bind(Runner.class).in(Singleton.class);
    }

    @Override protected void bindProject() {
        bind(SimpleProjectService.class).in(Singleton.class);
        bind(IProjectService.class).to(SimpleProjectService.class);
        bind(ISimpleProjectService.class).to(SimpleProjectService.class);
    }

    @Override protected void bindEditor() {
        bind(IEditorRegistry.class).to(NullEditorRegistry.class).in(Singleton.class);
    }

    @Override
    protected void bindTestReporter() {
        if (this.customReporterClass != null) {
            // Bind the custom reporter.
            bind(ITestReporterService.class).to(this.customReporterClass).in(Singleton.class);
        } else {
            // Bind the default reporter.
            super.bindTestReporter();
        }
    }
}
