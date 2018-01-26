package com.trendmicro.test.testrail;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.trendmicro.test.junit.JunitTestCase;
import com.trendmicro.test.junit.JunitTestSuite;
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
 *
 */
public class TestResultsManager {

	private Properties properties;
	private CustomTestRailService testRailService;
	private TestRun testRun;
	private HashMap<String, JunitTestSuite> sections;
	private HashMap<String, JunitTestCase> junitResults;
	private HashMap<Integer, TestCase> dbTestCases;
	private HashMap<Integer, Section> dbSections;
	
	public static void main(String[] args) {
		TestResultsManager testResults = new TestResultsManager();
		testResults.parse();
	}
	public TestResultsManager() {

	}
	
	/**
	 * Parse files specified via property file. Post results to test rail.
	 * 
	 */
	public void parse() {
		properties = Env.getProperties();
		testRailService = Env.getTestRailService();	
		
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
		//flatten classname.methodname
		junitResults = new HashMap<String, JunitTestCase>();
		for (String testSectionName : sections.keySet() ) {
			System.out.println("Section name:" + testSectionName);
			if ( sections.get(testSectionName).getCases() != null ) {
			// if ( sections.get(testSectionName) != null ) {
				for( JunitTestCase testCase: sections.get(testSectionName).getCases()) {
					System.out.println("....Testcase:" + testSectionName+"."+testCase.getName());
					junitResults.put(testSectionName+"."+testCase.getName(), testCase);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else {
				System.out.println("...error processing results, no section found (in testrail) for:" + testSectionName + " ... skipping.");
			}
		}

		//find the project
		System.out.println( "Fetching test rail data..."); 
		Project project = testRailService.getProjectByName(properties.getProperty(Env.PROJECT_NAME_PROPERTY));
		if ( project == null ) {
			System.out.println( "Failed to find project:"+ properties.getProperty(Env.PROJECT_NAME_PROPERTY) +", exiting..."); 
			System.exit(3);
		}
		TestSuite dbTestSuite = project.getTestSuiteByName(properties.getProperty(Env.TESTSUITE_NAME_PROPERTY));
		if ( dbTestSuite == null ) {
			System.out.println( "Failed to find test suite:"+ properties.getProperty(Env.TESTSUITE_NAME_PROPERTY) +", exiting..."); 
			System.exit(4);
		}
		dbSections = new HashMap<Integer, Section>();
		dbTestCases = new  HashMap<Integer, TestCase>();
		
        System.out.println("Project:" + project.getName() + " ,# testsuites:" + project.getTestSuites().size());
        System.out.println("TestSuite:" + dbTestSuite.getName());
        //flatten, map by sectionname,title (class name.methodname)
        for ( Section section : dbTestSuite.getSections() ) {
        	for(TestCase testCase : section.getTestCases()) {
        		dbTestCases.put(testCase.getId(), testCase);
        	}
        	dbSections.put(section.getId(), section);
        }
		//create a Testrun
		//add a TestResult for each test junit test case result

        TestRunCreator creator = new TestRunCreator();
		creator.setSuiteId(dbTestSuite.getId());
		creator.setTestRailService(testRailService);
		if (properties.containsKey("testrun.name")) {
			creator.setName(properties.getProperty("testrun.name"));			
		}
		if (properties.containsKey("testrun.description")) {
			creator.setDescription(properties.getProperty("testrun.description"));			
		} else {
			creator.setDescription("Test Run:" + dbTestSuite.getName() + " " + new Date());
		}
		testRun = testRailService.addTestRun(project.getId(), creator);
		System.out.println("Adding test results to testrail..." );
		System.out.println("Adding testrun ID: " + testRun.getId() );
		//the testRun created has a list of test instances (created from the TestSuite)
		//add a test result to each test instance
		System.out.println( "...adding " + dbTestSuite.getSections().size() + " sections.");
		System.out.println( "...adding " + dbTestSuite.getTestCases().size() + " test cases.");
		try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
		}
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
		
		// A reminder that the Testrail section is $PACKAGENAME.$CLASSNAME
		// Within each section is a testcase $TESTNAME, which is a method
		// We could get fancy and refer to these generically later on
		for (TestInstance testInstance: testRun.getTests() ) {
			System.out.println( ".Test instance:" + testInstance.getTitle());
			TestResult testResult = new TestResult();
			testResult.setTestId(testInstance.getId());
			TestCase testCase = dbTestCases.get(testInstance.getCaseId());
			Section section = dbSections.get(testCase.getSectionId());	
			
			//get the junit test for this test instance
			System.out.println( ".get test: "+ section.getName()+"."+testInstance.getTitle());
			JunitTestCase junitTestResult = junitResults.get(section.getName()+"."+testInstance.getTitle());
			boolean isErr = false;
			boolean useMethod = false;
			if (properties.containsKey(Env.TESTRESULTS_USEMETHOD)) {
				if (properties.getProperty(Env.TESTRESULTS_USEMETHOD).equalsIgnoreCase("true")) {useMethod = true;}
			}
			if ( junitTestResult != null && junitTestResult.getSkipped() == null ) {
				// Skipped tests are not added to testRail
				if ( (junitTestResult.getFailure() != null) || (junitTestResult.getError() != null)  ) {
					testResult.setStatusId(5);
					isErr = true;
					// Add failed results
					String errorComment = "";
					if (assignedTo) {testResult.setAssignedtoId(assignedToId);}
					System.out.println( "... test failed: " + section.getName()+"."+testInstance.getTitle());
					if ( junitTestResult.getFailure() != null) {
						errorComment += junitTestResult.getFailure().getMessage() + "\n=========================\n";
						errorComment += junitTestResult.getFailure().getText()    + "\n=========================\n";
						if ((junitTestResult.getFailure().getMessage() != null) && (junitTestResult.getFailure().getText() != null)) {
							System.out.println( "...... :"+ junitTestResult.getFailure().getClass() + junitTestResult.getFailure().getType() + ": " + junitTestResult.getFailure().getMessage() + junitTestResult.getFailure().getText() );
						}	
					} else {
						errorComment += junitTestResult.getError().getMessage() + "\n=========================\n";
						errorComment += junitTestResult.getError().getText()    + "\n=========================\n";
						if ((junitTestResult.getError().getMessage() != null) && (junitTestResult.getError().getText() != null)) {
							System.out.println( "...... :" + junitTestResult.getError().getClass() + junitTestResult.getError().getType() + ": " + junitTestResult.getError().getMessage() + junitTestResult.getError().getText() );
						}
					}
					if (properties.containsKey(Env.TESTRESULTS_URL_RESULTS)) {
						errorComment += "Results dir: " + properties.getProperty(Env.TESTRESULTS_URL_RESULTS) + "/work/" + section.getName();
						if (useMethod) {
							errorComment += "%23" + testInstance.getTitle() + "\n";
						}
						errorComment += "\n";
					}
					if (properties.containsKey(Env.TESTRESULTS_URL_REGRESSION)) {
						// Refer to $PACKAGE/$CLASSNAME
						int index = section.getName().lastIndexOf(".");
						if (index != -1 && index != section.getName().length()) {
							errorComment += "Jenkins regression results: " + 
								properties.getProperty(Env.TESTRESULTS_URL_REGRESSION) + "/testReport/" +
								section.getName().substring(0, index) + "/" + 
								section.getName().substring(index+1) + "\n";
						}
					}
					if (properties.containsKey(Env.TESTRESULTS_URL_BUILD)) {
						errorComment += "Jenkins build: " + properties.getProperty(Env.TESTRESULTS_URL_BUILD) + "\n";
					}
					if (addErr) { testResult.setComment(errorComment); } 
				} else {
					testResult.setStatusId(1);
					System.out.println( "... test passed: "+ section.getName()+"."+testInstance.getTitle());
				}
				try {
		            ObjectMapper mapper = new ObjectMapper();
		            mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
		            String body = mapper.writeValueAsString(testResult);
		            System.out.println( "...about to add test result, testId:" + testInstance.getId() + ", post body:" + body);
				} catch (Exception ee) {
					System.out.println( "...exception printing test result, "+ ee.getMessage());
				}
				testRailService.addTestResult(testInstance.getId(), testResult);
				//if (testCase.getRefs() != null && testCase.getRefs().compareTo("") != 0) {
				if (isErr) {
					System.out.println( "... test failed refs:"
						+ section.getName()+"."+testInstance.getTitle() + ":"
						+ testInstance.getId() + " " + testInstance.getRunId() + ":" 
						+ testCase.getRefs());
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}			
		}
		System.out.println( "Test run processing complete");
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
			int total = testSuite.getErrors() + testSuite.getSkipped() + testSuite.getFailures();
			System.out.println("......total:" + testSuite.getTests());
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
