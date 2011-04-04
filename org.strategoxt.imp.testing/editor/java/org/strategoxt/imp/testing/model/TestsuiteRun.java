package org.strategoxt.imp.testing.model;

import java.util.Collection;
import java.util.HashMap;

import org.strategoxt.imp.testing.SpoofaxTestingParseController;

public class TestsuiteRun {
	private String resource;
	private TestRun testrun;
	private HashMap<String,TestcaseRun> testcases = new HashMap<String, TestcaseRun>();
	
	public TestsuiteRun(TestRun testrun, String resource) {
		this.testrun = testrun;
		this.resource = resource;
	}

	public String getName() {
		return resource;
	}
	
	public Collection<TestcaseRun> getTestcases() {
		return testcases.values();
	}
	
	public TestcaseRun getTestcase(String testcase) {
		return testcases.get(testcase);
	}
	
	public TestRun getParent() {
		return testrun;
	}
	
	public TestcaseRun addTestCase(String description, int line) {
		TestcaseRun tcr = new TestcaseRun(description, this, line);
		testcases.put(description, tcr) ;
		return tcr;		
	}
	
	public int getNrTests() {
		return getTestcases().size();
	}
	
}
