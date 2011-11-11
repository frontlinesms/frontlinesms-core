package net.frontlinesms.ui.handler.settings;

import java.util.Collection;

import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.data.repository.ConfigurableServiceSettingsDao;
import net.frontlinesms.events.FrontlineEventNotification;
import net.frontlinesms.messaging.sms.events.InternetServiceEventNotification;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;
import net.frontlinesms.messaging.sms.internet.SmsInternetServiceLoader;
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
				controller.add(accountList, controller.createListItem(getProviderName(service.getClass()) + " - " + service.getIdentifier(), service));
			}
		}
	}
	
	public ConfigurableServiceSettingsDao getServiceDao() {
		return controller.getSmsInternetServiceSettingsDao();
	}

	@Override
	public FrontlineEventNotification createDeletedNotification(SmsInternetService service) {
		return new InternetServiceEventNotification(InternetServiceEventNotification.EventType.DELETE, service);
	}

	@Override
	public FrontlineEventNotification createSavedNotification(SmsInternetService service) {
		return new InternetServiceEventNotification(InternetServiceEventNotification.EventType.ADD, service);
	}

	@Override
	public String getIconMapLocation() {
		return "sms.internet.icons";
	}
}