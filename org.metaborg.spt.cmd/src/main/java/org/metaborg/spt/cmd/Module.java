package org.metaborg.spt.cmd;

import org.metaborg.core.editor.IEditorRegistry;
import org.metaborg.core.editor.NullEditorRegistry;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.core.project.SimpleProjectService;
import org.metaborg.core.testing.ITestReporterService;
import org.metaborg.core.testing.LoggingTestReporterService;
import org.metaborg.spoofax.core.SpoofaxModule;

import com.google.inject.Singleton;

import javax.annotation.Nullable;

public class Module extends SpoofaxModule {

    @Nullable
    private final String customReporterClassName;

    public Module(@Nullable String customReporterClassName) {
        this.customReporterClassName = customReporterClassName;
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
        Class<? extends ITestReporterService> reporterClass = getClassByNameOrDefault(this.customReporterClassName, LoggingTestReporterService.class);
        // Bind the (custom or default) reporter.
        bind(ITestReporterService.class).to(reporterClass).in(Singleton.class);
    }

    private <T> Class<? extends T> getClassByNameOrDefault(@Nullable String className, Class<? extends T> defaultClass) {
        if (className == null)
            return defaultClass;
        try {
            return getClassByName(className);
        } catch (ClassNotFoundException | ClassCastException e) {
            // Fallback to default.
            throw new RuntimeException(e);
            // TODO: Return default instead of throwing an error:
            // TODO: Should log when given class could not be loaded!
//            return defaultClass;
        }
    }

    private <T> Class<? extends T> getClassByName(String className) throws ClassNotFoundException {
        return (Class<? extends T>) Class.forName(className);
    }
}
