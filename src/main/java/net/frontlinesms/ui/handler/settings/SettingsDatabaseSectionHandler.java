package net.frontlinesms.ui.handler.settings;

import java.util.ArrayList;
import java.util.List;

import net.frontlinesms.AppProperties;
import net.frontlinesms.settings.BaseSectionHandler;
import net.frontlinesms.settings.DatabaseSettings;
import net.frontlinesms.settings.FrontlineValidationMessage;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.settings.UiSettingsSectionHandler;

/**
 * UI Handler for the "General/Database" section of the Core Settings
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public class SettingsDatabaseSectionHandler extends BaseSectionHandler implements UiSettingsSectionHandler, ThinletUiEventHandler {
	private static final String UI_SECTION_DATABASE = "/ui/core/settings/general/pnDatabaseSettings.xml";
	private static final String UI_SECTION_DATABASE_AS_DIALOG = "/ui/core/database/pnSettings.xml";

	private static final String I18N_SETTINGS_MENU_DATABASE_SETTINGS  = "menuitem.edit.db.settings";

	/** The combobox containing the different databases */
	private static final String COMPONENT_SETTINGS_SELECTION = "cbConfigFile";
	/** The panel containing individual settings controls */
	private static final String COMPONENT_PN_DATABASE_SETTINGS = "pnSettings";
	/** The constant property key for database passwords */
	private static final String PASSWORD_PROPERTY_KEY = "password";
	private static final String I18N_MESSAGE_DATABASE_SETTINGS_CHANGED = "message.database.settings.changed";
	
	private static final String SECTION_ITEM_DATABASE_CONFIG_PATH = "GENERAL_DATABASE_CONFIG_PATH";
	private static final String SECTION_ITEM_DATABASE_CONFIG = "GENERAL_DATABASE_CONFIG_";
	
	/** The settings currently selected in the combobox */
	private DatabaseSettings selectedSettings;

	private Object dialogComponent;
	
	public SettingsDatabaseSectionHandler (UiGeneratorController ui) {
		super(ui);
	}
	
	protected void init() {
		this.panel = ui.loadComponentFromFile(UI_SECTION_DATABASE, this);
		
		// Populate combobox
		String selectedDatabaseConfigPath = AppProperties.getInstance().getDatabaseConfigPath();
		List<DatabaseSettings> databaseSettings = DatabaseSettings.getSettings();
		Object settingsSelection = find(COMPONENT_SETTINGS_SELECTION);
		for(int settingsIndex = 0; settingsIndex < databaseSettings.size(); ++settingsIndex) {
			DatabaseSettings settings = databaseSettings.get(settingsIndex);
			Object comboBox = createComboBoxChoice(settings);
			this.ui.add(settingsSelection, comboBox);

			// if appropriate, choose the combobox selection
			if(settings.getFilePath().equals(selectedDatabaseConfigPath)) {
				this.selectedSettings = settings;
				this.ui.setSelectedIndex(settingsSelection, settingsIndex);
			}
		}
		
		this.originalValues.put(SECTION_ITEM_DATABASE_CONFIG_PATH, selectedDatabaseConfigPath);
		
		// populate settings panel
		refreshSettingsPanel();
	}
	
	private Object createComboBoxChoice(DatabaseSettings settings) {
		Object cb = this.ui.createComboboxChoice(settings.getName(), settings);
		// TODO perhaps we could set a settings-specific icon here
		return cb;
	}
	
	/** Refresh the panel containing settings specific to the currently-selected {@link DatabaseSettings}. */
	private void refreshSettingsPanel() {
		// populate the settings panel
		Object settingsPanel = find(COMPONENT_PN_DATABASE_SETTINGS);
		this.ui.removeAll(settingsPanel);
		
		this.selectedSettings.loadProperties();
		for(String key : this.selectedSettings.getPropertyKeys()) {
			// TODO would be nice to set icons for the different settings
			this.ui.add(settingsPanel, this.ui.createLabel(key));
			// TODO may want to set the types of these, e.g. password, number etc.
			String value = this.selectedSettings.getPropertyValue(key);
			Object field;
			if (key.equals(PASSWORD_PROPERTY_KEY)) {
				field = this.ui.createPasswordfield(key, value);
			} else {
				field = this.ui.createTextfield(key, value);
			}
			
			if (this.selectedSettings.getFilePath().equals(this.originalValues.get(SECTION_ITEM_DATABASE_CONFIG_PATH))) {
				if (this.originalValues.get(SECTION_ITEM_DATABASE_CONFIG + key) == null) {
					// Let's save those settings for the first time
					this.originalValues.put(SECTION_ITEM_DATABASE_CONFIG + key, value);
				} else {
					/**
					 *  The original config path has been reselected.
					 *	The fields are crecreated, so potential previous changes have been lost
					 *	Let's call the fields "unchanged" for the {@link FrontlineSettingsHandler}
				 	**/
					this.settingChanged(SECTION_ITEM_DATABASE_CONFIG + key, value);
				}
			}
			
			this.ui.setAttachedObject(field, key);
			this.ui.setAction(field, "configFieldChanged(this)", null, this);
			this.ui.add(settingsPanel, field);
		}
	}
	
	/**
	 * A new database type has been selected in the ComboBox
	 */
	public void configFileChanged() {
		String selected = this.ui.getText(this.ui.getSelectedItem(find(COMPONENT_SETTINGS_SELECTION)));
		int selectedIndex = this.ui.getSelectedIndex(find(COMPONENT_SETTINGS_SELECTION));

		if (selected != null) {
			this.selectedSettings = DatabaseSettings.getSettings().get(selectedIndex);
			this.refreshSettingsPanel();
			
			super.settingChanged(SECTION_ITEM_DATABASE_CONFIG_PATH, selectedSettings.getFilePath());
		}
	}
	
	/**
	 * A database configuration field has been modified
	 * @param databaseConfigField The component having been modified.
	 */
	public void configFieldChanged(Object databaseConfigField) {
		if (selectedSettings.getFilePath().equals(this.originalValues.get(SECTION_ITEM_DATABASE_CONFIG_PATH))) {
			this.settingChanged(SECTION_ITEM_DATABASE_CONFIG + this.ui.getAttachedObject(databaseConfigField, String.class), this.ui.getText(databaseConfigField));
		}
	}

	public void save() {
		// get the settings we are modifying
		DatabaseSettings selectedSettings = this.selectedSettings;
		
		// check if the settings file has changed
		AppProperties appProperties = AppProperties.getInstance();
		boolean settingsFileChanged = !selectedSettings.getFilePath().equals(appProperties.getDatabaseConfigPath());
		
		// If settings file has NOT changed, check if individual settings have changed
		boolean updateIndividualSettings = false;
		List<Setting> displayedSettings = getDisplayedSettingValues();
		// We are modifying the current settings rather than changing to a whole new database config, so check if the
		// settings have changed at all
		for(Setting displayed : displayedSettings) {
			String originalValue = this.selectedSettings.getPropertyValue(displayed.getKey());
			if(!originalValue.equals(displayed.getValue())) {
				updateIndividualSettings = true;
				break;
			}
		}
		
		if(settingsFileChanged || updateIndividualSettings) {
			if(settingsFileChanged) {
				appProperties.setDatabaseConfigPath(selectedSettings.getFilePath());
				appProperties.saveToDisk();
			}
			
			if(updateIndividualSettings) {
				for(Setting displayed : displayedSettings) {
					this.selectedSettings.setPropertyValue(displayed.getKey(), displayed.getValue());
				}
				this.selectedSettings.saveProperties();
			}
			
			this.ui.alert(InternationalisationUtils.getI18nString(I18N_MESSAGE_DATABASE_SETTINGS_CHANGED));
		}
	}
	
	/** @return the settings and values that are currently displayed on the UI */
	private List<Setting> getDisplayedSettingValues() {
		Object settingsPanel = find(COMPONENT_PN_DATABASE_SETTINGS);
		Object[] settingsComponents = this.ui.getItems(settingsPanel);
		
		ArrayList<Setting> settings = new ArrayList<Setting>();
		for (int settingIndex=1; settingIndex<settingsComponents.length; settingIndex+=2) {
			// This code assumes that all settings are in TEXTFIELDS; this may change in the future.
			Object tf = settingsComponents[settingIndex];
			String key = this.ui.getName(tf);
			String value = this.ui.getText(tf);
			settings.add(new Setting(key, value));
		}
		
		return settings;
	}
	
	/**
	 * Show this panel as a dialog.  The dialog will be removed by default by the removeDialog method.
	 * @param titleI18nKey
	 */
	public void showAsDialog() {
		Object dialogComponent = this.ui.createDialog("Pwals");
		this.panel = this.ui.loadComponentFromFile(UI_SECTION_DATABASE_AS_DIALOG, this);
		this.init();
		
		this.ui.add(dialogComponent, panel);
		this.ui.setCloseAction(dialogComponent, "removeDialog", dialogComponent, this);
		this.ui.add(dialogComponent);
		this.dialogComponent = dialogComponent;
	}
	
	public void removeDialog() {
		this.ui.remove(this.dialogComponent);
	}

	public List<FrontlineValidationMessage> validateFields() {
		return null;
	}
	
	public String getTitle() {
		return InternationalisationUtils.getI18nString(I18N_SETTINGS_MENU_DATABASE_SETTINGS);
	}
	
	public Object getSectionNode() {
		return createSectionNode(InternationalisationUtils.getI18nString(I18N_SETTINGS_MENU_DATABASE_SETTINGS), this, "/icons/database_edit.png");
	}
}

class Setting {
	private final String key;
	private final String value;
	
	public Setting(String key, String value) {
		this.key = key;
		this.value = value;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getValue() {
		return value;
	}
}