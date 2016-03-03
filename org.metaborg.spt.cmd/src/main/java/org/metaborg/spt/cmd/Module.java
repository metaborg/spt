package org.metaborg.spt.cmd;

import org.metaborg.core.editor.IEditorRegistry;
import org.metaborg.core.editor.NullEditorRegistry;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.core.project.SimpleProjectService;
import org.metaborg.spoofax.core.SpoofaxModule;
import org.metaborg.spt.core.ITestCaseExtractor;
import org.metaborg.spt.core.ITestExpectation;
import org.metaborg.spt.core.ITestRunner;
import org.metaborg.spt.core.TestCaseExtractor;
import org.metaborg.spt.core.TestRunner;

import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class Module extends SpoofaxModule {
    @Override protected void configure() {
        super.configure();

        bind(Runner.class).in(Singleton.class);

        // TODO: where should we bind these?
        // should we get an SPT module?
        bind(TestCaseExtractor.class).in(Singleton.class);
        bind(ITestCaseExtractor.class).to(TestCaseExtractor.class);
        bind(TestRunner.class).in(Singleton.class);
        bind(ITestRunner.class).to(TestRunner.class);

        // TODO: the parse test expectation should probably be in another project
        Multibinder<ITestExpectation> expectationBinder = Multibinder.newSetBinder(binder(), ITestExpectation.class);
        expectationBinder.addBinding().to(ParseExpectationTest.class);
    }

    @Override protected void bindProject() {
        bind(SimpleProjectService.class).in(Singleton.class);
        bind(IProjectService.class).to(SimpleProjectService.class);
        bind(ISimpleProjectService.class).to(SimpleProjectService.class);
    }

    @Override protected void bindEditor() {
        bind(IEditorRegistry.class).to(NullEditorRegistry.class).in(Singleton.class);
    }
}
