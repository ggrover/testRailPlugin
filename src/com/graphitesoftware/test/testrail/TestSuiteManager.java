package com.graphitesoftware.test.testrail;

import java.util.HashMap;
import java.util.List;

import com.rmn.testrail.entity.Project;
import com.rmn.testrail.entity.Section;
import com.rmn.testrail.entity.TestCase;
import com.rmn.testrail.entity.TestSuite;
import com.rmn.testrail.service.TestRailService;

/**
 * This class is used to manage TestRail TestSuites
 * from custom testcase annotations of JUnit Testsuites.
 * 
 * Pre-requisites
 *  - Assumes the following have been setup in TestRail
 *  - test rail project identifier
 *  - testsuite identifier
 *  - a valid set of TestRail credentials
 * Input
 *  - 
 * 
 * @author garney.adams
 *
 */
public class TestSuiteManager extends TestSuite {

	private CustomTestRailService testRailService;
	private HashMap<String, CustomSection> parsedSections;
	private String projectName;
	private String testSuiteName;
	private int projectId;
	private int testSuiteId;

	
	
	//retrieve TestSuite from TestRail
	//get list of Testcases: classnames (do we want sections e.g each class is  a section?)
	//for each class
	//  
	//  parse javadoc
	//  if (new testcase) {
	//     add TestCase to  test suite
	//  } else if (updated test case) {
	//     update test caes in test suite
	//  }
	// for all testcases in testrail and not in source code, mark for delete??
	//  delete
	
	public TestSuiteManager(String projectName, String testSuiteName, String clientId, String userName, String password) {
		
		this.projectName = projectName;
		this.testSuiteName = testSuiteName;
		parsedSections = new HashMap<String, CustomSection>();
		testRailService = new CustomTestRailService(clientId, userName, password );

	}
	
	public void addSection(CustomSection section) {
		if ( section != null ) {
			parsedSections.put(section.getName(), section);
		}
	}

	public boolean hasSection( String name) {
		return parsedSections.containsKey(name);
	}
	
	
	public Integer countTestCases() {
		int result = 0;
		for ( CustomSection section : parsedSections.values()) {
			result += section.countTestCases();
		}
		return result;
	}

	public Integer getProjectId() {
		return projectId;
	}

	public void setProjectId(int projectId) {
		this.projectId = projectId;
	}

	public String getTestSuiteName() {
		return testSuiteName;
	}

	public void setTestSuiteName(String testSuiteName) {
		this.testSuiteName = testSuiteName;
	}

	public Integer getTestSuiteId() {
		return testSuiteId;
	}
	
	public TestRailService getTestRailService() {
		return testRailService;
	}

	public boolean synchronizeTestSuite() throws InterruptedException {
		HashMap<String, Section> deletedSections = new HashMap<String, Section>();
		HashMap<String, CustomSection> updateSections = new HashMap<String, CustomSection>();
		HashMap<String, CustomSection> newSections = new HashMap<String, CustomSection>();
		HashMap<String, HashMap<String, TestCase>> dbSectionTestCases = new HashMap<String, HashMap<String, TestCase>> ();
		
		//find the project
		Project project = testRailService.getProjectByName(projectName);
		TestSuite dbTestSuite = project.getTestSuiteByName(testSuiteName);

        System.out.println("Project:" + project.getName() + " ,# testsuites:" + project.getTestSuites().size());
        if ( dbTestSuite == null ) {
        	System.out.println("TestSuite:" + testSuiteName + " does not exist, you need to create the test suite in TestRail.");
        	System.exit(1);
        }
        System.out.println("TestSuite:" + dbTestSuite.getName());

        System.out.println("Getting lists of deleted and existing sections");
        for( Section section: dbTestSuite.getSections() ) {
        	if ( parsedSections.containsKey(section.getName())) {
        		updateSections.put(section.getName(), parsedSections.get(section.getName()));
        		HashMap<String, TestCase> dbTestCases = new HashMap<String, TestCase>();
        		for ( TestCase testCase : section.getTestCases()) {
        			dbTestCases.put(testCase.getTitle(), testCase);
        		}
        		dbSectionTestCases.put(section.getName(), dbTestCases);
        	} else {
        		deletedSections.put(section.getName(), section);
        	}
        }
        
        System.out.println("get the list of new sections");
       for ( CustomSection section : this.parsedSections.values()) {
        	if ( !updateSections.containsKey(section.getName()) ) {
        		newSections.put(section.getName(), section);
        		for (CustomTestCase testCase : section.getParsedTestCases()) {
        			testCase.setNew(true);
        		}
        	}
        }
        
        System.out.println("TestSuite: run, number of deleted sections" + deletedSections.size());
        System.out.println("TestSuite: run, number of updated sections" + updateSections.size());
        System.out.println("TestSuite: run, number of new sections" + newSections.size());

        
        //add new sections
        int sectionCount = dbTestSuite.getSections().size();
        for (CustomSection section : newSections.values() ) {
        	section.setSuiteId(dbTestSuite.getId());
        	section.setProjectId(project.getId());
        	section.setTestRailService(testRailService);
        	this.testRailService.addSection(project.getId(), section);
        	Thread.sleep(2000);
        }
        
        //fetch the updated sections and save the section to the parsed testsuite
        List<Section> dbSections = project.getTestSuiteByName(testSuiteName).getSections();
        for (Section section : dbSections ) {
        	System.out.println( "DB Sections, name:" + section.getName() + ", id:" + section.getId());
        	if ( newSections.containsKey(section.getName()) ) {
        		newSections.get(section.getName()).setId(section.getId());
        	}
        	if ( updateSections.containsKey(section.getName()) ) {
        		updateSections.get(section.getName()).setId(section.getId());
        	}
        }
        //For each new section add all new testcases
        for (CustomSection section : newSections.values()) {
        	synchronizeTestCases(project, dbTestSuite, section);
        }
        //for each updated section, set flag on new tests 
        for (CustomSection section : updateSections.values()) {
        	//for each section flag any new test cases
            for ( CustomTestCase testCase : section.getParsedTestCases()) {
            	TestCase dbTestCase = dbSectionTestCases.get(section.getName()).get(testCase.getTitle());
            	if ( dbTestCase == null) {
            		testCase.setNew(true);
            	} else {
    				testCase.setSectionId(dbTestCase.getSectionId());
    				testCase.setSuiteId(dbTestCase.getSuiteId());
    				testCase.setId(dbTestCase.getId());
            	}
            }
        	synchronizeTestCases(project, dbTestSuite, section);
        }

        
		return true;
	}
	
	private void synchronizeTestCases(Project project, TestSuite testSuite, CustomSection section) throws InterruptedException {
		int id = 1;
		System.out.println("TestSuite: synchronizeTestCases, processing section:" + section.getName());
		for ( CustomTestCase testCase : section.getParsedTestCases() ) {
			System.out.println("TestSuite: synchronizeTestCases, processing testcase:" + testCase.getTitle());

			if (testCase.isNew())  {
				System.out.println("TestSuite: synchronizeTestCases, processing testcase: sectionId:" + section.getId() + ", testSuiteId:" + testSuite.getId());
				testCase.setSectionId(section.getId());
				testCase.setSuiteId(testSuite.getId());
				testCase.setId(id);
				this.testRailService.addTestCase(section.getId(), testCase);				
			} else {
				this.testRailService.updateTestCase(testCase.getId(), testCase);
			}
			id += 1;
			Thread.sleep(2000);
		}
		
	}
}
