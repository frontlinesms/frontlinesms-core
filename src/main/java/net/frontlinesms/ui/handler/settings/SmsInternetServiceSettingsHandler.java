package net.frontlinesms.ui.handler.settings;

import java.util.List;

import net.frontlinesms.data.repository.ConfigurableServiceSettingsDao;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;
import net.frontlinesms.messaging.sms.internet.SmsInternetServiceLoader;
import net.frontlinesms.serviceconfig.ui.BaseServiceSettingsHandler;
import net.frontlinesms.settings.FrontlineValidationMessage;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.settings.UiSettingsSectionHandler;

/**
 * Ui Handler for {@link SmsInternetServiceSettingsHandler} settings.
 * @author Alex Anderson, Carlos Eduardo Genz
 */
public class SmsInternetServiceSettingsHandler extends BaseServiceSettingsHandler<SmsInternetService>
		implements UiSettingsSectionHandler {
	private static final String I18N_SETTINGS_MENU_INTERNET_SERVICES = "settings.menu.internet.services";
	
	public SmsInternetServiceSettingsHandler(UiGeneratorController controller) {
		super(controller, controller.getFrontlineController().getSmsInternetServiceSettingsDao(),
				new SmsInternetServiceLoader().getAll(),
				InternationalisationUtils.getI18nString(I18N_SETTINGS_MENU_INTERNET_SERVICES), "/icons/sms_http_edit.png");
	}
	
	@Override
	public Class<SmsInternetService> getServiceSupertype() {
		return SmsInternetService.class;
	}
	
	public ConfigurableServiceSettingsDao<SmsInternetService> getServiceDao() {
		return ui.getSmsInternetServiceSettingsDao();
	}

	@Override
	public String getIconMapLocation() {
		return "sms.internet.icons";
	}

	public void save() {
	}

	public List<FrontlineValidationMessage> validateFields() {
		return null;
	}

	public String getTitle() {
		// TODO Auto-generated method stub
		return null;
	}
}