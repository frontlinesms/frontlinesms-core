package net.frontlinesms.ui;

import net.frontlinesms.resources.MultiLocationClasspathPropertySet;

public class IconMap extends MultiLocationClasspathPropertySet {
//> STATIC CONSTANTS

//> INSTANCE PROPERTIES
	
//> CONSTRUCTORS
	/**
	 * Set up a new {@link IconMap}.
	 * @param name the name of the icon map
	 */
	IconMap(String path) {
		super(path);
	}

//> ACCESSORS
	/**
	 * Check if there is an icon available. 
	 * @param key The key for the icon.
	 * @return <code>true</code> if there is an icon specified for this key; <code>false</code> otherwise.
	 */
	public boolean hasIcon(String key) {
		return this.getIcon(key) != null;
	}
	
	/**
	 * Get the icon path for the specified key.
	 * @param key
	 * @return
	 */
	public String getIcon(String key) {
		return super.getProperty(key);
	}

//> INSTANCE HELPER METHODS

//> STATIC FACTORIES

//> STATIC HELPER METHODS
}
