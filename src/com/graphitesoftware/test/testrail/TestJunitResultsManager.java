package com.graphitesoftware.test.testrail;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.graphitesoftware.test.junit.JunitTestCase;
import com.graphitesoftware.test.junit.JunitTestSuite;
import com.rmn.testrail.entity.Project;
import com.rmn.testrail.entity.Section;
import com.rmn.testrail.entity.TestCase;
import com.rmn.testrail.entity.TestInstance;
import com.rmn.testrail.entity.TestResult;
import com.rmn.testrail.entity.TestRun;
import com.rmn.testrail.entity.TestRunCreator;
import com.rmn.testrail.entity.TestSuite;
import com.rmn.testrail.entity.User;


/** 
 * This class will parse Junit test result files and submit the results
 * to testRail as a TestRun.
 * 
 * @author garney.adams
 *
 */
public class TestJunitResultsManager {

	private Properties properties;
	private CustomTestRailService testRailService;
	private TestRun testRun;
	private HashMap<String, JunitTestSuite> sections;
	private HashMap<String, JunitTestCase> junitResults;
	private HashMap<Integer, TestCase> dbTestCases;
	private HashMap<Integer, Section> dbSections;
	
	private Project project;
	private TestSuite testSuite;
	
	public static void main(String[] args) throws InterruptedException {
		TestJunitResultsManager testResults = new TestJunitResultsManager();
		testResults.parse();
	}
	public TestJunitResultsManager() {

	}
	
	/**
	 * Parse files specified via property file. Post results to test rail.
	 * @throws InterruptedException 
	 * 
	 */
	private void parse() throws InterruptedException {
		// Read JUnit results,
		// Create a testsuite, with sections, and testcases.  It's OK to re-use testsuite name, but the testsuite should be cleared.
		// Create a testrun based on the testsuite, and populate with Junit results
		// All info is read from properties file
		properties = Env.getProperties();
		testRailService = Env.getTestRailService();	
		System.out.println( "TestRailService:" + testRailService.toString());
		
		//parse the JUnit test result files.
		//add to map, key is section name (class name)
		sections = new HashMap<String, JunitTestSuite>();
	
		TestResultsFilter filter = new TestResultsFilter( properties.getProperty(Env.TESTRESULTS_FILE_PATTERN));
		File directory = new File(properties.getProperty(Env.TESTRESULTS_FILE_DIRECTORY));
		for ( String fileName : directory.list() ) {
			if ( filter.accept(directory, fileName)) {
				System.out.println( "Parsing junit results," + directory.getAbsolutePath() + " , filename:" + fileName);
				System.out.println( "......filename matches:" + filter.accept(directory, fileName));
				parseResults( directory, fileName);
			}
		}
		
		//find the project
		System.out.println( "Fetching test rail data..."); 
		project = testRailService.getProjectByName(properties.getProperty(Env.PROJECT_NAME_PROPERTY));
		if ( project == null ) {
			System.out.println( "Failed to find project:"+ properties.getProperty(Env.PROJECT_NAME_PROPERTY) +", exiting..."); 
			System.exit(3);
		}
		testSuite = project.getTestSuiteByName(properties.getProperty(Env.TESTSUITE_NAME_PROPERTY));
		if ( testSuite == null ) {
			System.out.println( "Failed to find test suite:"+ properties.getProperty(Env.TESTSUITE_NAME_PROPERTY) +", exiting..."); 
			System.exit(4);
		}
		//junitResults = new HashMap<String, JunitTestCase>();
		// Add new entries to the Test Suite
		CustomSection newSection;
		Integer newSectionId;
        for (String testSectionName : sections.keySet() ) {
        	newSectionId = this.getSectionIdByName(testSectionName);
        	if (newSectionId == null) {
				newSection = new CustomSection();
				newSection.setProjectId(project.getId());
				newSection.setSuiteId(testSuite.getId());
				newSection.setName(testSectionName);
	        	System.out.println("....Add Section " + newSection.getName());
				newSection.setTestRailService(testRailService);
				this.testRailService.addSection(project.getId(), newSection);
	        	System.out.println("....Added Section " + newSection.getName() + ", " + newSectionId);
	        	newSectionId = this.getSectionIdByName(testSectionName);
	        	System.out.println("....Added Section " + newSection.getName() + ", " + newSectionId);
        	}
			if ( sections.get(testSectionName).getCases() != null ) {
				for( JunitTestCase testCase: sections.get(testSectionName).getCases()) {
			        if ( this.getTestCaseIdByName(testCase.getName()) == null) {
						//junitResults.put(testSectionName+"."+testCase.getName(), testCase);
						CustomTestCase tc = new CustomTestCase();
						tc.setTitle(testCase.getName());
						tc.setTypeId(1);
	    				tc.setSuiteId(testSuite.getId());
	    				tc.setTestRailService(testRailService);
			        	System.out.println("....Add TestCase to S(" + newSectionId + "):" + tc.getTitle() +", " + testSectionName);
	    				this.testRailService.addTestCase(newSectionId, tc);
	    				Thread.sleep(2000);
	    				//this.testRailService.addTestCase(sectionId, tc);
			        }
				}
			}
		}

		//flatten classname.methodname
		junitResults = new HashMap<String, JunitTestCase>();
		for (String testSectionName : sections.keySet() ) {
			System.out.println("Section name:" + testSectionName);
			if ( sections.get(testSectionName).getCases() != null ) {
			// if ( sections.get(testSectionName) != null ) {
				for( JunitTestCase testCase: sections.get(testSectionName).getCases()) {
					System.out.println("....Testcase:" + testSectionName+"."+testCase.getName());
					junitResults.put(testSectionName+"."+testCase.getName(), testCase);
    				Thread.sleep(2000);
				}
			} else {
				System.out.println("...error processing results, no section found (in testrail) for:" + testSectionName + " ... skipping.");
			}
		}

		dbSections = new HashMap<Integer, Section>();
		dbTestCases = new  HashMap<Integer, TestCase>();
        //flatten, map by sectionname,title (class name.methodname)
        for ( Section section : testSuite.getSections() ) {
        	for(TestCase testCase : section.getTestCases()) {
        		dbTestCases.put(testCase.getId(), testCase);
        	}
        	dbSections.put(section.getId(), section);
        }
		System.out.println("Adding sections to P" + project.getId() + " (" + project.getName() + "), S" + testSuite.getId() + " (" + testSuite.getName() + ").");


		//create a Testrun
		//add a TestResult for each test junit test case result

		TestRunCreator creator = new TestRunCreator();
		creator.setSuiteId(testSuite.getId());
		creator.setTestRailService(testRailService);
		if (properties.containsKey("testrun.name")) {
			creator.setName(properties.getProperty("testrun.name"));			
		}
		if (properties.containsKey("testrun.description")) {
			creator.setDescription(properties.getProperty("testrun.description"));			
		} else {
			creator.setDescription("Test Run:" + testSuite.getName() + " " + new Date());
		}
		testRun = testRailService.addTestRun(project.getId(), creator);
		System.out.println("Adding test results to testrail..." );
		System.out.println("Adding testrun ID: " + testRun.getId() );
		//the testRun created has a list of test instances (created from the TestSuite)
		//add a test result to each test instance
		System.out.println( "...adding " + testSuite.getSections().size() + " sections.");
		System.out.println( "...adding " + testSuite.getTestCases().size() + " test cases.");

		boolean assignedTo = false;
		Integer assignedToId = 0;
		if (properties.containsKey(Env.TEST_ASSIGNEDTO)) {
			String assignedToEmail = properties.getProperty(Env.TEST_ASSIGNEDTO);
			User testUser = testRailService.getUserByEmail(assignedToEmail);
			// Error handling is piss-poor.  
			if (testUser != null) {
				assignedTo = true;
				assignedToId = testUser.getId();
				System.out.println( "...adding " + assignedToEmail + " userID " + assignedToId + ".");
			}
		}
		boolean addErr = false;
		if (properties.containsKey(Env.TEST_INCLUDEFAIL)) {
			if (properties.getProperty(Env.TEST_INCLUDEFAIL).equalsIgnoreCase("true")) {addErr = true;}
		}
		
		
        for ( Section section : testSuite.getSections() ) {
        	for(TestCase testCase : section.getTestCases()) {
        		System.out.println(section.getId() + " " + testCase.getId() + " " + section.getName() + "." + testCase.getTitle());
        	}
        }
		
		for (TestInstance testInstance: testRun.getTests() ) {
			//System.out.println( ".Test instance:" + testInstance.getTitle());
			TestResult testResult = new TestResult();
			testResult.setTestId(testInstance.getId());
			TestCase testCase = dbTestCases.get(testInstance.getCaseId());
			Section section = dbSections.get(testCase.getSectionId());	
			
			//get the junit test for this test instance
			System.out.println( ".get test: "+ section.getName()+"."+testInstance.getTitle()+":"+testInstance.getStatusId());
			JunitTestCase junitTestResult = junitResults.get(section.getName()+"."+testInstance.getTitle());
			// System.out.println( "");	
			if ( junitTestResult != null ) {
			//if ( junitTestResult != null && junitTestResult.getSkipped() == null ) {
				// Skipped tests are not added to testRail
				boolean isErr = false;
				if ( (junitTestResult.getFailure() != null) || (junitTestResult.getError() != null)  ) {
					testResult.setStatusId(5);
					isErr = true;
					// Add failed results
					if (assignedTo) {testResult.setAssignedtoId(assignedToId);}
					System.out.println( "... test failed: " + section.getName()+"."+testInstance.getTitle());
					if ( junitTestResult.getFailure() != null) {
						if (addErr) {testResult.setComment(junitTestResult.getFailure().getMessage() + "\n" + junitTestResult.getFailure().getText());}
						if ((junitTestResult.getFailure().getMessage() != null) && (junitTestResult.getFailure().getText() != null)) {
							System.out.println( "...... :"+ junitTestResult.getFailure().getClass() + junitTestResult.getFailure().getType() + ": " + junitTestResult.getFailure().getMessage() + junitTestResult.getFailure().getText() );
						}	
					} else {
						if (addErr) {testResult.setComment(junitTestResult.getError().getMessage() + "\n" + junitTestResult.getError().getText());}
						if ((junitTestResult.getError().getMessage() != null) && (junitTestResult.getError().getText() != null)) {
							System.out.println( "...... :" + junitTestResult.getError().getClass() + junitTestResult.getError().getType() + ": " + junitTestResult.getError().getMessage() + junitTestResult.getError().getText() );
						}
					}
					// Print a log message with the refs 
					System.out.println( "... test failed");
				} else {
					testResult.setStatusId(1);
					System.out.println( "... test passed: "+ section.getName()+"."+testInstance.getTitle());
				}
				System.out.println( "... adding " + section.getName());
				testRailService.addTestResult(testInstance.getId(), testResult);
				Thread.sleep(2000);
				System.out.println( "... added.");
				//if (testCase.getRefs() != null && testCase.getRefs().compareTo("") != 0) {
				if (isErr) {
					System.out.println( "... test failed refs:"
						+ section.getName()+"."+testInstance.getTitle() + ":"
						+ testInstance.getId() + " " + testInstance.getRunId() + ":" 
						+ testCase.getRefs());
				}
				//}
			} else {
				System.out.println("Failed to find test result for " + section.getName()+"."+testInstance.getTitle());
			}
		}
		System.out.println( "Test run processing complete");
	}

	private Integer getSectionIdByName(String sectionName) {
        for ( Section section : this.testSuite.getSections() ) {
        	if (sectionName.equals(section.getName())) { return section.getId(); }
        }
        return null;
	}
	
	private Integer getTestCaseIdByName(String testCaseName) {
        for ( TestCase tc : this.testSuite.getTestCases() ) {
        	if (testCaseName.equals(tc.getTitle())) {return tc.getId();}
        }
		return null;		
	}
	
	private void parseResults(File directory, String fileName) {
		try {

			// create JAXB context and initializing Marshaller
			JAXBContext jaxbContext = JAXBContext.newInstance(JunitTestSuite.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			// specify the location and name of xml file to be read

			//File XMLfile = new File("C:\\temp\\TEST-com.securespaces.server.restapi.test.UpdateWorkAdminTests.xml");
			File xmlFile = new File(directory, fileName);
			// this will create Java object - country from the XML file
			JunitTestSuite testSuite = (JunitTestSuite) jaxbUnmarshaller.unmarshal(xmlFile);
			System.out.println("...TestSuite Name: " + testSuite.getName());
			System.out.println("......errors:" + testSuite.getErrors());
			System.out.println("......skipped:" + testSuite.getSkipped());
			System.out.println("......failures:" + testSuite.getFailures());
			sections.put(testSuite.getName(), testSuite);
			/*
			List<JunitTestCase> list = testSuite.getCases();
			
			int i=0;
			for (JunitTestCase testCase : list) {
				System.out.println("TestCase:" + testCase.getName() );
				JunitFailure failure = testCase.getFailure();
				if ( failure != null ) {
				    System.out.println("...failure msg:" + failure.getMessage());
				}
				
			}*/
		} catch (JAXBException e) {
			// some exception occured
			e.printStackTrace();
		}
	}
	
}
