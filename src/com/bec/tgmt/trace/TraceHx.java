package com.bec.tgmt.trace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TraceHx {
	
	static TraceLineIdentification lineId = new TraceLineIdentification();
	static NavigableMap<String, Integer> lines = new TreeMap<String, Integer>();

	public static void main(String[] args)
	{
		System.out.println("TraceHx v1.00, 15.11.2015");
		
		if (args.length < 1) {
			System.err.println ("usage: TraceH <OSA-Log-File-Wildcard> ...\n");
			System.exit(-1);
		};
		

		try {
			for (int i=0; i < args.length; i++) {
				File argi0 = new File (args[i]);
				String s0 = argi0.getAbsolutePath();
	 			File argi = new File (s0);
				String wildcard = argi.getName();
				File logfiledir = argi.getParentFile();
				JFnPatternMatcher patternmatcher = new JFnPatternMatcher (wildcard);
				File logfiles[] = logfiledir.listFiles(patternmatcher);
				for (final File logfile : logfiles) {
					processLog(logfile);
				}
			}
		} catch (Exception e) {
			System.err.println("Excption: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}

		evaluate ();
		
       	System.out.println("ready.\n");
	}

	static void processLog (final File logDataFile) throws Exception
	{
		System.out.println("Processing: \"" + logDataFile.getAbsolutePath() + "\"");
		
		ZipFile zf = null;
		BufferedReader logDataBr;
		
		if (logDataFile.getName().endsWith(".zip"))
		{
			zf = new ZipFile(logDataFile);
			int len = logDataFile.getName().length();
			String s0 = logDataFile.getName();
			String s1 = s0.substring(0, len-4); 
			String s2 = s1 + ".txt";
			InputStream in = zf.getInputStream(new ZipEntry(s2));
			InputStreamReader inr = new InputStreamReader (in);
			logDataBr = new BufferedReader(inr);
		}
		else
		{
			FileReader logDataFr = new FileReader (logDataFile);
			logDataBr = new BufferedReader(logDataFr);
		}
		
		String zeile = "";
		lineId.nextFile (logDataFile);
		
		while ((zeile = logDataBr.readLine()) != null)
		{
			lineId.parseLine(zeile, lineId);

			// TODO
		}
		
		logDataBr.close();
		if (zf != null) zf.close();

	}  // of processLog
	
	static void evaluate()
	{
		System.out.println("lineNoTotal=" + lineId.lineNoTotal);
		
		// TODO
	}
}
