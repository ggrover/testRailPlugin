package com.graphitesoftware.test.testrail;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

public class TestResultsFilter implements FilenameFilter {

	Pattern pattern ;
	public TestResultsFilter(String filterPattern) {
		pattern = Pattern.compile( filterPattern );
	}
	@Override
	public boolean accept(File dir, String name) {
		// TODO Auto-generated method stub
		
		return pattern.matcher(name).matches();
	}

}
