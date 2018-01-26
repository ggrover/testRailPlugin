package com.trendmicro.test.testrail;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rmn.testrail.entity.BaseEntity;
import com.rmn.testrail.entity.Section;
import com.rmn.testrail.service.TestRailService;
import com.rmn.testrail.util.HTTPUtils;
import com.rmn.testrail.util.JSONUtils;

public class CustomTestRailService extends TestRailService {

    /**
     * This might not last forever--we'll need to make "v2" a variable at some point--but this works for the moment
     */
    private static final String ENDPOINT_SUFFIX = "index.php?/api/v2/%s%s";
    private Logger log = LoggerFactory.getLogger(getClass());
    
	private String ADD_TESTCASE_CMD = "add_case";
	private String ADD_SECTION_CMD = "add_section";
	private String UPDATE_TESTCASE_CMD = "update_case";
	private String apiEndpoint = "https://%s.testrail.com/";
    private String username;
    private String password;
    private HTTPUtils utils = new HTTPUtils();

    /**
     * Construct a new TestRailService with the necessary information to start communication immediately
     * @param clientId The clientID--usually the "<id>.testrail.com" you are assigned when you first open an account
     * @param username The username you will use to communicate with the API. It is recommended to create an account with minimal privileges, specifically for API use
     * @param password The password to use with this account
     */
    public CustomTestRailService(String clientId, String username, String password) {
    	super(clientId, username, password);
        this.apiEndpoint = String.format(apiEndpoint, clientId) + ENDPOINT_SUFFIX;
        this.username = username;
        this.password = password;
    }
	
	/**
     * Builds the proper TestRails request URL based on the type and number of parameters. It tries to be smart about how to add
     * parameters to calls that require 0, 1, or 2 arguments
     * @param apiCall The end-point you wish to request
     * @param urlParams The full parameters of the request you're making (it's up to you to make it correct)
     * @return The URL you've built
     */
    private String buildRequestURL(String apiCall, String urlParams) {
        //Some API calls take 2 parameters, like get_cases/16/1231, so we need to account for both
        String argString = "";
        if (!StringUtils.isEmpty(urlParams)) {
            argString = String.format("/%s", urlParams);
        }

        //Build the complete url
        return String.format(apiEndpoint, apiCall, argString);
    }

    /**
     * Posts the given String to the given TestRails end-point
     * @param apiCall The end-point that expects to receive the entities (e.g. "add_result")
     * @param urlParams The remainder of the URL required for the POST. It is up to you to get this part right
     * @param entity The BaseEntity object to use at the POST body
     * @return The Content of the HTTP Response
     */
    private HttpResponse postRESTBody(String apiCall, String urlParams, BaseEntity entity) {
    	System.out.println( "postRESTBody start:"  + apiCall + ", urlParams:" + urlParams);
        HttpClient httpClient = new DefaultHttpClient();
       String completeUrl = buildRequestURL( apiCall, urlParams );
        try {
            HttpPost request = new HttpPost( completeUrl );
            String authentication = utils.encodeAuthenticationBase64(username, password);
            request.addHeader("Authorization", "Basic " + authentication);
            request.addHeader("Content-Type", "application/json");
            System.out.println( "postRESTBody request uri:" + request.getURI().toString());
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
            System.out.println( "postRESTBody mapper initialized");
            String body = mapper.writeValueAsString(entity).replace("\n", "\\n");
            System.out.println( "postRESTBody, body: " + body);
            request.setEntity(new StringEntity(body));

            HttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() != 200) {
                Error error = JSONUtils.getMappedJsonObject(Error.class, utils.getContentsFromHttpResponse(response));
                log.error("Response code: {}", response.getStatusLine().getStatusCode());
                log.error("TestRails reported an error message: {}", error.getMessage());
            }
            return response;
        }
        catch (IOException e) {
            log.error(String.format("An IOException was thrown while trying to process a REST Request against URL: [%s]", completeUrl), e.toString());
            e.printStackTrace();
            throw new RuntimeException(String.format("Connection is null, check URL: %s", completeUrl));
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
    
    /**
     * Add a TestCase to a particular Section, given the Section id
     * @param sectionId The id of the Section to which you would like to add a TestCase entity
     * @param testCase TestCase you wish to add to this Section
     */
    public void addTestCase(int sectionId, CustomTestCase testCase) {
        HttpResponse response = postRESTBody(ADD_TESTCASE_CMD, Integer.toString(sectionId), testCase);
        
        if (response.getStatusLine().getStatusCode() != 200) {
        	System.out.println( "...Error adding test case:" + testCase.getTitle());
        	System.out.println( "..........response code: " + response.getStatusLine().getStatusCode());
        	System.out.println( "..........response reason: " + response.getStatusLine().getReasonPhrase());
            throw new RuntimeException(String.format("TestCase was not properly added to TestSuite [%d]: %s", sectionId, response.getStatusLine().getReasonPhrase()));
        }
    }
    
    /**
     * Add a Section to a particular TestSuite, given the Project id
     * @param projectId The id of the Project to which you would like to add a Section entity
     * @param section Section you wish to add to the TestSuite
     */
    public void addSection(int projectId, Section section) {
        HttpResponse response = postRESTBody(ADD_SECTION_CMD, Integer.toString(projectId), section);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException(String.format("Section was not properly added to project [%d]: %s", projectId, response.getStatusLine().getReasonPhrase()));
        }
    }
    
    /**
     * Update a TestCase given the testCase id
     * @param testCaseId The id of the Testcase to update
     * @param testCase TestCase you wish to add to this Section
     */
    public void updateTestCase(int testCaseId, CustomTestCase testCase) {
        HttpResponse response = postRESTBody(UPDATE_TESTCASE_CMD, Integer.toString(testCaseId), testCase);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException(String.format("TestCase was not properly added to TestSuite [%d]: %s", testCaseId, response.getStatusLine().getReasonPhrase()));
        }
    }
}
