package com.graphitesoftware.test.junit;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.HashMap;
import java.util.List;


@XmlRootElement(name="testsuite")
public class JunitTestSuite {
	
    private String name;
    private int failures;
    private int errors;
    private int skipped;
    private int tests;
    private List<JunitTestCase> cases;
    private HashMap<String, JunitTestCase> testCaseMap;

    @XmlAttribute
    public void setName(String name) { this.name = name; }
    @XmlAttribute
    public void setFailures(int failures) { this.failures = failures; }
    @XmlAttribute
    public void setErrors(int errors) { this.errors = errors; }
    @XmlAttribute
    public void setSkipped(int skipped) { this.skipped = skipped; }
    @XmlAttribute
    public void setTests(int tests) { this.tests = tests; }
    @XmlElement(name = "testcase")
    public void setCases(List<JunitTestCase> cases) { this.cases = cases; }

    public String getName() { return this.name; }
    public List<JunitTestCase> getCases() { return this.cases; }
    public int getFailures() { return this.failures; }
    public int getErrors() { return this.errors; }
    public int getSkipped() { return this.skipped; }
    public int getTests() { return this.tests; }
    
    public JunitTestCase getTestCase(String name) {
    	if ( testCaseMap == null ) {
    		testCaseMap = new HashMap<String, JunitTestCase>();
    		for ( JunitTestCase testCase : cases ) {
    			testCaseMap.put(testCase.getName(), testCase);
    		}
    	}
    	return testCaseMap.get(name);
    }
}