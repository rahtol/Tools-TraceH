package com.bec.tgmt.trace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TraceH {
	
	static NavigableMap<String, Integer> words = new TreeMap<String, Integer>();

	public static void main(String[] args)
	{
		System.out.println("TraceH v1.00, 13.11.2015");
		
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
		
		while ((zeile = logDataBr.readLine()) != null)
		{
			String sa[] = zeile.split("\\s");
			
			for (String s : sa)
			{
				if (s.length() == 0){
					continue;
				}
				Integer count = words.get(s);
				if (count != null)
				{
					count++;
					words.put (s, count);
				}
				else
				{
					words.put (s, 1);
				}
			};
		}
		
		logDataBr.close();
		if (zf != null) zf.close();

	}  // of processLog
	
	static void evaluate()
	{
		int size = words.size();
		int maxcount = 0;
		int sumcount = 0;
	
		Set<Map.Entry<String,Integer>> w = words.entrySet(); 
		Iterator<Map.Entry<String,Integer>> it = w.iterator();
		Map.Entry<String,Integer> e;
//		Iterator<Integer> it = words.values().iterator();
		while (it.hasNext())
		{
			e = it.next();
			String s=e.getKey();
			int count = e.getValue();
			
			if (count > maxcount) maxcount = count;
			sumcount += count;
			
			if (count > 10000)
			{
				System.out.println("\"" + s + "\": " + count);
				
			}
		}
		
		System.out.println("size=" + size);
		System.out.println("maxcount=" + maxcount);
		System.out.println("sumcount=" + sumcount);

		double totalcount = (double) sumcount;
		double H = 0.0;
		int symbolcount = 0;
		it = w.iterator();
		while (it.hasNext())
		{
			e = it.next();
//			String s=e.getKey();
			int count = e.getValue();
			symbolcount += count;
			
			double p = ((double) count) / totalcount;
			double Ip = -Math.log(p);
			H += p * Ip;
		}
		
		H = H / Math.log(2);
		
		System.out.println("H = " + H + " Bit/Symbol");
		System.out.println("n = " + symbolcount  + "   (Anzahl Symbole)");
		System.out.println("bits = " + H*sumcount);
		System.out.println("bytes=" + (H*sumcount)/8.0);
		
	}
}
