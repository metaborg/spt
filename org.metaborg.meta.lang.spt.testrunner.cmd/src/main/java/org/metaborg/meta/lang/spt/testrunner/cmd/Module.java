package org.metaborg.meta.lang.spt.testrunner.cmd;

import org.metaborg.core.editor.IEditorRegistry;
import org.metaborg.core.editor.NullEditorRegistry;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.core.project.SimpleProjectService;
import org.metaborg.spoofax.core.SpoofaxModule;
import org.metaborg.spoofax.core.project.ILegacyMavenProjectService;
import org.metaborg.spoofax.core.project.NullLegacyMavenProjectService;
import org.metaborg.spoofax.meta.spt.testrunner.core.TestRunner;

import com.google.inject.Singleton;

public class Module extends SpoofaxModule {
    @Override protected void configure() {
        super.configure();

        bind(TestRunner.class).in(Singleton.class);
        bind(Runner.class).in(Singleton.class);
    }

    @Override protected void bindProject() {
        bind(SimpleProjectService.class).in(Singleton.class);
        bind(IProjectService.class).to(SimpleProjectService.class).in(Singleton.class);
        bind(ISimpleProjectService.class).to(SimpleProjectService.class).in(Singleton.class);
    }

    @Override protected void bindMavenProject() {
        bind(ILegacyMavenProjectService.class).to(NullLegacyMavenProjectService.class).in(Singleton.class);
    }

    @Override protected void bindEditor() {
        bind(IEditorRegistry.class).to(NullEditorRegistry.class).in(Singleton.class);
    }
}
