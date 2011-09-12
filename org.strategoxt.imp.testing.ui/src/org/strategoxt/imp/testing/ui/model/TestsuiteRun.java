package org.strategoxt.imp.testing.ui.model;

import java.util.Collection;
import java.util.HashMap;

public class TestsuiteRun {
	private String name;
	private String resource;
	private TestRun testrun;
	private HashMap<String,TestcaseRun> testcases = new HashMap<String, TestcaseRun>();
	
	public TestsuiteRun(TestRun testrun, String name, String resource) {
		this.testrun = testrun;
		this.resource = resource;
		this.name = name;
	}

	public String getFilename() {
		return resource;
	}

	public String getName() {
		return name;
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

	public int getNrFailedTests() {
		int res = 0;
		for(TestcaseRun tcr : getTestcases()) {
			if(tcr.isFinished() && !tcr.hasSucceeded())
				res++;
		}
		return res;
	}

}
