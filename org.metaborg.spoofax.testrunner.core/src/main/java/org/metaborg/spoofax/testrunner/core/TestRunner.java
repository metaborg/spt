package org.metaborg.spoofax.testrunner.core;

import java.io.IOException;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.spoofax.core.language.ILanguageDiscoveryService;
import org.metaborg.spoofax.core.resource.IResourceService;
import org.metaborg.sunshine.drivers.SunshineMainDriver;
import org.metaborg.sunshine.environment.ServiceRegistry;
import org.metaborg.sunshine.environment.SunshineMainArguments;

public class TestRunner {
    public final ServiceRegistry services;


    public TestRunner(String testsLocation, String sptBuilder) {
        final SunshineMainArguments params = new SunshineMainArguments();
        params.project = testsLocation;
        params.filestobuildon = ".";
        params.noanalysis = true;
        params.nonincremental = true;
        params.builder = sptBuilder;
        params.filefilter = ".+\\.spt";
        org.metaborg.sunshine.drivers.Main.initEnvironment(params);

        this.services = ServiceRegistry.INSTANCE();
    }


    public void registerLanguage(FileObject targetLangLocation) throws Exception {
        final ILanguageDiscoveryService discovery = services.getService(ILanguageDiscoveryService.class);
        final IResourceService resources = services.getService(IResourceService.class);
        discovery.discover(targetLangLocation);
        discovery.discover(resources.resolve("res:spt"));
    }

    public int run() throws IOException {
        final SunshineMainDriver driver = services.getService(SunshineMainDriver.class);
        return driver.run();
    }
}
