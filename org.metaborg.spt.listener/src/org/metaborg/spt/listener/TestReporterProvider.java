package org.metaborg.spt.listener;

import java.util.Iterator;
import java.util.ServiceLoader;

public class TestReporterProvider {

	private static TestReporterProvider instance;
	private ServiceLoader<ITestReporter> loader;
	
	private TestReporterProvider() {
		loader = ServiceLoader.load(ITestReporter.class, ITestReporter.class.getClassLoader());
	}
	
	public static synchronized TestReporterProvider getInstance() {
		if (instance == null) {
			instance = new TestReporterProvider();
		}
		return instance;
	}
	
	public Iterator<ITestReporter> getReporters() {
		return loader.iterator();
	}
}
