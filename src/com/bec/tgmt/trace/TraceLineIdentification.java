package com.bec.tgmt.trace;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TraceLineIdentification implements TraceLineParser {
	
	final static Pattern p1 = Pattern.compile("Input[:]\\sCycle\\s0x([0-9a-f]*)\\s.*@(\\d*)[:].*");
	final static Pattern p2 = Pattern.compile("I_BAS\\s..[:]Time of day[:]\\s(\\d*).*");
	
	/**
	 * the file currently parsed
	 */
	public File file;
	
	/**
	 * line number in file
	 */
	public int lineNo;
	
	/**
	 * overall line number
	 */
	public int lineNoTotal;
	
	/** 
	 * current time in log file, resolution is [ms]; 
	 * windows tickcount at start of current PCB cycle; 
	 * extracted from input-cycle traceline
	*/
	public int tickcount;
	
	/**
	 * ...
	 */
	public int timeofday;

	public TraceLineIdentification()
	{
		this.lineNoTotal = 0;
		this.file = null;
		this.lineNo = 0;
		this.tickcount = 0;
		this.timeofday = 0;
	}
	
	public TraceLineIdentification(TraceLineIdentification lineId) {
		this.lineNoTotal = lineId.lineNoTotal;
		this.file = lineId.file;
		this.lineNo = lineId.lineNo;
		this.tickcount = lineId.tickcount;
		this.timeofday = lineId.timeofday;
	}

	public void nextFile (final File file)
	{
		this.file = file;
		lineNo = 0;
	}
	
	@Override
	public void parseLine(String line, TraceLineIdentification lineId)
	{
		lineNoTotal++;
		lineNo++;
		
		// check for "Input: Cycle ..."
		Matcher m1 = p1.matcher(line);
		if (m1.matches()) {
			int t = Integer.parseInt(m1.group(2));
			tickcount = t;
		}
		
		Matcher m2 = p2.matcher(line);
		if (m2.matches()) {
			int t = Integer.parseInt(m2.group(1));
			timeofday = t;
		}
		
	}

	public String pr(int lvl)
	{
		String indent = "                ".substring(0, 2*lvl);
		
		return
			String.format("%s<lineid t=\"%d\" fn=\"%s\" lineno=\"%d\"/>\n", indent, this.tickcount, this.file.getName(), this.lineNo);
	}
	
	public String timeOfDay()
	{
		return LocalDateTime.of(1985, 1, 1, 0, 0, 0).plusSeconds(timeofday).format(DateTimeFormatter.ISO_DATE_TIME);
	}
}
