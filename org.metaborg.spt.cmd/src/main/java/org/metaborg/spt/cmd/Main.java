package org.metaborg.spt.cmd;

import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spt.core.SPTModule;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.core.testing.ITestReporterService;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.inject.Injector;
import jakarta.annotation.Nullable;

public class Main {
    private static final ILogger logger = LoggerUtils.logger(Main.class);


    public static void main(String[] args) {
        final Arguments arguments = new Arguments();
        final JCommander jc = new JCommander(arguments);

        try {
            jc.parse(args);
        } catch(ParameterException e) {
            logger.error("Could not parse parameters", e);
            jc.usage();
            System.exit(1);
        }

        if(arguments.help) {
            jc.usage();
            System.exit(0);
        }

        if(arguments.exit) {
            logger.info("Exiting immediately for testing purposes");
            System.exit(0);
        }

        Class<? extends ITestReporterService> customReporterClass = getClassByName("test reporter", arguments.customReporter);
        final Module module = new Module(customReporterClass);
        try(final Spoofax spoofax = new Spoofax(module, new SPTModule())) {

            final Injector injector = spoofax.injector;

            final Runner runner = injector.getInstance(Runner.class);

            final boolean successFull = runner.run(arguments.sptLocation, arguments.lutLocation, arguments.targetLanguageLocation,
                arguments.testsLocation, arguments.startSymbol);

            System.exit(successFull ? 0 : 1);

        } catch(Exception e) {
            logger.error("Error while running tests", e);
            System.exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> Class<? extends T> getClassByName(String subject, @Nullable String className) {
        if (className == null)
            return null;

        try {
            return (Class<? extends T>)Class.forName(className);
        } catch (ClassNotFoundException e) {
            logger.error("Class for " + subject + " not found: " + className, e);
            // Fallback to default.
            return null;
        }
    }
}
