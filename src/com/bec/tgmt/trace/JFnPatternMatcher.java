package com.bec.tgmt.trace;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JFnPatternMatcher implements FilenameFilter {

	@SuppressWarnings("unused")
	private final String patternstring;
	private final Pattern pattern;
	
	JFnPatternMatcher (String patternstring)
	{
		this.patternstring = patternstring;
		this.pattern = Pattern.compile(patternstring);
	}
	
	@Override
	public boolean accept(File arg0, String arg1) {
		Matcher m = pattern.matcher(arg1);
		boolean rc = m.matches();
		return rc;
	}

}
