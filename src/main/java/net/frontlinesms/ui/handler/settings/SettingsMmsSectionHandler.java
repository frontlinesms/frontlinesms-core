package net.frontlinesms.ui.handler.settings;

import java.util.ArrayList;
import java.util.List;

import net.frontlinesms.AppProperties;
import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.settings.FrontlineValidationMessage;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.settings.UiSettingsSectionHandler;

/**
 * UI Handler for the "General/MMS" section of the Core Settings
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public class SettingsMmsSectionHandler extends SettingsAbstractEmailsSectionHandler {
	private static final String UI_FILE_EMAIL_ACCOUNTS_SETTINGS_PANEL = "/ui/core/settings/services/pnMmsSettings.xml";
	private static final String UI_COMPONENT_PN_EMAIL_ACCOUNTS = "pnEmailAccounts";
	private static final String UI_COMPONENT_TF_POLLING_FREQUENCY = "tfPollFrequency";
	
	private static final String SECTION_ICON = "/icons/mms.png";
	private static final String SECTION_ITEM_POLLING_FREQUENCY = "SERVICES_MMS_POLLING_FREQUENCY";
	
	private static final String I18N_INVALID_POLLING_FREQUENCY = "settings.message.mms.invalid.polling.frequency";
	private static final String I18N_SETTINGS_MENU_MMS = "settings.menu.mms";
	
	public SettingsMmsSectionHandler (UiGeneratorController ui) {
		super(ui, true);
	}
	
	protected void init() {
		this.panel = this.ui.loadComponentFromFile(UI_FILE_EMAIL_ACCOUNTS_SETTINGS_PANEL, this);

		this.ui.add(find(UI_COMPONENT_PN_EMAIL_ACCOUNTS), super.getAccountsListPanel());
		this.populateMmsSettings();
	}
	
	public void deinit() {}
	
	private void populateMmsSettings() {
		AppProperties appProperties = AppProperties.getInstance();
		
		String pollingFrequency = String.valueOf(appProperties.getMmsPollingFrequency() / 1000);
		this.ui.setText(find(UI_COMPONENT_TF_POLLING_FREQUENCY), pollingFrequency);
		
		this.originalValues.put(SECTION_ITEM_POLLING_FREQUENCY, pollingFrequency);
	}

	public void pollFrequencyChanged (String frequency) {
		super.settingChanged(SECTION_ITEM_POLLING_FREQUENCY, frequency);
	}
	
	public void save() {
		AppProperties appProperties = AppProperties.getInstance();
		
		int frequency;
		try {
			frequency = Integer.parseInt(this.ui.getText(find(UI_COMPONENT_TF_POLLING_FREQUENCY)));
		} catch (NumberFormatException e) {
			// Should never happen
			frequency = FrontlineSMSConstants.DEFAULT_MMS_POLLING_FREQUENCY;
		}
		
		appProperties.setMmsPollingFrequency(frequency * 1000);
		appProperties.saveToDisk();
	}

	/**
	 * @see UiSettingsSectionHandler#validateFields()
	 */
	public List<FrontlineValidationMessage> validateFields() {
		List<FrontlineValidationMessage> validationMessages = new ArrayList<FrontlineValidationMessage>();

		String pollFrequency = this.ui.getText(find(UI_COMPONENT_TF_POLLING_FREQUENCY));
		
		try {
			if (pollFrequency == null || Integer.parseInt(pollFrequency) <= 0) {
				validationMessages.add(new FrontlineValidationMessage(I18N_INVALID_POLLING_FREQUENCY, null, getIcon()));
			}
		} catch (NumberFormatException e) {
			validationMessages.add(new FrontlineValidationMessage(I18N_INVALID_POLLING_FREQUENCY, null, getIcon()));
		}
		
		return validationMessages;
	}
	
	public String getTitle() {
		return InternationalisationUtils.getI18nString(I18N_SETTINGS_MENU_MMS);
	}
	
	private String getIcon() {
		return SECTION_ICON;
	}
//> UI EVENT METHODS

	public Object getSectionNode() {
		return createSectionNode(InternationalisationUtils.getI18nString(I18N_SETTINGS_MENU_MMS), this, getIcon());
	}
}