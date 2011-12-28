package net.frontlinesms.ui.handler.settings;

import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

public class SettingsServicesSectionHandler extends SettingsEmptySectionHandler {
	private static final String I18N_SETTINGS_MENU_SERVICES = "settings.menu.services";

	public SettingsServicesSectionHandler(UiGeneratorController ui) {
		super(ui, I18N_SETTINGS_MENU_SERVICES);
	}
	
	public Object getSectionNode() {
		Object servicesRootNode = createSectionNode(InternationalisationUtils.getI18nString(I18N_SETTINGS_MENU_SERVICES), this, "/icons/database_execute.png");

		SettingsDevicesSectionHandler devicesHandler = new SettingsDevicesSectionHandler(ui);
		ui.add(servicesRootNode, devicesHandler.getSectionNode());
		
		SmsInternetServiceSettingsHandler internetServicesHandler = new SmsInternetServiceSettingsHandler(ui);
		ui.add(servicesRootNode, internetServicesHandler.getSectionNode());
		
		SettingsMmsSectionHandler mmsHandler = new SettingsMmsSectionHandler(ui);
		ui.add(servicesRootNode, mmsHandler.getSectionNode());
		
		return servicesRootNode;
	}
}