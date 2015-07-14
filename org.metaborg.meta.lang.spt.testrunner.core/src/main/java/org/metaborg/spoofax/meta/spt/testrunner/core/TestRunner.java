package org.metaborg.spoofax.meta.spt.testrunner.core;

import java.io.IOException;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.language.ILanguageDiscoveryService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.sunshine.drivers.SunshineMainDriver;
import org.metaborg.sunshine.environment.ServiceRegistry;
import org.metaborg.sunshine.environment.SunshineMainArguments;

public class TestRunner {
    private final ServiceRegistry services;
    private final ILanguageDiscoveryService discovery;
    private final IResourceService resources;

    private FileObject tmpSPTLocation;


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
        tmpSPTLocation = resources.resolve(System.getProperty("java.io.tmpdir") + "/spt");
        final FileObject sptLocation = resources.resolve("res:spt");
        tmpSPTLocation.delete(new AllFileSelector());
        tmpSPTLocation.copyFrom(sptLocation, new AllFileSelector());
        discovery.discover(tmpSPTLocation);
    }

    public void registerLanguage(String targetLangLocation) throws Exception {
        discovery.discover(resources.resolve(targetLangLocation));
    }

    public int run() throws IOException {
        if(tmpSPTLocation == null) {
            throw new IllegalStateException(
                "SPT has not been registered, call registerSPT() before calling run()");
        }
        final SunshineMainDriver driver = services.getService(SunshineMainDriver.class);
        final int result = driver.run();
        tmpSPTLocation.delete();
        return result;
    }
}
