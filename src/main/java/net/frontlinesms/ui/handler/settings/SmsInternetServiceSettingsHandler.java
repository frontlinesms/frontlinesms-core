package net.frontlinesms.ui.handler.settings;

import java.util.Collection;

import net.frontlinesms.data.repository.ConfigurableServiceSettingsDao;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;
import net.frontlinesms.messaging.sms.internet.SmsInternetServiceLoader;
import net.frontlinesms.serviceconfig.ui.BaseServiceSettingsHandler;
import net.frontlinesms.ui.UiGeneratorController;

/**
 * Ui Handler for {@link SmsInternetServiceSettingsHandler} settings.
 * @author Alex Anderson, Carlos Eduardo Genz
 */
public class SmsInternetServiceSettingsHandler extends BaseServiceSettingsHandler<SmsInternetService> {
	public SmsInternetServiceSettingsHandler(UiGeneratorController controller) {
		super(controller, new SmsInternetServiceLoader().getAll(),
				controller.getFrontlineController().getSmsInternetServiceSettingsDao());
	}
	
	// TODO this should only need to provide the up-to-date list rather than actually
	// generate descriptions as well.
	public void refreshAccounts(Object accountList) {
		if (accountList != null) {
			controller.removeAll(accountList);
			Collection<SmsInternetService> smsInternetServices = controller.getSmsInternetServices();
			for (SmsInternetService service : smsInternetServices) {
				controller.add(accountList, controller.createListItem(getProviderName(service.getClass()) + " - " + service.getIdentifier(), service.getSettings()));
			}
		}
	}
	
	@Override
	public Class<SmsInternetService> getServiceSupertype() {
		return SmsInternetService.class;
	}
	
	public ConfigurableServiceSettingsDao<SmsInternetService> getServiceDao() {
		return controller.getSmsInternetServiceSettingsDao();
	}

	@Override
	public String getIconMapLocation() {
		return "sms.internet.icons";
	}
}