package com.bec.tgmt.trace;

public interface TraceLineParser {
	
	public void parseLine (String line, TraceLineIdentification lineId) throws Exception;

}
