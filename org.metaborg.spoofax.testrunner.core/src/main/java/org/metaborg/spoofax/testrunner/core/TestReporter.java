package org.metaborg.spoofax.testrunner.core;

import java.util.Collection;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.metaborg.spt.listener.ITestReporter;

public class TestReporter implements ITestReporter {
    private static final Logger logger = LogManager.getLogger(TestReporter.class);


    @Override public void addTestcase(String testsuiteFile, String description) throws Exception {
        logger.debug("Adding test case: " + description + "- " + testsuiteFile);
    }

    @Override public void reset() throws Exception {
        logger.debug("Resetting");
    }

    @Override public void addTestsuite(String name, String filename) throws Exception {
        logger.debug("Adding test suite: " + name + "-" + filename);
    }

    @Override public void startTestcase(String testsuiteFile, String description) throws Exception {
        logger.debug("Starting test case: " + description + "- " + testsuiteFile);
    }

    @Override public void finishTestcase(String testsuiteFile, String description, boolean succeeded,
        Collection<String> messages) throws Exception {
        final Level level = succeeded ? Level.INFO : Level.ERROR;
        final String prefix = succeeded ? "Success" : "Failure";
        final boolean hasMessages = !messages.isEmpty();
        final String postfix = hasMessages ? ", messages:" : "";

        logger.log(level, prefix + ": " + description + "- " + testsuiteFile + postfix);
        if(hasMessages) {
            for(String message : messages) {
                logger.log(level, "  " + message);
            }
        }
    }
}
