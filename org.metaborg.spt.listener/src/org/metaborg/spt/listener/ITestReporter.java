package org.metaborg.spt.listener;

import java.util.Collection;

/**
 * An interface for use with Java Services.
 * 
 * A TestReporter is notified of test cases and test execution
 * and is expected to report this somehow (e.g. generate a jUnit report).
 * 
 * @author Volker Lanting
 *
 */
public interface ITestReporter {

	/**
	 * Notify this reporter of the existence of a test case.
	 * @param testsuiteFile the file name of the test suite to which the test case belongs
	 * @param description the description (i.e. name) of the test case
	 */
	public void addTestcase(String testsuiteFile, String description) throws Exception;
	
	/**
	 * Reset the reporter, so it can be used again for a new run of tests.
	 */
	public void reset() throws Exception;
	
	/**
	 * Notify this reporter of the existence of a test suite.
	 * @param name the name of the test suite
	 * @param filename the file name of the test suite
	 */
	public void addTestsuite(String name, String filename) throws Exception;
	
	/**
	 * Notify this reporter that execution of a test case has started
	 * @param testsuiteFile the file name of the test suite to which the started test case belongs
	 * @param description the description (i.e. name) of the test case that was started
	 */
	public void startTestcase(String testsuiteFile, String description) throws Exception;
	
	/**
	 * Notify this reporter that a test case execution has finished.
	 * @param testsuiteFile the file name of the test suite to which the finished test case belongs
	 * @param description the description (i.e. name) of the test case
	 * @param succeeded true iff the test case succeeded
	 * @param messages an optional list of errors, warnings or notes generated during test execution
	 */
	public void finishTestcase(String testsuiteFile, String description,
			boolean succeeded, Collection<String> messages) throws Exception;
}
