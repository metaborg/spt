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
	
	void reset();

	void addTestcase(String testsuite, String description, int offset);

	void addTestsuite(String name, String filename);

	void startTestcase(String testsuite, String description);

	void finishTestcase(String testsuite, String description, boolean succeeded);
}
