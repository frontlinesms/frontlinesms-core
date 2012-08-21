/**
 * 
 */
package net.frontlinesms.ui.handler.core;

import java.util.ArrayList;
import java.util.List;

import net.frontlinesms.AppProperties;
import net.frontlinesms.ui.DatabaseSettings;
import net.frontlinesms.ui.FrontlineUI;
import net.frontlinesms.ui.handler.BasePanelHandler;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.i18n.TextResourceKeyOwner;

/**
 * Thinlet UI Component event handler for displaying and modifying database settings.
 * @author aga
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
@TextResourceKeyOwner
public class DatabaseSettingsPanel extends BasePanelHandler implements DatabaseSettingsChangedCallbackListener {

//> STATIC CONSTANTS
	/** XML UI Layout File path: database settings panel */
	private static final String XML_SETTINGS_PANEL = "/ui/core/database/pnSettings.xml";
	/** i18n Text Key: "Database Config" - used as title for the dialog, and menu text to launch the dialog */
	private static final String I18N_KEY_DATABASE_CONFIG = "common.database.config";
	
	private static final String COMPONENT_SETTINGS_SELECTION = "cbConfigFile";
	/** The panel containing individual settings controls */
	private static final String COMPONENT_SETTINGS_PANEL = "pnSettings";
	/** UI Component: cancel button */
	private static final String COMPONENT_CANCEL_BUTTON = "btCancel";
	/** The constant property key for database passwords */
	private static final String PASSWORD_PROPERTY_KEY = "password";

//> INSTANCE PROPERTIES
	/** The settings currently selected in the combobox */
	private DatabaseSettings selectedSettings;
	/** A boolean saying whether or not the application must restart after the changes */
	private boolean needToRestartApplication;
	
	/** Dialog UI Component.  This should only be used if {@link #showAsDialog()} is called, and then should only be used by {@link #removeDialog()}. */
	private Object dialogComponent;
	
	/** Callback listener to take action if the database settings have changed. */
	private DatabaseSettingsChangedCallbackListener settingsChangedCallbackListener;
	
//> CONSTRUCTORS
	private DatabaseSettingsPanel(FrontlineUI ui) {
		super(ui);
	}
	
	/**
	 * Initialise the UI.
	 * @param restartRequired
	 */
	private void init(String selectedPath) {
		super.loadPanel(XML_SETTINGS_PANEL);
		
		// Populate combobox
		String selectedDatabaseConfigPath = (selectedPath == null ? AppProperties.getInstance().getDatabaseConfigFilename() : selectedPath);
		List<DatabaseSettings> databaseSettings = DatabaseSettings.getSettings();
		Object settingsSelection = getConfigFileSelecter();
		for(int settingsIndex = 0; settingsIndex < databaseSettings.size(); ++settingsIndex) {
			DatabaseSettings settings = databaseSettings.get(settingsIndex);
			Object comboBox = createComboBox(settings);
			ui.add(settingsSelection, comboBox);

			// if appropriate, choose the combobox selection
			if(settings.getFilePath().equals(selectedDatabaseConfigPath)) {
				this.selectedSettings = settings;
				ui.setSelectedIndex(settingsSelection, settingsIndex);
			}
		}
		
		// populate settings panel
		refreshSettingsPanel();
	}
	
//> ACCESSORS
	@Override
	public Object getPanelComponent() {
		return super.getPanelComponent();
	}

	/**
	 * Sets the saveMethod
	 * @param methodCall
	 * @param eventHandler
	 */
	public void setSettingsChangedCallbackListener(DatabaseSettingsChangedCallbackListener callbackListener) {
		this.settingsChangedCallbackListener = callbackListener;
	}
	
	/** @param enabled <code>true</code> if the cancel button should be enabled, <code>false</code> if it should be disabled */
	public void setCancelEnabled(boolean enabled) {
		ui.setEnabled(find(COMPONENT_CANCEL_BUTTON), enabled);
	}

//> INSTANCE HELPER METHODS
	
//> UI HELPER METHODS
	/** Refresh the panel containing settings specific to the currently-selected {@link DatabaseSettings}. */
	private void refreshSettingsPanel() {
		// populate the settings panel
		Object settingsPanel = super.find(COMPONENT_SETTINGS_PANEL);
		ui.removeAll(settingsPanel);
		
		this.selectedSettings.loadProperties();
		for(String key : this.selectedSettings.getPropertyKeys()) {
			// TODO would be nice to set icons for the different settings
			ui.add(settingsPanel, ui.createLabel(key));
			// TODO may want to set the types of these, e.g. password, number etc.
			if (key.equals(PASSWORD_PROPERTY_KEY))
				ui.add(settingsPanel, ui.createPasswordfield(key, this.selectedSettings.getPropertyValue(key)));
			else
				ui.add(settingsPanel, ui.createTextfield(key, this.selectedSettings.getPropertyValue(key)));
		}
	}

	private Object getConfigFileSelecter() {
		return super.find(COMPONENT_SETTINGS_SELECTION);
	}
	
	private Object createComboBox(DatabaseSettings settings) {
		Object cb = ui.createComboboxChoice(settings.getName(), settings);
		// TODO perhaps we could set a settings-specific icon here
		return cb;
	}
	
	/** @return the settings and values that are currently displayed on the UI */
	private List<Setting> getDisplayedSettingValues() {
		Object settingsPanel = super.find(COMPONENT_SETTINGS_PANEL);
		Object[] settingsComponents = ui.getItems(settingsPanel);
		
		ArrayList<Setting> settings = new ArrayList<Setting>();
		for (int settingIndex=1; settingIndex<settingsComponents.length; settingIndex+=2) {
			// This code assumes that all settings are in TEXTFIELDS; this may change in the future.
			Object tf = settingsComponents[settingIndex];
			String key = ui.getName(tf);
			String value = ui.getText(tf);
			settings.add(new Setting(key, value));
		}
		
		return settings;
	}
	
//> UI EVENT METHODS
	public void configFileChanged() {
		String selected = ui.getText(ui.getSelectedItem(getConfigFileSelecter()));
		
		if (selected != null) {
			this.openNewDialog(selected);
			this.removeDialog();
		}
	}
	
	/**
	 * Save button pressed: Saves the database settings and restarts FrontlineSMS to use
	 * the new settings.
	 */
	public void save() {
		// get the settings we are modifying
		DatabaseSettings selectedSettings = this.selectedSettings;
		
		// check if the settings file has changed
		AppProperties appProperties = AppProperties.getInstance();
		boolean settingsFileChanged = !selectedSettings.getFilePath().equals(appProperties.getDatabaseConfigFilename());
		
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
		
		if(!settingsFileChanged && !updateIndividualSettings) {
			// Nothing has changed, so no need to update anything
			ui.alert("You did not make any changes to the settings."); // FIXME i18n
			removeDialog();
		} else {
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
			
			this.settingsChangedCallbackListener.handleDatabaseSettingsChanged();
		}
	}

	/** Cancel button pressed. */
	public void cancel() {
		// Currently this method should only function properly if we are displaying
		// the settings panel in a dialog.
		assert(this.dialogComponent != null) : "Currently the cancel button should only be enabled if we are displaying as a dialog.";
		
		// If we are displaying the settings panel as a dialog, remove it.
		removeDialog();
	}
	
	/** This should be called if we are showing the settings panel in a dialog.  The dialog should now be removed. */
	public void handleDatabaseSettingsChanged() {
		// Display alert warning the user that their settings have changed, and they
		// must restart FrontlineSMS for the changes to take effect.  In a perfect world,
		// we would AUTOMATICALLY restart here, but this is not quite trivial, so is not
		// implemented at this stage.
		if (needToRestartApplication)
			ui.alert("Settings saved.  Please restart FrontlineSMS immediately."); // FIXME i18n
		
		removeDialog();	
	}

	/**
	 * Show this panel as a dialog.  The dialog will be removed by default by the removeDialog method.
	 * @param titleI18nKey
	 */
	public void showAsDialog(boolean needToRestartApplication) {
		this.settingsChangedCallbackListener = this;
		this.needToRestartApplication = needToRestartApplication;
		
		Object dialogComponent = ui.createDialog(InternationalisationUtils.getI18nString(I18N_KEY_DATABASE_CONFIG));
		ui.add(dialogComponent, this.getPanelComponent());
		ui.setCloseAction(dialogComponent, "removeDialog", dialogComponent, this);
		ui.add(dialogComponent);
		this.dialogComponent = dialogComponent;
	}
	
//> UI EVENT METHODS
	public void removeDialog() {
		this.ui.removeDialog(this.dialogComponent);
	}
	
//> STATIC FACTORIES
	public static DatabaseSettingsPanel createNew(FrontlineUI ui, String selectedPath) {
		DatabaseSettingsPanel panel = new DatabaseSettingsPanel(ui);
		panel.init(selectedPath);
		return panel;
	}
	
	public void openNewDialog(String selectedPath) {
		DatabaseSettingsPanel databaseSettings = DatabaseSettingsPanel.createNew(ui, selectedPath);
		databaseSettings.showAsDialog(needToRestartApplication);
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