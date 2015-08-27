package org.metaborg.spoofax.testrunner.cmd;

import org.metaborg.spoofax.testrunner.core.TestRunner;
import org.metaborg.spt.listener.TestReporterProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.Iterables;

public class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		final Arguments arguments = new Arguments();
		final JCommander jc = new JCommander(arguments);

		try {
			jc.parse(args);
		} catch (ParameterException e) {
			logger.error("Could not parse parameters", e);
			jc.usage();
			System.exit(1);
		}

		if (arguments.help) {
			jc.usage();
			System.exit(0);
		}

		try {
			final TestRunner runner = new TestRunner(arguments.testsLocation,
					"testrunnerfile");
			runner.registerSPT();
			runner.registerLanguage(arguments.targetLanguageLocation);
			runner.run();

			ConsoleTestReporter reporter = (ConsoleTestReporter) Iterables.get(
					TestReporterProvider.getInstance().getReportersIterable(),
					0);

			if (!reporter.failed) {
				logger.info("Testing completed normally");
				System.exit(0);
			} else {
				logger.info("Tests failed");
				System.exit(1);
			}
		} catch (Exception e) {
			logger.error("Error while running tests", e);
			System.exit(1);
		}
	}
}
