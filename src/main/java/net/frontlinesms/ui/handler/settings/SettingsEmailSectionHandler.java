package net.frontlinesms.ui.handler.settings;

import java.util.List;

import net.frontlinesms.settings.FrontlineValidationMessage;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

/**
 * UI Handler for the "General/Email" section of the Core Settings
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public class SettingsEmailSectionHandler extends SettingsAbstractEmailsSectionHandler {
	private static final String UI_FILE_EMAIL_ACCOUNTS_SETTINGS_PANEL = "/ui/core/settings/general/pnEmailSettings.xml";
	private static final String UI_COMPONENT_PN_EMAIL_ACCOUNTS = "pnEmailAccounts";

	private static final String I18N_SETTINGS_MENU_EMAIL_SETTINGS = "menuitem.email.settings";

	public SettingsEmailSectionHandler (UiGeneratorController ui) {
		super(ui, false);
	}
	
	protected void init() {
		this.panel = this.ui.loadComponentFromFile(UI_FILE_EMAIL_ACCOUNTS_SETTINGS_PANEL, this);

		this.ui.add(find(UI_COMPONENT_PN_EMAIL_ACCOUNTS), super.getAccountsListPanel());
	}
	
	public void deinit() {}

	public void save() {	}

	public List<FrontlineValidationMessage> validateFields() {
		return null;
	}
	
	public String getTitle() {
		return InternationalisationUtils.getI18nString(I18N_SETTINGS_MENU_EMAIL_SETTINGS);
	}
	
	public Object getSectionNode() {
		return createSectionNode(InternationalisationUtils.getI18nString(I18N_SETTINGS_MENU_EMAIL_SETTINGS), this, "/icons/emailAccount_edit.png");
	}
}