package org.metaborg.spoofax.testrunner.core;

import java.io.IOException;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.spoofax.core.language.ILanguageDiscoveryService;
import org.metaborg.spoofax.core.resource.IResourceService;
import org.metaborg.sunshine.drivers.SunshineMainDriver;
import org.metaborg.sunshine.environment.ServiceRegistry;
import org.metaborg.sunshine.environment.SunshineMainArguments;

public class TestRunner {
    public final ServiceRegistry services;

    private final ILanguageDiscoveryService discovery;
    private final IResourceService resources;


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
        this.discovery = services.getService(ILanguageDiscoveryService.class);
        this.resources = services.getService(IResourceService.class);
    }


    public void registerSPT() throws Exception {
        final FileObject sptLocation = resources.resolve("res:spt");
        final FileObject tmpSPTLocation = resources.resolve(System.getProperty("java.io.tmpdir") + "/spt");
        tmpSPTLocation.delete(new AllFileSelector());
        tmpSPTLocation.copyFrom(sptLocation, new AllFileSelector());
        discovery.discover(tmpSPTLocation);
    }

    public void registerLanguage(String targetLangLocation) throws Exception {
        discovery.discover(resources.resolve(targetLangLocation));
    }

    public int run() throws IOException {
        final SunshineMainDriver driver = services.getService(SunshineMainDriver.class);
        return driver.run();
    }
}
