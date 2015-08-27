package org.metaborg.spoofax.testrunner.cmd;

import java.util.Collection;

import org.metaborg.spt.listener.ITestReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleTestReporter implements ITestReporter {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleTestReporter.class);

    public boolean failed = false;
    

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
    	if(!succeeded) {
    		failed = true;
    	}
    	
        final String prefix = succeeded ? "Success" : "Failure";
        final boolean hasMessages = !messages.isEmpty();
        final String postfix = hasMessages ? ", messages:" : "";

        logger.info(prefix + ": " + description + "- " + testsuiteFile + postfix);
        if(hasMessages) {
            for(String message : messages) {
                logger.info("  " + message);
            }
        }
    }
}
