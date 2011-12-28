/**
 * 
 */
package net.frontlinesms.ui;

import net.frontlinesms.resources.UserHomeFilePropertySet;

/**
 * Wrapper class for UI properties file.
 * @author Alex
 */
public final class UiProperties extends UserHomeFilePropertySet {
//> STATIC CONSTANTS
	/** The name of the {@link UserHomeFilePropertySet} which these properties are loaded from and saved in. */
	private static final String PROPERTYSET_NAME = "ui";
	
//> PROPERTY KEYS & VALUES
	/** Property key (int): Window Width */
	public static final String KEY_WINDOW_WIDTH = "window.width";
	/** Property key (int): Window Height */
	public static final String KEY_WINDOW_HEIGHT = "window.height";

	/** Property key (String): Window State */
	public static final String KEY_WINDOW_STATE = "window.state";
	/** Property value for {@link #KEY_WINDOW_STATE}: maximised */
	public static final String WINDOW_STATE_MAXIMISED = "maximised";
	/** Property value for {@link #KEY_WINDOW_STATE}: not maximised */
	public static final String WINDOW_STATE_NORMAL = "normal";

	/** Property Key (boolean) indicating if the logo is visible */
	public static final String KEY_HOMETABLOGO_VISIBLE = "hometab.logo.visible";
	/** Property Key (boolean) indicating if the logo is the default logo */
	public static final String KEY_HOMETABLOGO_CUSTOM = "hometab.logo.custom";
	/** Property Key (String) indicating the path to image file containing the logo. */
	public static final String KEY_HOMETABLOGO_SOURCE = "hometab.logo.source";
	/** Property Key (String) indicating whether the custom logo should keep its original size. */
	public static final String KEY_HOMETABLOGO_KEEP_ORIGINAL_SIZE = "hometab.logo.keeporiginalsize";
	/** Property Key (int) the number of items to display on the home tab event list */
	public static final String KEY_HOMETAB_EVENT_LIMIT = "hometab.events.limit";
	/** Property key (int) the number of items to display per page */
	public static final String KEY_ITEMS_PER_PAGE = "paging.itemcount";

	/** Property Key (String) currency of currently selected language */
	public static final String CURRENCY_FORMAT = "currency.format";
	/** Property key (boolean) indicating whether the user uses a custom currency format */
	public static final String CURRENCY_FORMAT_IS_CUSTOM = "currency.format.is.custom";
	
	/** Singleton instance of this class. */
	private static UiProperties instance;
	

	private static final String DEFAULT_CURRENCY_FORMAT = "£#,##0.00";

//> INSTANCE PROPERTIES

//> CONSTRUCTORS
	/** Create a new instance of this class. */
	private UiProperties() {
		super(PROPERTYSET_NAME);
	}

//> STATIC ACCESSORS
	/**
	 * Sets the property to make a tab visible or invisible.
	 * @param tabName The name of the tab.
	 * @param visible <code>true</code> if the tab should be visible, <code>false</code> otherwise.
	 */
	public void setTabVisible(String tabName, boolean visible) {
		super.setPropertyAsBoolean(tabName + ".visible", visible);
	}
	/** 
	 * @param tabName The name of the tab.
	 * @return <code>true</code> if the tab should be visible, <code>false</code> otherwise.
	 */
	public boolean isTabVisible(String tabName) {
		return super.getPropertyAsBoolean(tabName + ".visible", true);
	}
	
	/** @return value for {@link #KEY_WINDOW_STATE} */
	public boolean isWindowStateMaximized() {
		String windowState = super.getProperty(KEY_WINDOW_STATE);
		return windowState != null
				&& windowState.equals(WINDOW_STATE_MAXIMISED);
	}	
	/** @return the saved width of the window, or <code>null</code> if none was set. */
	public Integer getWindowWidth() {
		String widthAsString = super.getProperty(UiProperties.KEY_WINDOW_WIDTH);
		Integer width = null;
		if(widthAsString != null) {
			try {
				width = Integer.parseInt(widthAsString);
			} catch (NumberFormatException ex) { /* Do nothing - we will return null */ }
		}
		return width;
	}
	/** @return the saved height of the window, or <code>null</code> if none was set. */
	public Integer getWindowHeight() {
		String heightAsString = super.getProperty(UiProperties.KEY_WINDOW_HEIGHT);
		Integer height = null;
		if(heightAsString != null) {
			try {
				height = Integer.parseInt(heightAsString);
			} catch (NumberFormatException ex) { /* Do nothing - we will return null */ }
		}
		return height;
	}
	/**
	 * Set the window state and dimensions.
	 * @param maximized
	 * @param width
	 * @param height
	 */
	public void setWindowState(boolean maximized, int width, int height) {
		super.setProperty(KEY_WINDOW_STATE,
				maximized ? WINDOW_STATE_MAXIMISED : WINDOW_STATE_NORMAL);
		super.setProperty(UiProperties.KEY_WINDOW_WIDTH, String.valueOf(width));
		super.setProperty(UiProperties.KEY_WINDOW_HEIGHT, String.valueOf(height));
	}
	
	/** @return <code>true</code> if the logo should be shown on the home tab; <code>false</code> otherwise */
	public boolean isHometabLogoVisible() {
		return super.getPropertyAsBoolean(KEY_HOMETABLOGO_VISIBLE, true);
	}
	/**
	 * Set visibility of the logo on the home tab.
	 * @param visible value for property {@link #KEY_HOMETABLOGO_VISIBLE}
	 */
	public void setHometabLogoVisible(boolean visible) {
		super.setPropertyAsBoolean(KEY_HOMETABLOGO_VISIBLE, visible);
	}
	
	/** @return <code>true</code> if the logo shown on the home tab is the default logo; <code>false</code> otherwise */
	public boolean isHometabCustomLogo() {
		return super.getPropertyAsBoolean(KEY_HOMETABLOGO_CUSTOM, false);
	}
	
	
	/**
	 * Set logo on the home tab (default or custom).
	 * @param isCustomLogo value for property {@link #KEY_HOMETABLOGO_CUSTOM}
	 */
	public void setHometabCustomLogo(boolean isCustomLogo) {
		super.setPropertyAsBoolean(KEY_HOMETABLOGO_CUSTOM, isCustomLogo);
	}
	

	/** @return <code>true</code> if the custom logo should keep its original size; <code>false</code> otherwise */
	public boolean isHometabLogoOriginalSizeKept() {
		return super.getPropertyAsBoolean(KEY_HOMETABLOGO_KEEP_ORIGINAL_SIZE, false);
	}
	
	/**
	 * Set whether the custom logo should keep its original size.
	 * @param isOriginalSizeKept value for property {@link #KEY_HOMETABLOGO_KEEP_ORIGINAL_SIZE}
	 */
	public void setHometabLogoOriginalSizeKept(boolean isOriginalSizeKept) {
		super.setPropertyAsBoolean(KEY_HOMETABLOGO_KEEP_ORIGINAL_SIZE, isOriginalSizeKept);
	}	
	
	/** @return the path to the file containing the logo to display on the home tab */
	public String getHometabLogoPath() {
		return super.getProperty(KEY_HOMETABLOGO_SOURCE);
	}
	/**
	 * Set path of the logo on the home tab.
	 * @param path value for property {@link #KEY_HOMETABLOGO_SOURCE}
	 */
	public void setHometabLogoPath(String path) {
		super.setProperty(KEY_HOMETABLOGO_SOURCE, path);
	}

	/** @return number of items to display per page */
	public int getItemsPerPage() {
		return super.getPropertyAsInt(KEY_ITEMS_PER_PAGE, 100);
	}
	
	/** @return currency format string to be used for currency formatting */
	public String getCustomCurrencyFormat() {
		return super.getProperty(CURRENCY_FORMAT) == null ? DEFAULT_CURRENCY_FORMAT : super.getProperty(CURRENCY_FORMAT);
	}
	
	/** @param format the currency format string to be used for currency formatting*/
	public void setCustomCurrencyFormat(String format) {
		super.setProperty(CURRENCY_FORMAT, format);
	}
	
	/** @return <code>true</code> if the user uses a custom currency format, <code>false</code> otherwise. */
	public boolean isCurrencyFormatCustom() {
		return super.getPropertyAsBoolean(CURRENCY_FORMAT_IS_CUSTOM, false);
	}
	
	/** @param isCurrencyFormatCustom A boolean indicating whether the user uses a custom currency format */
	public void setIsCurrencyFormatCustom(boolean isCurrencyFormatCustom) {
		super.setPropertyAsBoolean(CURRENCY_FORMAT_IS_CUSTOM, isCurrencyFormatCustom);
	}

	/** @return the number of events to display in the home tab event list */
	public int getHomeTabEventLimit() {
		return super.getPropertyAsInt(KEY_HOMETAB_EVENT_LIMIT, 100);
	}
	
//> INSTANCE HELPER METHODS

//> STATIC FACTORIES
	/**
	 * Lazy getter for {@link #instance}
	 * @return The singleton instance of this class
	 */
	public static synchronized UiProperties getInstance() {
		if(instance == null) {
			instance = new UiProperties();
		}
		return instance;
	}

//> STATIC HELPER METHODS
}
