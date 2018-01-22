package com.graphitesoftware.test.testrail;


import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.rmn.testrail.entity.Project;
import com.rmn.testrail.entity.TestSuite;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;

/**
 * This is a custom doclet class that parses javadoc annotations of  JUnit test cases 
 * and updates a pre-existing TestRail TestSuite. 
 * 
 * @author garney.adams
 *
 */
public class TestSuiteDoclet {


	private static Properties properties = null;
	
	private static String DESCRIPTION_TAG = "@description";
	private static String REFS_TAG = "@refs";
	private static String PRECONDITIONS_TAG = "@preconds";
	private static String EXPECTED_TAG = "@expected";
	private static String STEPS_TAG = "@steps";
	private static String PRIORITY_TAG = "@priority";
	private static String TYPE_TAG = "@type";
	private static String ASSIGNEDTO_TAG = "@assignedto";
	private static boolean testAnnotationsUsed;
	private static boolean testFilter;
	private static boolean testFilterAutomated;
	private static boolean testFilterDefault;
	
	private static String DEFAULT_PRECONDITIONS = "None";
	//private static String DEFAULT_REFS = "1";
	private static Integer DEFAULT_TYPE = 1;
	private static Integer DEFAULT_PRIORITY = 3;
	
	public static void init() {
		//load some properties
		if (properties == null ) {
			properties = Env.getProperties();
		}
		testAnnotationsUsed = true;
		String testAnnotationsUsedProperty = properties.getProperty(Env.TEST_ANNOTATIONS_USED);
		if ( testAnnotationsUsedProperty != null ) {
			testAnnotationsUsed = Boolean.getBoolean(testAnnotationsUsedProperty);
		}

		testFilterDefault = false;
		String testFilterDefaultProperty = properties.getProperty(Env.TEST_FILTER_DEFAULT);
		if ( testFilterDefaultProperty != null ) {
			testFilterDefault = Boolean.getBoolean(testFilterDefaultProperty);
		}

		testFilter = false;
		testFilterAutomated = false;
		String testFilterAutomatedProperty = properties.getProperty(Env.TEST_FILTER_AUTOMATED);
		if ( testFilterAutomatedProperty != null ) {
			testFilterAutomated = Boolean.parseBoolean(testFilterAutomatedProperty);
			testFilter = true;
		}
	}
	
	public static boolean start(RootDoc root) throws InterruptedException {
		System.out.println("Starting TestSuite doclet" );
		 
		init();
		TestSuiteManager testSuiteManager = parseTestSuite(root.classes());

		System.out.println( "TestSuite, no of test cases:" + testSuiteManager.countTestCases());
			 
		testSuiteManager.synchronizeTestSuite();
			 
		return true;
	}
    

	
	public static TestSuiteManager parseTestSuite(ClassDoc[] classes) {
		TestSuiteManager testSuiteManager = new TestSuiteManager(
												properties.getProperty(Env.PROJECT_NAME_PROPERTY),
												properties.getProperty(Env.TESTSUITE_NAME_PROPERTY),
												properties.getProperty(Env.CLIENT_ID),
												properties.getProperty(Env.USER_NAME),
												properties.getProperty(Env.PASSWORD));
		
		for( ClassDoc classDoc : classes ) {
			System.out.println( "Class name:" + classDoc.name());
			CustomSection section = new com.graphitesoftware.test.testrail.CustomSection();
			section.setName(classDoc.qualifiedName());
			
			for ( MethodDoc methodDoc : classDoc.methods() ) {
				if (isTestMethod( methodDoc)) {
					// Check for filtering: Either automated (type=1) or otherwise
					String description = getTagValue( DESCRIPTION_TAG, methodDoc);
					if ( description != null ) {
						if(testFilter) {
							if ( testFilterDefault ) {
					            System.out.println( " Default add:" );
								section.addTestCase(parseTestCase(methodDoc));
							} else {
						        String typeId = getTagValue( TYPE_TAG, methodDoc);
						        //System.out.println("Checking type:" + typeId);
						        if ( typeId != null && typeId.length() > 0 ) {
						            Integer value = Integer.valueOf(typeId);
						            if (testFilterAutomated && ( value == 1 )) { section.addTestCase(parseTestCase(methodDoc)); }
						            if ((testFilterAutomated == false) && ( value != 1 )) { section.addTestCase(parseTestCase(methodDoc)); }
						        }
						    }
						} else {
							section.addTestCase(parseTestCase(methodDoc));
						}
					}
				}
			}
			if ( section.countTestCases()>0)
			    testSuiteManager.addSection(section);
		}
		return testSuiteManager;
	}
	/**
	 * Creates a test case from annotations within method comments.
	 * Testcase fields are populated as follows
	 * Section Name: Class name
	 * TestCase Title : method name
	 * TestCase Description DECRIPTION_TAG
	 * TestCase References  :REFS_TAG
	 * TestCase Type : TYPE_TAG
	 * TestCase Priority : PRIORITY_TAG , must be in rage 1-5
	 * TestCase Steps : PRECONDITIONS_TAG
	 * TestCase Steps : STEPS_TAG
	 * TestCase Expected EXPECTED_TAG
	 * 
	 * @param methodDoc
	 * @return
	 */
	public static CustomTestCase parseTestCase( MethodDoc methodDoc) {
		System.out.println( "..fq method name:" + methodDoc.qualifiedName());
		CustomTestCase testCase = new CustomTestCase();
		testCase.setTitle(methodDoc.name());
		
		String refs = getTagValue( REFS_TAG, methodDoc);
		System.out.println("parseTestCase refs:" + refs);
		if ( refs != null && refs.length() > 0 ) 
			testCase.setRefs(refs);
		
		String description = getTagValue( DESCRIPTION_TAG, methodDoc);
		System.out.println("parseTestCase description:" + description);
		if ( description != null && description.length() > 0 )
			testCase.setCustomOurDescription(description);
		
		String preconds = getTagValue( PRECONDITIONS_TAG, methodDoc);
		System.out.println("parseTestCase preconds:" + preconds);
		if ( preconds != null && preconds.length() > 0 ) {
			testCase.setCustomPreconds(preconds);
		} else {
        	System.out.println("parseTestCase default preconds:" + DEFAULT_PRECONDITIONS);
			testCase.setCustomPreconds(DEFAULT_PRECONDITIONS);		
		}
		
		String steps = getTagValue( STEPS_TAG, methodDoc);
		System.out.println("parseTestCase steps:" + steps);
		if ( steps != null && steps.length() > 0 )
			testCase.setCustomSteps(steps);
		
		String expected = getTagValue( EXPECTED_TAG, methodDoc);
		System.out.println("parseTestCase expected:" + expected);
		if ( expected != null && expected.length() > 0 )
			testCase.setCustomExpected(expected);
		
        String typeId = getTagValue( TYPE_TAG, methodDoc);
        System.out.println("parseTestCase type:" + typeId);
        if ( typeId != null && typeId.length() > 0 ) {
            Integer value = Integer.valueOf(typeId);
            //only save if a valid value
            if ( value >= 1 && value <= 6 ) {
                testCase.setTypeId(value);
            }
		} else {
        	System.out.println("parseTestCase default type:" + DEFAULT_TYPE);
			testCase.setTypeId(DEFAULT_TYPE);		
		}
        
        /*
         * So many ways for this to go wrong, in terms of users getting it wrong.
         * The original "get" for assigned_to uses the email string.  
        String typeId = getTagValue( ASSIGNEDTO_TAG, methodDoc);
        System.out.println("parseTestCase assignedto:" + typeId);
        if ( typeId != null && typeId.length() > 0 ){
            Integer value = Integer.valueOf(typeId);
            if ( value >= 1 && value <= 6 ) {
                testCase.setTypeId(value);
            }
        }
        */
        
		String priorityId = getTagValue( PRIORITY_TAG, methodDoc);
		System.out.println("parseTestCase priorityId:" + priorityId);
		if ( priorityId != null && priorityId.length() > 0 ){
			Integer value = Integer.valueOf(priorityId);
			//only save if a valid value
			if ( value >= 1 && value <= 5 ) {
			    testCase.setPriorityId(value);
			}
		} else {
        	System.out.println("parseTestCase default priorityId:" + DEFAULT_PRIORITY);
			testCase.setPriorityId(DEFAULT_PRIORITY);		
		}
		System.out.println("parseTestCase done " );
		return testCase;
		
	}
	
	private static boolean isTestMethod( MethodDoc methodDoc) {
		System.out.println("...isTestMethod: " + methodDoc.name() );
		boolean result = false;
		if (testAnnotationsUsed) {
			AnnotationDesc[] annotations = methodDoc.annotations();
			for ( AnnotationDesc annotation : annotations ) {
				if (annotation.annotationType().name().equalsIgnoreCase("test")) {
					result = true;
					break;
				}
			}
		} else {
			if (methodDoc.name().startsWith("test")) {
				result = true;
			}
		}
		return result;
	}
	
	public static String getTagValue(String tagName, MethodDoc methodDoc) {
		
		String tagValue = null;
		AnnotationDesc[] annotations = methodDoc.annotations();

		if (testAnnotationsUsed) {
			for ( AnnotationDesc annotation : annotations ) {
				String name = annotation.annotationType().name();
				System.out.println( methodDoc.name() + " Annotation: " + name);
				if (name.equals("Test")) {
					//System.out.println( "Comments:" + methodDoc.commentText());
					//System.out.println( "Comments raw:" + methodDoc.getRawCommentText());
					Tag[] tags = methodDoc.tags();
					for ( Tag tag : tags) {
						if ( tag.name().equalsIgnoreCase(tagName)) {
						 System.out.println( "Tag name:" + tag.name() + " text:" + tag.text());
						 tagValue = tag.text();
						}
					}
				}
			}
		} else {
			Tag[] tags = methodDoc.tags();
			for ( Tag tag : tags) {
				if ( tag.name().equalsIgnoreCase(tagName)) {
				 System.out.println( "Tag name:" + tag.name() + " text:" + tag.text());
				 tagValue = tag.text();
				}
			}
		}
		return tagValue;
	}
	
}

