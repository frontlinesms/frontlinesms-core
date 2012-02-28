/**
 * 
 */
package net.frontlinesms.ui.handler.mms;

import thinlet.Thinlet;
import net.frontlinesms.AppProperties;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.handler.email.EmailAccountDialogHandler;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.i18n.TextResourceKeyOwner;

/**
 * @author aga
 *
 */
@TextResourceKeyOwner
public class MmsSettingsDialogHandler implements ThinletUiEventHandler {
//> UI LAYOUT FILES
	private static final String UI_FILE_MMS_SETTINGS_FORM = "/ui/core/mms/dgMmsSettings.xml";
	private static final String UI_FILE_EMAIL_ACCOUNTS_LIST_FORM = "/ui/core/email/pnAccountsList.xml";
	
	//> THINLET COMPONENT NAMES
	private static final String UI_COMPONENT_BT_SAVE_SETTINGS = "btSaveSettings";
	private static final String UI_COMPONENT_PN_ACCOUNTS_LIST = "pnAccountsList";
	private static final String UI_COMPONENT_TF_POLL_FREQUENCY = "tfPollFrequency";
	
	private static final String I18N_MMS_EMAIL_ACCOUNTS = "mms.email.accounts";
	private static final String I18N_SETTINGS_SAVED = "common.settings.saved";
	private static final String I18N_ERROR_INVALID_NUMBER = "common.error.invalid.number";
	
//> INSTANCE PROPERTIES
	
	private UiGeneratorController ui;
	private Object dialogComponent;
	
	
	public MmsSettingsDialogHandler(UiGeneratorController ui) {
		this.ui = ui;
	}
	
	public Object getDialog() {
		initDialog();
		return this.dialogComponent;
	}
	
	private void initDialog() {
		this.dialogComponent = ui.loadComponentFromFile(UI_FILE_MMS_SETTINGS_FORM, this);
		EmailAccountDialogHandler dialogHandler = new EmailAccountDialogHandler(ui, true);
		Object pnAccountsList = ui.loadComponentFromFile(UI_FILE_EMAIL_ACCOUNTS_LIST_FORM, dialogHandler);
		this.ui.add(this.dialogComponent, pnAccountsList, 0);
		
		this.populate();
		
		dialogHandler.setDialogComponent(dialogComponent);
		dialogHandler.refresh();
	}
	
	private void populate() {
		Object accountsList = find(UI_COMPONENT_PN_ACCOUNTS_LIST);
		this.ui.setBorder(accountsList, true);
		this.ui.setText(accountsList, InternationalisationUtils.getI18nString(I18N_MMS_EMAIL_ACCOUNTS));
		
		AppProperties appProperties = AppProperties.getInstance();
		this.ui.setText(find(UI_COMPONENT_TF_POLL_FREQUENCY), String.valueOf(appProperties.getMmsPollingFrequency() / 1000));
	}

	/**
	 * This method is called when the "Save" button in the "General Settings" panel is clicked
	 * and actually saves the settings.
	 */
	public void saveSettings () {
		String pollFrequency = this.ui.getText(find(UI_COMPONENT_TF_POLL_FREQUENCY));
		int frequency = 0;
		try {
			frequency = Integer.parseInt(pollFrequency);
		} catch (NumberFormatException e) {
			frequency = 0;
		}
		
		if (frequency < 1) {
			this.ui.alert(InternationalisationUtils.getI18nString(I18N_ERROR_INVALID_NUMBER));
			return;
		}
		
		AppProperties appProperties = AppProperties.getInstance();
		appProperties.setMmsPollingFrequency(frequency * 1000);
		appProperties.saveToDisk();
		
		this.ui.infoMessage(InternationalisationUtils.getI18nString(I18N_SETTINGS_SAVED));
	}
	
	public void checkSettingsFields () {
		String pollFrequency = this.ui.getText(find(UI_COMPONENT_TF_POLL_FREQUENCY));
		
		boolean shouldEnableSaveButton = (pollFrequency.length() > 0);
		
		this.ui.setEnabled(find(UI_COMPONENT_BT_SAVE_SETTINGS), shouldEnableSaveButton);
	}


//> UI EVENT METHODS
		
	/** @see UiGeneratorController#removeDialog(Object) */
	public void removeDialog(Object dialog) {
		this.ui.removeDialog(dialog);
	}
	
//> UI HELPER METHODS
	/**
	 * Find a UI component within the {@link #dialogComponent}.
	 * @param componentName the name of the UI component
	 * @return the ui component, or <code>null</code> if it could not be found
	 */
	private Object find(String componentName) {
		return Thinlet.find(this.dialogComponent, componentName);
	}
}
