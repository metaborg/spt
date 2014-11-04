package org.metaborg.spoofax.testrunner.cmd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.metaborg.spoofax.testrunner.core.TestRunner;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);


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

        try {
            final TestRunner runner = new TestRunner(arguments.testsLocation, "testrunnerfile");
            runner.registerSPT();
            runner.registerLanguage(arguments.targetLanguageLocation);

            final int exit = runner.run();
            if(exit == 0) {
                logger.info("Testing completed normally");
                System.exit(0);
            } else {
                logger.info("Testing completed with non-zero status " + exit);
                System.exit(1);
            }
        } catch(Exception e) {
            logger.error("Error while running tests", e);
            System.exit(1);
        }
    }
}
