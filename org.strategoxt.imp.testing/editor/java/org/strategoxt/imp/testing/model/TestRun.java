package org.strategoxt.imp.testing.model;

import java.util.Collection;
import java.util.HashMap;

public class TestRun {
	private HashMap<String, TestsuiteRun> testsuites = new HashMap<String, TestsuiteRun>();

	public TestsuiteRun addTestsuite(String testsuite, String filename) {
		TestsuiteRun ts = new TestsuiteRun(this, testsuite, filename);
		testsuites.put(filename, ts) ;
		return ts;
	}
	
	public TestsuiteRun getTestsuite(String testsuite) {
		return testsuites.get(testsuite);
	}
	
	public Collection<TestsuiteRun> getTestSuites() {
		return testsuites.values();
	}

	public int getNrTests() {
		int res = 0;
		for(TestsuiteRun tr : getTestSuites()) {
			res += tr.getNrTests();
		}
		return res;
	}
	
}
