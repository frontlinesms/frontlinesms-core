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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.messaging.MessageFormatter;

/**
 * Utilities for reading and writing Comma Seperated Value (CSV) files.
 * 
 * These methods follow RFC 4180.  This can be read at http://tools.ietf.org/html/rfc4180.
 * 
 * @author Alex
 */
public class CsvUtils {
	
//> SUBSTITUTION MARKERS
	public static final String MARKER_SENDER_NUMBER = MessageFormatter.MARKER_SENDER_NUMBER;
	public static final String MARKER_SENDER_NAME = MessageFormatter.MARKER_SENDER_NAME;
	public static final String MARKER_RECIPIENT_NUMBER = MessageFormatter.MARKER_RECIPIENT_NUMBER;
	public static final String MARKER_RECIPIENT_NAME = MessageFormatter.MARKER_RECIPIENT_NAME;
	public static final String MARKER_MESSAGE_CONTENT = MessageFormatter.MARKER_MESSAGE_CONTENT;
	public static final String MARKER_KEYWORD_KEY = MessageFormatter.MARKER_KEYWORD_KEY;
	public static final String MARKER_COMMAND_RESPONSE = MessageFormatter.MARKER_COMMAND_RESPONSE;
	
	/** [Substitution marker] {@link FrontlineMessage#getDate()} */
	public static final String MARKER_MESSAGE_DATE = "${message_date}";
	/** [Substitution marker] {@link Contact#getName()} */
	public static final String MARKER_CONTACT_NAME = "${contact_name}";
	/** [Substitution marker] {@link Contact#getPhoneNumber()} */
	public static final String MARKER_CONTACT_PHONE = "${contact_phone}";
	/** [Substitution marker] {@link Contact#getOtherPhoneNumber()} */
	public static final String MARKER_CONTACT_OTHER_PHONE = "${contact_other_phone}";
	/** [Substitution marker] {@link Contact#getEmailAddress()} */
	public static final String MARKER_CONTACT_EMAIL = "${contact_email}";
	/** [Substitution marker] {@link Contact#isActive()} */
	public static final String MARKER_CONTACT_STATUS = "${contact_status}";
	/** [Substitution marker] {@link Contact#getNotes()} */
	public static final String MARKER_CONTACT_NOTES = "${contact_notes}";
	/** [Substitution marker] {@link Contact#getGroups()} converted to a {@link String} TODO define how it's converted to a string */
	public static final String MARKER_CONTACT_GROUPS = "${contact_groups}";
	/** [Substitution marker] {@link FrontlineMessage#getType()} */
	public static final String MARKER_MESSAGE_TYPE = "${message_type}";
	/** [Substitution marker] {@link FrontlineMessage#getStatus()} */
	public static final String MARKER_MESSAGE_STATUS = "${message_status}";
	/** [Substitution marker] {@link Keyword#getDescription()} */
	public static final String MARKER_KEYWORD_DESCRIPTION = "${keyword_description}";
	
//> STATIC CONSTANTS
	/** Character encoding String for UTF-8 */
	static final String ENCODING_UTF8 = "UTF-8";
	
	/** Value read from an {@link InputStream} when the end has been reached. */
	private static final int END = -1;
	/** Carriage return character. */
	private static final char CR = '\r';
	/** The newline character. */
	private static final char LF = '\n';
	/** The '"' (inverted comma) character. */
	private static final char QUOTE = '"';
	/** The {@link #QUOTE} character as a {@link String} */
	private static final String QUOTE_STRING = "" + QUOTE;
	/** A csv-escaped {@link #QUOTE} character */
	private static final String DOUBLE_QUOTE_STRING = QUOTE_STRING + QUOTE_STRING;
	/** The comma character, used for separating items in a line of CSV */
	private static final char SEPARATOR = ',';
	/** Line terminator used at the end of each line of a CSV file */
	private static final String LINE_TERMINATOR = "\r\n";
	/** Array containing all special characters that cause a CSV value to require escaping */
	@SuppressWarnings("unused")
	private static final char[] RESTRICTED_CHARS = {CR, LF, QUOTE, SEPARATOR};

	/**
	 * Writes a line of CSV, substituting markers for replacements where this is requested.
	 * @param out The {@link Writer} to write the line of CSV to.
	 * @param rowFormat The line format to be output to CSV.
	 * @param markersAndReplacements List of markers and their replacements.  Each marker should be followed directly by its replacement in this list.
	 * @throws IOException 
	 */
	public static void writeLine(Writer out, CsvRowFormat rowFormat, String... markersAndReplacements) throws IOException {
		if((markersAndReplacements.length&1) == 1) throw new IllegalArgumentException("Each marker must have a replacement!  Odd number of markers+replacements provided: " + markersAndReplacements.length);

		for(Iterator<String> format = rowFormat.format(markersAndReplacements).iterator(); format.hasNext(); ) {
			out.write(format.next());
			if(format.hasNext()) {
				out.write(SEPARATOR);
			}
		}
		
		out.write(LINE_TERMINATOR);
	}
	
	/**
	 * Write a simple line of CSV
	 * @param out The {@link Writer} to write the line of CSV to.
	 * @param values The array of strings representing the values to write
	 * @throws IOException
	 */
	public static void writeLine(Writer out, String[] values) throws IOException {
		for(int i = 0 ; i < values.length ; ++i) {
			out.write(values[i]);
			if ((i + 1) < values.length) {
				out.write(SEPARATOR);
			}
		}
		
		out.write(LINE_TERMINATOR);
	}
	
	/**
	 * Escapes a String for use as a CSV cell value.
	 * @param value <code>null</code> values will be treated as empty strings
	 * @return intput {@link String} escaped for outputting as a single value of a CSV file
	 */
	static String escapeValue(String value) {
		// Could check if this value requires escaping.  This would save space
		// in the generated file, but would complicate the code.  It's allowed
		// in the spec to just include QUOTES around everything, so let's do
		// that.
		String escapedValue = value == null ? "" : value.replaceAll(QUOTE_STRING, DOUBLE_QUOTE_STRING);
		return QUOTE + escapedValue + QUOTE;
	}

	/**
	 * Read a line of CSV from the supplied reader and split the line into an array
	 * of unescaped values.
	 * 
	 * Reads following RFC 4180, http://tools.ietf.org/html/rfc4180
	 * @param reader
	 * @return Array containing list of values on this line, or <code>null</code> if the end of the file was reached before reading any characters
	 * @throws IOException
	 * @throws CsvParseException 
	 */
	public static String[] readLine(Reader reader) throws IOException, CsvParseException {
		ArrayList<String> readStrings = new ArrayList<String>();
		int read;
		StringBuilder lastValue = new StringBuilder();
		boolean insideQuotes = false;
		read = reader.read();
		// if there was NOTHING here, just return null.
		if(read == END) return null;
		while(read != END) {
			if(insideQuotes) {
				// If we're inside quotes, we need to carry on building our response
				// until we have ended the quotes again!
				if(read == QUOTE) {
					// We've read a quote character.  This is either the end of the escaped section,
					// or it is an escaped quote character, or it is the end of the line.  Any other
					// value is unexpected.
					read = reader.read();
					if(read == QUOTE) {
						// We've found two quotes in a row, which is actually read as a single, escaped, quote character.
						lastValue.append(QUOTE);
						// We're still inside our quoted region, so: on to the next character!
						read = reader.read();
					} else if(read == SEPARATOR || read == CR || read == LF || read == END) {
						// We've finished this value, so we should continue to the next one
						insideQuotes = false;
						readStrings.add(lastValue.toString());
						lastValue.delete(0, Integer.MAX_VALUE);
						read = reader.read();
					} else {
						// Can anything else appear here?  Should be the end of this value!
						throw new CsvParseException("We've reached the end of the quoted section, but apparently not the end of the value.  Only sensible option here is the end of the line...");
					}
				} else {
					// This is just a regular character in this value.  Add it 
					// to the builder, and carry on parsing.
					lastValue.append((char)read);
					read = reader.read();
				}
			} else {
				// We're not inside quotes.  At this stage, action characters are:
				//   - single quote - when this occurs, we are now inside quotes
				//   - end of line - if there is a line termination.  In ISO spec, this is \r\n,
				//     and this is what we search for.
				if(read == QUOTE) {
					// We're starting a quoted value, it seems.  There should be nothing else
					// read from the reader for this value.  If there was anything read, and
					// it wasn't whitespace, then there is something funny going on.
					if(lastValue.length() > 0)
						throw new CsvParseException("Unexpected characters before quote in value: '" + lastValue.toString() + "'");
					insideQuotes = true;
				} else if(read == SEPARATOR) {
					// We've finished this value, so we should continue to the next one
					readStrings.add(lastValue.toString());
					lastValue.delete(0, Integer.MAX_VALUE);
				} else if(read == CR || read == LF) {
					// In case we are seeing combinations like CRLF, or only CR or LF alone, accept all and ignore empty lines

					if(lastValue.length() > 0) {
						// we've finished the line, with a new value!
						readStrings.add(lastValue.toString());
					} if(readStrings.size() > 0) {
						return readStrings.toArray(new String[readStrings.size()]);
					}
				} else {
					lastValue.append((char)read);
				}
				read = reader.read();
			}
		}
		// This is the end of the reader.  Add the current value we are building,
		// and return the string array.
		if(lastValue.length() > 0) {
			readStrings.add(lastValue.toString());
		}
		
		if(readStrings.size() == 0) {
			// We have reached the end of the file, so return null here
			return null;
		} else {
			return readStrings.toArray(new String[readStrings.size()]);
		}
	}
	
	/**
	 * Closes a {@link Closeable}, e.g. an {@link OutputStream} or {@link Writer}.
	 * @param closeable the object to close
	 * @see Closeable#close()
	 */
	static void close(Closeable closeable) {
		if(closeable != null) { try { closeable.close(); } catch(IOException ex) { /* do nothing as nothing can be done */ } }
	}
}