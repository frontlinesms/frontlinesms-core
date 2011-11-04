/*
 * FrontlineSMS <http://www.frontlinesms.com>
 * Copyright 2007, 2008 kiwanja
 * 
 * This file is part of FrontlineSMS.
 * 
 * FrontlineSMS is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * FrontlineSMS is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FrontlineSMS. If not, see <http://www.gnu.org/licenses/>.
 */
package net.frontlinesms.csv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.frontlinesms.FrontlineUtils;

import org.apache.log4j.Logger;

/**
 * This file contains methods for importing data to the FrontlineSMS service
 * from CSV files.
 * FIXME display a meaningful message if import fails!
 * @author Carlos Eduardo Genz <kadu@masabi.com>
 * @author Alex Anderson <alex@frontlinesms.com>
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public abstract class CsvImporter {
	/** Logging object */
	protected Logger log = FrontlineUtils.getLogger(this.getClass());
	/** The first line of values as loaded from disk */
	private String[] rawFirstLine;
	/** Raw values as loaded from disk */
	private List<String[]> rawValues;
	
	protected CsvImporter(File importFile) throws CsvParseException {
		this.loadValuesFromCsvFile(importFile);
	}

	/**
	 * Import contacts from a CSV file.
	 * @param filename the file to import from
	 * @param rowFormat 
	 * @throws IOException If there was a problem accessing the file
	 * @throws CsvParseException If there was a problem with the format of the file
	 */
	private void loadValuesFromCsvFile(File importFile) throws CsvParseException {
		log.trace("ENTER");
		List<String[]> valuesList = new ArrayList<String[]>();
		
		if(log.isDebugEnabled()) log.debug("File [" + importFile.getAbsolutePath() + "]");
		Utf8FileReader reader = null;
		try {
			reader = new Utf8FileReader(importFile);
			boolean firstLine = true;
			String[] lineValues;
			while((lineValues = CsvUtils.readLine(reader)) != null) {
				if(firstLine) {
					firstLine = false;
					this.rawFirstLine = lineValues;
				} else {
					valuesList.add(lineValues);
				}
			}
		} catch(IOException ex) {
			throw new CsvParseException(ex);
		} finally {
			if (reader != null) reader.close();
		}
		
		log.trace("EXIT");
		this.rawValues = valuesList;
	}
	
	public List<String[]> getRawValues() {
		return this.rawValues;
	}
	
	public String[] getRawFirstLine() {
		return this.rawFirstLine;
	}
}
