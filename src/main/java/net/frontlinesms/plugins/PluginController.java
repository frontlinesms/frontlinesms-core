/**
 * 
 */
package net.frontlinesms.plugins;

import java.util.Locale;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import net.frontlinesms.FrontlineSMS;
import net.frontlinesms.ui.UiGeneratorController;

/**
 * Basic interface that all FrontlineSMS plugins must implement.
 * Implementers must also have an empty no-arg constructor.  This may be called at any time, and
 * should not initialise variables. 
 * @author Alex
 */
public interface PluginController {
	/**
	 * Gets the name for this plugin from the key given in the annotation.
	 * Please note that this function may perform some intensive resources loading.
	 * @param locale 
	 * @return The name of this plugin.
	 */
	public String getName(Locale locale);
	
	/**
	 * Initialise the plugin from the {@link FrontlineSMS} controller instance. 
	 * @param frontlineController {@link FrontlineSMS} instance that this plugin is "plugged-in" to.
	 * @param applicationContext {@link ApplicationContext} for FrontlineSMS config
	 * @throws PluginInitialisationException if there was an identified problem initialising the plugin
	 */
	public void init(FrontlineSMS frontlineController, ApplicationContext applicationContext) throws PluginInitialisationException;
	
	/** Deinitialise the plugin.  This method is called immediately before this plugin instance is discarded. */
	public void deinit();
	
	/**
	 * Gets the tab for this plugin.
	 * Multiple calls to this method should always return the exact same object (i.e. satisfies == equality). 
	 * @param uiController {@link UiGeneratorController} instance that will be the parent of this tab.
	 * @return the tab to display for this plugin
	 */
	public Object getTab(UiGeneratorController uiController);
	
	/**
	 * N.B. This can be called when a plugin is not enabled.
	 * @param uiController
	 * @return
	 */
	public PluginSettingsController getSettingsController(UiGeneratorController uiController);

	/**
	 * Gets the default language bundle for text strings used in the UI of this plugin.
	 * @return map of text keys to English translations of strings used in the UI of this plugin, or an empty map if no text keys are required for this plugin
	 */
	public Map<String, String> getDefaultTextResource();
	
	/**
	 * Get the language bundle for text string to be used for the UI of this plugin in a particular language.
	 * @param locale the {@link Locale} of the translation to use
	 * @return map of text keys to translations of strings used in the UI of this plugin, or an empty map if there is no translation available for this plugin.
	 */
	public Map<String, String> getTextResource(Locale locale);
}
