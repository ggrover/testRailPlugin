package com.trendmicro.test.testrail;


import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utils class
 * 
 *
 */
public class Env {

	public static String FILE_NAME = "env";
	public static String FILE_SUFFIX = ".default";
	static String PROJECT_NAME_PROPERTY = "project.name";
	static String TESTSUITE_NAME_PROPERTY = "testsuite.name";
	//static String TEST_FILE_DIRECTORY = "C:\\TEMP";
	//static String TEST_FILE_PATTERN = ".*TEST-.*\\.xml";
	static String TESTRESULTS_FILE_DIRECTORY = "testresults.file.directory";
	static String TESTRESULTS_FILE_PATTERN = "testresults.file.pattern";
	public static String CLIENT_ID = "testrail.client.id";
	public static String USER_NAME = "testrail.username";
	public static String PASSWORD = "testrail.password";
	static String TEST_ANNOTATIONS_USED = "project.testCase.annotations.used";
	static String TEST_FILTER_AUTOMATED = "project.testCase.filter.automated";
	static String TEST_FILTER_DEFAULT = "project.testCase.filter.default";
	static String TEST_ASSIGNEDTO = "project.testCase.assignedto";
	static String TEST_INCLUDEFAIL = "project.testCase.includefail";
	static String TESTRESULTS_USEMETHOD = "testresults.usemethod";
	static String TESTRESULTS_URL_RESULTS = "testresults.url.results";
	static String TESTRESULTS_URL_BUILD = "testresults.url.build";
	static String TESTRESULTS_URL_REGRESSION = "testresults.url.regression";

	static Properties properties;
	
	
	//load system properties set using -D option
	private static void overrideWithSystemProperties(Properties properties) {

		for (Object key : properties.keySet()) {
			String value = System.getProperty(key.toString());
			if ( value !=null && value.length()>0 ) {
				properties.put(key, value);
			}
		}
		
	}
	//load the default property file, return null if not found
	//override with local property file
	//finally, load any corresponding system property values
	private static Properties loadProperties(String fileName) {
		Properties properties = null;
		try {
			InputStream inputStream = Env.class.getClassLoader().getResourceAsStream(fileName);
			if (inputStream != null ) {
				properties = new Properties();
				properties.load(inputStream);
			} else {
				System.out.println("In the loadProperties section, Failed to find properties file:" + fileName);
			}
		} catch (IOException ioe) {
			Logger.getGlobal().log(Level.INFO, "Failed to load properties file:" + fileName, ioe);
		}
		
		return properties;
	}
	
	public static Properties getProperties() {
		//load some properties
		if (properties == null ) {
			properties = loadProperties(FILE_NAME + FILE_SUFFIX);
			if ( properties == null ) {
				System.out.println("Failed to find properties file:" + FILE_NAME + FILE_SUFFIX);
	
			} else {
				//load local properties file if it exists
				try {
					String localHostName = InetAddress.getLocalHost().getHostName();
					Properties localProperties = loadProperties(FILE_NAME + "." + localHostName );
					if ( localProperties != null ) {
							properties.putAll(localProperties);
					}
				} catch (UnknownHostException uhe) {
					System.out.println( "Setup error:" +  uhe.getLocalizedMessage());
				}
				//set any properties passed via commandline
				overrideWithSystemProperties(properties);
			}
			System.out.println( "Properties:" + properties);
			Enumeration e = properties.keys();
			while (e.hasMoreElements()) {
				String key = (String)e.nextElement();
				System.out.println( key + " : " + properties.getProperty(key));
			}
		}
		return properties;
	}
	
    public static CustomTestRailService getTestRailService() {

		return new CustomTestRailService(
				properties.getProperty(CLIENT_ID),
				properties.getProperty(USER_NAME),
				properties.getProperty(PASSWORD));
    }
}

