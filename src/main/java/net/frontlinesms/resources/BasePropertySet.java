/**
 * 
 */
package net.frontlinesms.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.frontlinesms.FrontlineUtils;

import org.apache.log4j.Logger;

/**
 * Base properties wrapper class.
 * @author Alex
 */
class BasePropertySet {
//> STATIC CONSTANTS
	/** Logging object for this instance. */
	public static final Logger LOG = FrontlineUtils.getLogger(BasePropertySet.class);

//> INSTANCE PROPERTIES
	/** Map from property key to value */
	private Map<String, String> properties;

//> CONSTRUCTORS

//> ACCESSORS
	/**
	 * @param propertyKey The key for the property to get
	 * @return value from {@link #properties}
	 */
	String getProperty(String propertyKey) {
		return this.properties.get(propertyKey);
	}
	
	Map<String, String> getProperties() {
		return properties;
	}
	
	/**
	 * Set the properties.  This should be done exactly once.
	 * @param properties value for {@link #properties}
	 */
	protected void setProperties(Map<String, String> properties) {
		assert(this.properties == null) : "Properties already set.  Cannot be changed.";
		assert(properties != null) : "Cannot set properties to null.";
		this.properties = properties;
	}

//> INSTANCE HELPER METHODS

//> STATIC FACTORIES

//> STATIC HELPER METHODS
	/**
	 * Load properties from an {@link InputStream}.
	 * @param inputStream The input stream to load the properties from.
	 * @return The loaded properties
	 * @throws IOException If there was a problem loading the properties from the supplied {@link InputStream}.
	 */
	static void load(Map<String, String> map, InputStream inputStream) throws IOException {
		if(inputStream == null) throw new NullPointerException("The supplied input stream was null.");
		
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
			
			String line;
			while((line = in.readLine()) != null) {
				add(map, line);
			}
			
			LOG.trace("EXIT");
		} finally {
			// Close all streams
			if(in != null) try { in.close(); } catch(IOException ex) {
				// nothing we can do except log the exception
				LOG.warn("Exception thrown while closing stream 'fis'.", ex);
			}
		}
	}
	
	static HashMap<String, String> load(InputStream inputStream) throws IOException {
		HashMap<String, String> map = new HashMap<String, String>();
		load(map, inputStream);
		return map;
	}
	
	static HashMap<String, String> load(Collection<String> lines) {
		HashMap<String, String> map = new HashMap<String, String>();
		for(String line: lines) add(map, line, true);
		return map;
	}
	
	static void add(Map<String, String> map, String line, boolean overwrite) {
		line = line.trim();				
		if(line.length() == 0 || line.charAt(0) == '#') {
			// This is a comment, so we should remember it to write back later
		} else {
			int splitChar =  line.indexOf('=');
			if(splitChar <= 0) {
				// there's no "key=value" pair on this line, but it does have text on it.  That's
				// not strictly legal, so we'll log a warning and carry on.
				LOG.warn("Bad line in properties file: '" + line + "'");
			} else {
				String key = line.substring(0, splitChar);
				boolean insert = true;
				if(map.containsKey(key)) {
					// This key has already been read from the language file.
					LOG.warn("Duplicate key in properties file: ''");
					if(!overwrite) {
						insert = false;
					}
				}
				if(insert) {
					String value = line.substring(splitChar + 1);
					map.put(key, value);
				}
			}
		}
	}
	
	static void add(Map<String, String> map, String line) {
		add(map, line, false);
	}
}
