/**
 * 
 */
package org.strategoxt.imp.testing.listener;

/**
 * @author vladvergu
 * 
 */
public interface ITestListener {

	static final String EXTENSION_ID = "org.strategoxt.imp.testing.testlistener";

	void reset() throws Exception;

	void addTestcase(String testsuite, String description, int offset) throws Exception;

	void addTestsuite(String name, String filename) throws Exception;

	void startTestcase(String testsuite, String description) throws Exception;

	void finishTestcase(String testsuite, String description, boolean succeeded) throws Exception;

	void disableRefresh() throws Exception;

	void enableRefresh() throws Exception;

}
