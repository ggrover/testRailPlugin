package com.graphitesoftware.test.testrail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.rmn.testrail.entity.TestCase;



public class CustomSection extends com.rmn.testrail.entity.Section {
	
	private HashMap<String, CustomTestCase> parsedTestCases;
	private int projectId;
	
	public CustomSection() {
		super();
		parsedTestCases = new HashMap<String, CustomTestCase>();
	}
	
	public void addTestCase(CustomTestCase testCase) {
		if ( testCase != null ) {
			parsedTestCases.put(testCase.getTitle(), testCase);
		}
	}

	public boolean hasTestCase( String title) {
		return parsedTestCases.containsKey(title);
	}
	
	public CustomTestCase getTestCase( String title) {
		return parsedTestCases.get(title);
	}
	
	public Integer countTestCases() {
		return parsedTestCases.size();
	}
	
    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }
	
    public Integer getProjectId() {
        return projectId;
    }

    @Override
    public List<TestCase> getTestCases() {
        return new ArrayList();
    }
    public Collection<CustomTestCase> getParsedTestCases() {
    	return parsedTestCases.values();
    }
}
