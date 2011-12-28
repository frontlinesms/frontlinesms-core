/**
 * 
 */
package net.frontlinesms;

import java.io.IOException;

import net.frontlinesms.resources.ClasspathPropertySet;

/**
 * Loads and reads a properties file from the classpath.  This properties file is created at build time.
 * @author Alex
 */
public final class BuildProperties extends ClasspathPropertySet {
//> PROPERTY KEYS
	/** Property key: application build version */
	private static final String PROPERTY_VERSION = "Version";
	
//> STATIC CONSTANTS
	/** Singleton instance of this class. */
	private static BuildProperties instance;

//> INSTANCE PROPERTIES

//> CONSTRUCTORS
	/**
	 * Creates the singleton instance of this class by loading the properties from the commandline.
	 * @throws IOException If the class
	 */
	private BuildProperties() throws IOException {
		super("/net/frontlinesms/build.properties");
	}

//> ACCESSORS
	/** @return the version of FrontlineSMS that we are running. */
	public String getVersion() {
		return super.getProperty(PROPERTY_VERSION);
	}

	public boolean isSnapshot() {
		return getVersion().contains("SNAPSHOT");
	}

//> INSTANCE HELPER METHODS

//> STATIC FACTORIES

//> STATIC HELPER METHODS
	/**
	 * Lazy getter for {@link #instance}
	 * @return The singleton instance of this class
	 */
	public static synchronized BuildProperties getInstance() {
		if(instance == null) {
			try {
				instance = new BuildProperties();
			} catch (Exception ex) {
				// If we can't find the build properties, we may have serious issues later on
				throw new IllegalStateException("Could not load build properties!", ex);
			}
		}
		return instance;
	}
}
