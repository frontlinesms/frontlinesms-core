/**
 * 
 */
package net.frontlinesms.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

/**
 * Base implementation of the {@link PluginController} annotation.
 * 
 * Implementers of this class *must* carry the {@link PluginControllerProperties} annotation.
 * 
 * This class includes default implementation of the text resource loading methods.  These attempt to load text resources
 * in the following way:
 * TODO properly document how this is done from the methods {@link #getDefaultTextResource()} and {@link #getTextResource(Locale)}.
 * 
 * @author Alex
 */
public abstract class BasePluginController implements PluginController {
//> STATIC CONSTANTS

//> INSTANCE PROPERTIES
	/** Logging object for this class */
	protected final Logger log = FrontlineUtils.getLogger(this.getClass());
	/** Lazy-initialized singleton Thinlet Tab component for this instance of this plugin. */
	private Object thinletTab;
	/**
	 * The instance of {@link UiGeneratorController} used to {@link #initThinletTab(UiGeneratorController)}
	 * the current value of {@link #thinletTab}.  This is used to check whether the {@link UiGeneratorController} passed to
	 * {@link #getTab(UiGeneratorController)} is the same instance that was used to create the tab in the first place.  If the
	 * {@link UiGeneratorController} has changed, the tab will be discarded and this value reset.
	 */
	private UiGeneratorController tabUiController;

//> CONSTRUCTORS

//> ACCESSORS
	/** @see net.frontlinesms.plugins.PluginController#getTab(net.frontlinesms.ui.UiGeneratorController) */
	public synchronized Object getTab(UiGeneratorController uiController) {
		// N.B. we are deliberately checking the references of the UiGeneratorController here, rather
		// than .equals() as we just want to know if it is the same instance.
		if(this.thinletTab == null || this.tabUiController!=uiController) {
			this.tabUiController = uiController;
			this.thinletTab = this.initThinletTab(uiController);
		}
		return this.thinletTab;
	}
	
	/**
	 * Initialise the Thinlet tab component for this plugin instance.  This method should ONLY initialise
	 * the visible UI.  The tab itself can be discarded and re-initialised at any time by the
	 * {@link BasePluginController}.  Notably this will happen when the display language of the UI is
	 * changed. 
	 * @param uiController {@link UiGeneratorController} instance that will be the parent of this tab.
	 * @return a new instance of the thinlet tab component for this plugin.
	 */
	protected abstract Object initThinletTab(UiGeneratorController uiController);
	
	/**
	 * @see PluginController#getName(Locale locale)
	 * This actually loads the whole property file for this plugin and takes the plugin name.
	 * Try to avoid calling this function frequently.
	 **/
	public String getName(Locale locale) {
		assert(this.getClass().isAnnotationPresent(PluginControllerProperties.class)): "Implementers of this class *must* implement the PluginControllerProperties annotation and specify the i18nKey attribute.";
		String pluginName = getTextResource(locale).get(this.getClass().getAnnotation(PluginControllerProperties.class).i18nKey());
		if (pluginName == null) {
			return this.getClass().getAnnotation(PluginControllerProperties.class).name();
		} else {
			return pluginName;
		}
	}
	
	/**
	 * Override if the plugin needs settings
	 */
	public PluginSettingsController getSettingsController(UiGeneratorController uiController) {
		return null;
	}
	
	/** @see net.frontlinesms.plugins.PluginController#getDefaultTextResource() */
	public Map<String, String> getDefaultTextResource() {
		Map<String, String> defaultTextResource = getTextResource();
		if(defaultTextResource != null) return defaultTextResource;
		else return Collections.emptyMap();
	}
	
	/** @see net.frontlinesms.plugins.PluginController#getTextResource(java.util.Locale) */
	public Map<String, String> getTextResource(Locale locale) {
		String variant = locale.getVariant();
		String country = locale.getCountry();
		String language = locale.getLanguage();
		
		if(variant != null && variant.length() > 0) {
			Map<String, String> textResource = getTextResource(language, country, variant);
			if(textResource != null) return textResource;
		}
		
		if(country != null && country.length() > 0) {
			Map<String, String> textResource = getTextResource(language, country);
			if(textResource != null) return textResource;
		}
		
		if(language != null && language.length() > 0) {
			Map<String, String> textResource = getTextResource(language);
			if(textResource != null) return textResource;
		}
		
		return Collections.emptyMap();
	}

//> INSTANCE HELPER METHODS	
	/**
	 * Gets a text resource file from the classpath.
	 * @param nameExtensions extensions added to the end of the standard filename.  These will be separated from the base name and each other by underscores.
	 * @return The text resource, or <code>null</code> if the resource could not be found.
	 */
	final Map<String, String> getTextResource(String... nameExtensions) {
		String resourceFilePath = getTextResourcePath(nameExtensions);
		
		// Attempt to load the text resource using relative path with local classloader
		InputStream textResourceInputStream = this.getClass().getResourceAsStream(resourceFilePath);
		
		if(textResourceInputStream == null) {
			// Resource could not be found, so return null
			return null;
		} else {
			try {
				return InternationalisationUtils.loadTextResources(resourceFilePath, textResourceInputStream);
			} catch (IOException ex) {
				log.info("There was a problem loading language bundle from " + resourceFilePath, ex);
				return null;
			}
		}
	}
	
	/**
	 * Gets the path for a text resource bundle.
	 * @param nameExtensions extensions added to the end of the standard filename.  These will be separated from the base name and each other by underscores.
	 * @return classpath location of the text resource bundle
	 */
	private String getTextResourcePath(String... nameExtensions) {
		String resourceFilePath = getTextResourceFilename(nameExtensions) + ".properties";
		return resourceFilePath;
	}
	
	/**
	 * Gets the filename for the text resource bundle.
	 * @param nameExtensions extensions added to the end of the standard filename.  These will be separated from the base name and each other by underscores.
	 * @return File name for the text resource bundle
	 */
	private String getTextResourceFilename(String ... nameExtensions) {
		// Construct the name of the .properties file
		String fileName =  this.getClass().getSimpleName();
		
		// Add suffix "Text" to file name, BEFORE the language & country suffixes
		fileName += "Text";
		
		// Append any required extensions to the filename
		for(String extension : nameExtensions) {
			if(extension != null) {
				fileName += "_" + extension;
			}
		}
		
		return fileName;
	}

	/**
	 * Gets the main icon of the Plugin 
	 * @return
	 */
	public String getIcon(Class<? extends BasePluginController> clazz) {
		if(clazz.isAnnotationPresent(PluginControllerProperties.class)) {
			PluginControllerProperties properties = clazz.getAnnotation(PluginControllerProperties.class);
			return properties.iconPath();
		} else {
			return '/' + clazz.getPackage().getName().replace('.', '/') + '/' + clazz.getSimpleName() + ".png";
		}
	}

//> STATIC FACTORIES

//> STATIC HELPER METHODS
}
