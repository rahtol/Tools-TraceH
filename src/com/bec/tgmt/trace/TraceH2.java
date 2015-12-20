package com.bec.tgmt.trace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TraceH2 {
	
	static TraceLineIdentification lineId = new TraceLineIdentification();
	static Map<String, Integer> lines = new TreeMap<String, Integer>();
	static Map<String, Integer> numbers = new TreeMap<String, Integer>();

	public static void main(String[] args)
	{
		System.out.println("TraceH2 v1.00, 15.11.2015");
		
		if (args.length < 1) {
			System.err.println ("usage: TraceH2 <OSA-Log-File-Wildcard> ...\n");
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
			System.err.println("Exception: " + e.getMessage());
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
			
			if(lineId.lineNoTotal % 100000 == 0){
				System.err.print(lineId.lineNoTotal+"\n");
			}
			
			String line = zeile;
			
			// extract variable parts (numbers) and replace them by format specifiers
			line = extractHexdump (line);
			line = extractHexFigure1 (line);
			line = extractHexFigure2 (line);
			line = extractHexFigure3 (line);
			line = extractDecimalFigure (line);

			Integer count = lines.get(line);
			if (count != null)
			{
				count++;
				lines.put (line, count);
			}
			else
			{
				lines.put (line, 1);
			}
		}
		
		logDataBr.close();
		if (zf != null) zf.close();

	}  // of processLog
	
	static String extractHexdump (String lineIn)
	{
		// hexadecimal dump of length at least 2 values at the end of the line, each values has two figures, values separated by one blank
		// case of hex digits a-f not significant, i.e a-f of A-F
		// trailing blanks at end of line not significant
		final Pattern p1 = Pattern.compile("(.*?)(([0-9a-fA-F][0-9a-fA-F][ ])+[0-9a-fA-F][0-9a-fA-F])(\\s*)");
		String lineOut = lineIn;
		
		Matcher m1 = p1.matcher(lineIn);
		if (m1.matches())
		{
			lineOut = m1.group(1) + "%%DUMP%";  // trailing blanks in m1.group(4) are not significant
			
			String hexdump = m1.group(2);
			countData ("%DUMP", (hexdump.length()+1)/3);
		}

		return lineOut;
	}
	
	static String extractHexFigure1 (String lineIn)
	{
		// special treatment for hex figures in WCU_ATP traces like "2E" in "I_SEP 2E:CoR::oStat: A:1 U:79"
//		String lineOut = lineIn.replaceAll("[ ][0-9A-F][0-9A-F][:]", "%%H%");
//		return lineOut;
		Pattern p = Pattern.compile("[ ][0-9A-F][0-9A-F][:]");
		Matcher m = p.matcher(lineIn);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String number = m.group().substring(1, 3);
			int h = Integer.parseInt(number, 16);
			countData("%H:1", 1);
		    m.appendReplacement(sb, "%%H%");
		}
		m.appendTail(sb);
		String lineOut2 = sb.toString();
		return lineOut2;
	}
	
	static String extractHexFigure2 (String lineIn)
	{
		// variable length hex figures introduced by "0x"
//		String lineOut = lineIn.replaceAll("0x[0-9a-fA-F]+", "%%HH%");
//		return lineOut;
		Pattern p = Pattern.compile("0x[0-9a-fA-F]+");
		Matcher m = p.matcher(lineIn);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String number = m.group().substring(2).toLowerCase();
			int h = Integer.parseUnsignedInt(number, 16);
			if (h<128){
				countData("%HH:1", 1);
			}
			else if (h<32768){
				countData("%HH:2", 1);
			}
			else{
				countData("%HH:4", 1);
			}
		    m.appendReplacement(sb, "%%HH%");
		}
		m.appendTail(sb);
		String lineOut2 = sb.toString();
		return lineOut2;
	}
	
	static String extractHexFigure3 (String lineIn)
	{
		// special treatment for hex figures with trailing "h" as in "Dispatcher: time: 238824329, type: 206h, s: 60"
//		String lineOut = lineIn.replaceAll("[ ][0-9A-F][0-9A-F][0-9A-F]h", "%%HHH%");
//		return lineOut;
		Pattern p = Pattern.compile("[ ][0-9A-F][0-9A-F][0-9A-F]h");
		Matcher m = p.matcher(lineIn);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String number = m.group().substring(1, 4);
			int h = Integer.parseInt(number, 16);
			if (h<128){
				countData("%HHH:1", 1);
			}
			else if (h<32768){
				countData("%HHH:2", 1);
			}
			else{
				countData("%HHH:4", 1);
			}
		    m.appendReplacement(sb, "%%HHH%");
		}
		m.appendTail(sb);
		String lineOut2 = sb.toString();
		return lineOut2;
	}
	
	static String extractDecimalFigure (String lineIn)
	{
//		String lineOut = lineIn.replaceAll("[0-9]+", "%%d%");
//		return lineOut;
		Pattern p = Pattern.compile("[0-9]+");
		Matcher m = p.matcher(lineIn);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String number = m.group();
			int i = Integer.parseInt(number);
			if (i<128){
				countData("%d:1", 1);
			}
			else if (i<32768){
				countData("%d:2", 1);
			}
			else{
				countData("%d:4", 1);
			}
		    m.appendReplacement(sb, "%%d%");
		}
		m.appendTail(sb);
		String lineOut2 = sb.toString();
		return lineOut2;
	}
	
	static void writeFormatStrings()
	{
		FileWriter outf;
		try {
			outf = new FileWriter("TraceH2.out");
			
			Set<Map.Entry<String,Integer>> l = lines.entrySet(); 
			Iterator<Map.Entry<String,Integer>> it = l.iterator();
			Map.Entry<String,Integer> e;
			while (it.hasNext()) {
				e = it.next();
				String s=e.getKey();
				int count = e.getValue();
				outf.write(String.format("%06d %03d \"%s\"\n", count, s.length(), s));
			}
			
	       	outf.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static double calculateEntropy()
	{
		int maxcount = 0;
		int sumcount = 0;

		Set<Map.Entry<String,Integer>> l = lines.entrySet(); 
		Iterator<Map.Entry<String,Integer>> it = l.iterator();
		Map.Entry<String,Integer> e;
		while (it.hasNext())
		{
			e = it.next();
			int count = e.getValue();
			
			if (count > maxcount) maxcount = count;
			sumcount += count;
		}
		
		double totalcount = (double) sumcount;
		double H = 0.0;
		it = l.iterator();
		while (it.hasNext())
		{
			e = it.next();
			int count = e.getValue();
			
			double p = ((double) count) / totalcount;
			double Ip = -Math.log(p);
			H += p * Ip;
		}
		
		H = H / Math.log(2);
		
		return H;
	}
	
	static void countData (String format, int noBytes)
	{
		Integer count = numbers.get(format);
		if (count != null)
		{
			count += noBytes;
			numbers.put (format, count);
		}
		else
		{
			numbers.put (format, noBytes);
		}
	}
	
	static void evaluate()
	{
		System.out.println("lineNoTotal=" + lineId.lineNoTotal);
		System.out.println("lines.size()=" + lines.size());
		
		writeFormatStrings();

		// text encoding without numbers
		double H = calculateEntropy();
		System.out.println("H = " + H + " Bit/Symbol");
		System.out.println("bits = " + H*lineId.lineNoTotal);
		System.out.println("bytes=" + (H*lineId.lineNoTotal)/8.0);
		
		Set<Map.Entry<String,Integer>> n = numbers.entrySet(); 
		Iterator<Map.Entry<String,Integer>> it = n.iterator();
		Map.Entry<String,Integer> e;
		while (it.hasNext())
		{
			e = it.next();
			String s = e.getKey();
			int count = e.getValue();
			System.out.println(String.format("%s: %d", s, count));
		}
	}
}
