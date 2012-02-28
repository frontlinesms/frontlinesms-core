package net.frontlinesms.ui.handler.settings;

import java.util.ArrayList;
import java.util.List;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.settings.FrontlineValidationMessage;
import net.frontlinesms.settings.BaseSectionHandler;
import net.frontlinesms.ui.FrontlineUI;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.UiProperties;
import net.frontlinesms.ui.i18n.FileLanguageBundle;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.settings.HomeTabLogoChangedNotification;
import net.frontlinesms.ui.settings.UiSettingsSectionHandler;

import org.apache.log4j.Logger;

/**
 * UI Handler for the "Appearance" section of the Core Settings
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public class SettingsAppearanceSectionHandler extends BaseSectionHandler implements UiSettingsSectionHandler, ThinletUiEventHandler {
	protected final Logger log = FrontlineUtils.getLogger(this.getClass());

	private static final String UI_SECTION_APPEARANCE = "/ui/core/settings/appearance/pnAppearanceSettings.xml";

	/** Thinlet Component Name: Settings dialog: radio indicating if the logo is visible */
	private static final String COMPONENT_CB_HOME_TAB_LOGO_INVISIBLE = "cbHomeTabLogoInvisible";
	/** Thinlet Component Name: Settings dialog: radio used to choose the default logo */
	private static final String COMPONENT_CB_HOME_TAB_USE_DEFAULT_LOGO = "cbHomeTabLogoDefault";
	/** Thinlet Component Name: Settings dialog: radio used to choose a custom logo */
	private static final String COMPONENT_CB_HOME_TAB_USE_CUSTOM_LOGO = "cbHomeTabLogoCustom";
	/** Thinlet Component Name: Settings dialog: checkbox used to choose a custom logo */
	private static final String COMPONENT_CB_HOME_TAB_LOGO_KEEP_ORIGINAL_SIZE = "cbHomeTabLogoKeepOriginalSize";
	/** Thinlet Component Name: Settings dialog: panel grouping the path of the image file for the logo */
	private static final String COMPONENT_PN_CUSTOM_IMAGE = "pnCustomImage";
	/** Thinlet Component Name: Settings dialog: textfield inidicating the path of the image file for the logo */
	private static final String COMPONENT_TF_IMAGE_SOURCE = "tfImageSource";

	private static final String COMPONENT_PN_LANGUAGES = "fastLanguageSwitch";

	private static final String SECTION_ITEM_IMAGE_SOURCE = "APPEARANCE_LOGO_IMAGE_SOURCE";
	private static final String SECTION_ITEM_KEEP_LOGO_ORIGINAL_SIZE = "APPEARANCE_LOGO_ORIGINAL_SIZE";
	private static final String SECTION_ITEM_LOGO_TYPE = "APPEARANCE_LOGO_RADIOBUTTONS";
	private static final String SECTION_ITEM_LANGUAGE = "APPEARANCE_LANGUAGE";

	private static final String I18N_SETTINGS_MESSAGE_EMPTY_CUSTOM_LOGO = "settings.message.empty.custom.logo";
	private static final String I18N_SETTINGS_MENU_APPEARANCE = "settings.menu.appearance";

	private static final String SECTION_ICON = "/icons/display.png";

	public SettingsAppearanceSectionHandler (UiGeneratorController uiController) {
		super(uiController);
		this.ui = uiController;
	}
	
	protected void init() {
		this.panel = ui.loadComponentFromFile(UI_SECTION_APPEARANCE, this);
		
		initLanguageSettings();
		initLogoSettings();
	}
	
	public void deinit() {}

	private void initLanguageSettings() {
		Object fastLanguageSwitch = find(COMPONENT_PN_LANGUAGES);
		for (FileLanguageBundle languageBundle : InternationalisationUtils.getLanguageBundles()) {
			Object button = this.ui.createRadioButton("", "", "rdGroupLanguage", languageBundle.equals(FrontlineUI.currentResourceBundle));
			this.ui.setIcon(button, this.ui.getFlagIcon(languageBundle));
			this.ui.setString(button, "tooltip", languageBundle.getLanguageName());
			this.ui.setWeight(button, 1, 0);
			this.ui.setAttachedObject(button, languageBundle);
			this.ui.setAction(button, "languageChanged", null, this);
			
			this.ui.add(fastLanguageSwitch, button);
		}
		
		this.originalValues.put(SECTION_ITEM_LANGUAGE, FrontlineUI.currentResourceBundle);
	}
	
	/** Show the settings dialog for the home tab. */
	public void initLogoSettings() {
		log.trace("ENTER");
		UiProperties uiProperties = UiProperties.getInstance();
		boolean visible 			= uiProperties.isHometabLogoVisible();
		boolean isCustomLogo 		= uiProperties.isHometabCustomLogo();
		boolean isOriginalSizeKept 	= uiProperties.isHometabLogoOriginalSizeKept();
		
		String imageLocation = uiProperties.getHometabLogoPath();
		log.debug("Visible? " + visible);
		log.debug("Logo: " + (isCustomLogo ? "custom" : "default"));
		if (isCustomLogo)
			log.debug("Keep original size: " + isOriginalSizeKept);
		log.debug("Image location [" + imageLocation + "]");
		
		String radioButtonName;
		if(!visible) {
			radioButtonName = COMPONENT_CB_HOME_TAB_LOGO_INVISIBLE;
		} else if(isCustomLogo) {
			radioButtonName = COMPONENT_CB_HOME_TAB_USE_CUSTOM_LOGO;
		} else {
			radioButtonName = COMPONENT_CB_HOME_TAB_USE_DEFAULT_LOGO;
		}
		
		this.ui.setSelected(find(radioButtonName), true);
		this.ui.setSelected(find(COMPONENT_CB_HOME_TAB_LOGO_KEEP_ORIGINAL_SIZE), isOriginalSizeKept);
		
		setHomeTabCustomLogo(find(COMPONENT_PN_CUSTOM_IMAGE), isCustomLogo && visible);
		
		if (imageLocation != null && imageLocation.length() > 0) {
			this.ui.setText(find(COMPONENT_TF_IMAGE_SOURCE), imageLocation);
		}
		
		// Save the original values
		this.originalValues.put(SECTION_ITEM_LOGO_TYPE, radioButtonName);
		this.originalValues.put(SECTION_ITEM_KEEP_LOGO_ORIGINAL_SIZE, isOriginalSizeKept);
		this.originalValues.put(SECTION_ITEM_IMAGE_SOURCE, imageLocation);
				
		log.trace("EXIT");
	}

	public void save() {
		log.trace("Saving appearance settings...");
		
		/**** LOGO ****/
		boolean invisible = ui.isSelected(find(COMPONENT_CB_HOME_TAB_LOGO_INVISIBLE));
		boolean isCustomLogo = ui.isSelected(find(COMPONENT_CB_HOME_TAB_USE_CUSTOM_LOGO));
		boolean isOriginalSizeKept = ui.isSelected(find(COMPONENT_CB_HOME_TAB_LOGO_KEEP_ORIGINAL_SIZE));
		
		String imgSource = ui.getText(find(COMPONENT_TF_IMAGE_SOURCE));
		log.debug("Hidden? " + invisible);
		log.debug("Logo: " + (isCustomLogo ? "default" : "custom"));
		log.debug("Image location [" + imgSource + "]");
		UiProperties uiProperties = UiProperties.getInstance();
		uiProperties.setHometabLogoVisible(!invisible);
		uiProperties.setHometabCustomLogo(isCustomLogo);
		uiProperties.setHometabLogoOriginalSizeKept(isOriginalSizeKept);
		uiProperties.setHometabLogoPath(imgSource);
		uiProperties.saveToDisk();

		// Update visibility of logo
		this.eventBus.notifyObservers(new HomeTabLogoChangedNotification());
		
		
		/**** LANGUAGE ****/
		for (Object radioButton : this.ui.getItems(find(COMPONENT_PN_LANGUAGES))) {
			if (this.ui.isSelected(radioButton)) {
				FileLanguageBundle languageBundle = this.ui.getAttachedObject(radioButton, FileLanguageBundle.class);
				if (languageBundle != null && !languageBundle.equals(FrontlineUI.currentResourceBundle)) {
					this.ui.setAttachedObject(radioButton, languageBundle.getFile().getAbsolutePath());
					if (this.ui instanceof UiGeneratorController) {
						((UiGeneratorController) this.ui).changeLanguage(radioButton);
					}
				}
				break;
			}
		}
		
		log.trace("EXIT");
	}

	public List<FrontlineValidationMessage> validateFields() {
		List<FrontlineValidationMessage> validationMessages = new ArrayList<FrontlineValidationMessage>();

		// Home tab logo
		if (this.ui.isSelected(find(COMPONENT_CB_HOME_TAB_USE_CUSTOM_LOGO))
				&& this.ui.getText(find(COMPONENT_TF_IMAGE_SOURCE)).isEmpty()) {
			validationMessages.add(new FrontlineValidationMessage (I18N_SETTINGS_MESSAGE_EMPTY_CUSTOM_LOGO, null, getIcon()));
		}
		
		return validationMessages;
	}
	
	/**
	 * Enable or disable the bottom panel whether the logo is custom or not.
	 * @param panel
	 * @param isCustom <code>true</code> if the logo is a custom logo; <code>false</code> otherwise.
	 */
	public void setHomeTabCustomLogo(Object panel, boolean isCustom) {
		this.ui.setEnabled(panel, isCustom);
		for (Object obj : this.ui.getItems(panel)) {
			this.ui.setEnabled(obj, isCustom);
		}
	}
	
	public void languageChanged() {
		for (Object radioButton : this.ui.getItems(find(COMPONENT_PN_LANGUAGES))) {
			if (this.ui.isSelected(radioButton)) {
				FileLanguageBundle languageBundle = this.ui.getAttachedObject(radioButton, FileLanguageBundle.class);
				super.settingChanged(SECTION_ITEM_LANGUAGE, languageBundle);
				break;
			}
		}
	}
	
	public void logoRadioButtonChanged(Object panel, boolean isCustom) {
		this.setHomeTabCustomLogo(panel, isCustom);
		
		Object newValue;
		if (this.ui.isSelected(find(COMPONENT_CB_HOME_TAB_LOGO_INVISIBLE))) {
			newValue = COMPONENT_CB_HOME_TAB_LOGO_INVISIBLE;
		} else if (this.ui.isSelected(find(COMPONENT_CB_HOME_TAB_USE_CUSTOM_LOGO))) {
			newValue = COMPONENT_CB_HOME_TAB_USE_CUSTOM_LOGO;
		} else {
			newValue = COMPONENT_CB_HOME_TAB_USE_DEFAULT_LOGO;
		}
		
		super.settingChanged(SECTION_ITEM_LOGO_TYPE, newValue);
	}
	
	public void customImageSourceChanged(String imageSource) {
		this.ui.setText(find(COMPONENT_TF_IMAGE_SOURCE), imageSource);
		
		this.settingChanged(SECTION_ITEM_IMAGE_SOURCE, imageSource);
	}
	
	public void shouldLogoKeepOriginalSizeChanged(boolean shouldLogoKeepOriginalSize) {
		super.settingChanged(SECTION_ITEM_KEEP_LOGO_ORIGINAL_SIZE, shouldLogoKeepOriginalSize);
	}
	
	/**
	 * @param component
	 * @see UiGeneratorController#showOpenModeFileChooser(Object)
	 */
	public void showFileChooser(Object component) {
		this.ui.showFileChooser(this, "customImageSourceChanged");
	}

	public String getTitle() {
		return InternationalisationUtils.getI18nString(I18N_SETTINGS_MENU_APPEARANCE);
	}
	
	public Object getSectionNode() {
		return createSectionNode(InternationalisationUtils.getI18nString(I18N_SETTINGS_MENU_APPEARANCE), this, this.getIcon());
	}

	private String getIcon() {
		return SECTION_ICON;
	}
}
