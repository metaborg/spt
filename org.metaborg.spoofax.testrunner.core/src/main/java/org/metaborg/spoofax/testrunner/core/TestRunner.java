package org.metaborg.spoofax.testrunner.core;

import java.io.IOException;

import org.apache.commons.vfs2.FileObject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.metaborg.spoofax.core.language.ILanguageDiscoveryService;
import org.metaborg.sunshine.drivers.SunshineMainDriver;
import org.metaborg.sunshine.environment.ServiceRegistry;
import org.metaborg.sunshine.environment.SunshineMainArguments;

public class TestRunner {
    private final ServiceRegistry services;


    public TestRunner(FileObject testsLocation, String sptBuilder) {
        // Make logger STFU
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        LoggerConfig logger = ctx.getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        logger.setLevel(Level.ERROR);
        ctx.updateLoggers();

        final SunshineMainArguments params = new SunshineMainArguments();
        params.project = testsLocation.getName().getPath();
        params.filestobuildon = ".";
        params.noanalysis = true;
        params.builder = sptBuilder;
        org.metaborg.sunshine.drivers.Main.initEnvironment(params);

        this.services = ServiceRegistry.INSTANCE();
    }


    public void registerLanguages(FileObject sptLangLocation, FileObject targetLangLocation) throws Exception {
        final ILanguageDiscoveryService discovery = services.getService(ILanguageDiscoveryService.class);
        discovery.discover(sptLangLocation);
        discovery.discover(targetLangLocation);
    }

    public int run() throws IOException {
        final SunshineMainDriver driver = services.getService(SunshineMainDriver.class);
        return driver.run();
    }
}
