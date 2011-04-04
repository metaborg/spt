package org.strategoxt.imp.testing.model;

public class TestcaseRun {
	private String description;
	private int offset;
	private TestsuiteRun testsuite;
	private boolean finished = false;
	private boolean succeeded = false;
	private long start = 0, end = 0 ;
	
	public TestcaseRun(String description, TestsuiteRun testsuite, int offset) {
		this.description = description;
		this.offset = offset;
		this.testsuite = testsuite;
	}

	public String getDescription() {
		return description;
	}

	public TestsuiteRun getParent() {
		return testsuite;
	}
	
	public long getDuration() {
		if(start == 0 || end == 0 ) {
			return -1;
		} else {
			return end - start;
		}
	}
	
	public int getOffset() {
		return offset;
	}
	
	public boolean isFinished() {
		return finished;
	}
	
	public boolean hasSucceeded() {
		return succeeded;
	}
	
	public void start() {
		start = System.currentTimeMillis();
	}
	
	public void finished(boolean succeeded) {
		end = System.currentTimeMillis();
		this.finished = true;
		this.succeeded = succeeded;
	}
}
