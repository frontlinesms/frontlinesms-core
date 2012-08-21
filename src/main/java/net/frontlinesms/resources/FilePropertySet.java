/**
 * 
 */
package net.frontlinesms.resources;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * @author aga
 */
public class FilePropertySet extends BasePropertySet {
	/** The file the properties are loaded from and saved to. */
	private File file;
	
	/**
	 * Create a new instance of this class pointing to the supplied file.
	 * @param file
	 */
	protected FilePropertySet(File file) {
		this.file = file;
	}
	
//> INSTANCE METHODS
	/**
	 * Save this {@link UserHomeFilePropertySet} to disk.
	 * @return <code>true</code> if the properties file was successfully saved; <code>false</code> otherwise.
	 */
	public synchronized boolean saveToDisk() {
		LOG.trace("ENTER");
		File propFile = this.file;
		
		BufferedWriter out = null;
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(propFile);
			out = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
			
			for(String propertyKey : this.getProperties().keySet()) {
				out.write(propertyKey + "=" + this.getProperties().get(propertyKey) + "\n");
			}
			
			out.flush();
			
			LOG.trace("EXIT");
			return true;
		} catch(IOException ex) {
			LOG.debug("Exception thrown while saving properties file: " + propFile.getAbsolutePath(), ex);
			LOG.trace("EXIT");
			return false;
		} finally {
			if(fos != null) { try { fos.close(); } catch(IOException ex) {} }
			if(out != null) { try { out.close(); } catch(IOException ex) {} }
		}
	}
	
//> ACCESSOR METHODS
	/**
	 * Set a property in this property set.
	 * @param propertyName
	 * @param value
	 */
	protected synchronized void setProperty(String propertyName, String value) {
		this.getProperties().put(propertyName, value);
	}
	
	/**
	 * Gets the {@link String} value of a property.
	 * @param propertyName
	 * @return The value of the property as a {@link String} or <code>null</code> if it is not set.
	 */
	protected synchronized String getProperty(String propertyName) {
		return super.getProperty(propertyName);
	}
	
	/**
	 * Gets the <code>boolean</code> value of a property.
	 * @param propertyName the name of the property
	 * @param defaultValue the default value for the property, returned if the property is not set
	 * @return The value of the property or <code>defaultValue</code> if it is not set.
	 */
	protected boolean getPropertyAsBoolean(String propertyName, boolean defaultValue) {
		String value = getProperty(propertyName);
		if (value == null) return defaultValue;
		else return Boolean.parseBoolean(value);
	}
	
	/**
	 * Sets the {@link Boolean} value of a property.
	 * @param propertyName
	 * @param value
	 */
	protected void setPropertyAsBoolean(String propertyName, Boolean value) {
		if(value == null) setProperty(propertyName, null);
		else setProperty(propertyName, Boolean.toString(value));
	}
	
	/**
	 * Gets the <code>int</code> value of a property.
	 * @param propertyName the name of the property
	 * @param defaultValue the default value for the property
	 * @return The value of the property, or <code>defaultValue</code> if it is not set.
	 */
	protected int getPropertyAsInt(String propertyName, int defaultValue) {
		String value = getProperty(propertyName);
		try {
			return Integer.parseInt(value);
		} catch(NumberFormatException ex) {
			return defaultValue;
		}
	}
	
	protected String[] getPropertyValues(String propertyName, String... defaultValues) {
		LinkedList<String> values = new LinkedList<String>();

		for(int i=0; i>=0; ++i) {
			String val = getProperty(propertyName + "." + i);
			if(val == null) break;
			else values.add(val);
		}
		
		if(values.isEmpty()) {
			return defaultValues;
		} else {
			return values.toArray(new String[0]);
		}
	}
	
	/** Sets the {@link Integer} value of a property. */
	protected void setPropertyAsInteger(String propertyName, Integer value) {
		if(value == null) setProperty(propertyName, null);
		else setProperty(propertyName, Integer.toString(value));
	}
	
	/** @return the property keys in {@link #properties} */
	protected Set<String> getPropertyKeys() {
		return this.getProperties().keySet();
	}
	
	/** @return {@link #file} */
	protected File getFile() {
		return file;
	}
	
	/** @return {@link BasePropertySet#getProperties()} */
	public Map<String, String> getProperties() {
		return super.getProperties();
	}
	
//> GETTERS WITH DEFAULT VALUES
	/**
	 * Gets the {@link String} value of a property.  If no value is set, the default value is set and then returned.
	 * @param propertyName The name of this property
	 * @param defaultValue The value to use for this property if none is yet set
	 * @return The value to be used for this property
	 */
	protected synchronized String getProperty(String propertyName, String defaultValue) {
		if(!this.getProperties().containsKey(propertyName)) {
			this.getProperties().put(propertyName, defaultValue);
		}
		return this.getProperties().get(propertyName);
	}
	
//> STATIC FACTORIES
	public static FilePropertySet load(String filePath) {
		File file = new File(filePath);
		return load(file);
	}
	
	public static FilePropertySet load(File file) {
		FilePropertySet properties = new FilePropertySet(file);
		properties.setProperties(loadPropertyMap(file));
		return properties;
	}
	
	/**
	 * Loads a {@link UserHomeFilePropertySet} from the supplied file
	 * @param propFile The file to load the {@link UserHomeFilePropertySet} from
	 * @return new map of properties loaded from the requested file, or an empty map if no properties could be loaded. 
	 */
	protected static HashMap<String, String> loadPropertyMap(File propFile) {
		HashMap<String, String> properties = new HashMap<String, String>();
		loadPropertyMap(properties, propFile);
		
		return properties;
	}
	
	/**
	 * Loads a {@link UserHomeFilePropertySet} from the supplied file
	 * @param propFile The file to load the {@link UserHomeFilePropertySet} from
	 * @return new map of properties loaded from the requested file, or an empty map if no properties could be loaded. 
	 */
	protected static void loadPropertyMap(Map<String, String> map, File propFile) {
		LOG.debug("File [" + propFile.getAbsolutePath() + "]");

		FileInputStream fis = null;
		BufferedReader in = null;
		try {
			fis = new FileInputStream(propFile);
			BasePropertySet.load(map, fis);
		} catch(FileNotFoundException ex) {
			LOG.debug("Properties file not found [" + propFile.getAbsolutePath() + "]", ex);
		} catch(IOException ex) {
			LOG.debug("Exception thrown while loading properties file:" + propFile.getAbsolutePath(), ex);
		} finally {
			// Close all streams
			if(fis != null) try { fis.close(); } catch(Exception ex) {
				// nothing we can do except log the exception
				LOG.warn("Exception thrown while closing stream 'fis'.", ex);
			}
			if(in != null) try { in.close(); } catch(IOException ex) {
				// nothing we can do except log the exception
				LOG.warn("Exception thrown while closing stream 'fis'.", ex);
			}
		}
		LOG.trace("EXIT");
	}
}
