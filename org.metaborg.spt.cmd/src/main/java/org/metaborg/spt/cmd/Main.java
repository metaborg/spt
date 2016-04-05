package org.metaborg.spt.cmd;

import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.inject.Injector;

/**
 * An example
 *
 */
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
            logger.info("Exitting immediately for testing purposes");
            System.exit(0);
        }

        final Module module = new Module();
        try(final Spoofax spoofax = new Spoofax(module)) {

            final Injector injector = spoofax.injector;

            final Runner runner = injector.getInstance(Runner.class);

            runner.run(arguments.sptLocation, arguments.targetLanguageLocation, arguments.testsLocation);

            System.exit(0);

        } catch(Exception e) {
            logger.error("Error while running tests", e);
            System.exit(1);
        }
    }
}
