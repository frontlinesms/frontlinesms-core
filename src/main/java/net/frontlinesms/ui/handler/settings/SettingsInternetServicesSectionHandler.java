package net.frontlinesms.ui.handler.settings;

import java.util.Collection;
import java.util.List;

import net.frontlinesms.data.repository.ConfigurableServiceSettingsDao;
import net.frontlinesms.events.EventObserver;
import net.frontlinesms.events.FrontlineEventNotification;
import net.frontlinesms.messaging.sms.events.InternetServiceEventNotification;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;
import net.frontlinesms.settings.BaseSectionHandler;
import net.frontlinesms.settings.FrontlineValidationMessage;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiDestroyEvent;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.events.FrontlineUiUpateJob;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.settings.UiSettingsSectionHandler;

public class SettingsInternetServicesSectionHandler extends BaseSectionHandler implements UiSettingsSectionHandler, ThinletUiEventHandler, EventObserver {
	private static final String UI_SECTION_INTERNET_SERVICES = "/ui/core/settings/services/pnInternetServicesSettings.xml";

	private static final String UI_COMPONENT_LS_ACCOUNTS = "lsSmsInternetServices";
	private static final String UI_COMPONENT_PN_BUTTONS = "pnButtons";

	private static final String I18N_SETTINGS_MENU_INTERNET_SERVICES = "settings.menu.internet.services";

	public SettingsInternetServicesSectionHandler (UiGeneratorController ui) {
		super(ui);

		this.eventBus.registerObserver(this);
	}
	
	protected void init() {
		this.panel = uiController.loadComponentFromFile(UI_SECTION_INTERNET_SERVICES, this);

		// Update the list of accounts from the list provided
		Object accountList = find(UI_COMPONENT_LS_ACCOUNTS);
		this.refresh();
		
		selectionChanged(accountList, find(UI_COMPONENT_PN_BUTTONS));
	}

	private void refresh() {
		new FrontlineUiUpateJob() {
			public void run() {
				refreshAccounts();
			}
		}.execute();
	}
	
	public void refreshAccounts() {
		Object accountList = find(UI_COMPONENT_LS_ACCOUNTS);
		if (accountList != null) {
			this.uiController.removeAll(accountList);
			Collection<SmsInternetService> smsInternetServices = this.uiController.getSmsInternetServices();
			for (SmsInternetService service : smsInternetServices) {
				this.uiController.add(accountList, this.uiController.createListItem(SmsInternetServiceSettingsHandler.getProviderName(service.getClass()) + " - " + service.getIdentifier(), service));
			}
		}
		
		this.selectionChanged(accountList, find("pnButtons"));
	}
	
	/**
	 * Enables/Disables fields from panel, according to list selection.
	 * @param list
	 * @param panel
	 */
	public void selectionChanged(Object list, Object panel) {
		for (Object item : this.uiController.getItems(panel)) {
			String name = this.uiController.getName(item); 
			if (!"btNew".equals(name)
					&& !"btCancel".equals(name)) {
				this.uiController.setEnabled(item, this.uiController.getSelectedItem(list) != null);
			}
		}
	}
	
	public void save() {
		
	}
	
	public List<FrontlineValidationMessage> validateFields() {
		return null;
	}

	/** Show the wizard for creating a new service. */
	public void showNewServiceWizard() {
		SmsInternetServiceSettingsHandler internetServiceSettingsHandler = new SmsInternetServiceSettingsHandler(this.uiController);
		internetServiceSettingsHandler.showNewServiceWizard();
	}
	
	/** Confirms deletes of {@link SmsInternetService}(s) from the system and removes them from the list of services */
	public void removeServices() {
		this.uiController.removeConfirmationDialog();
		removeServices(find(UI_COMPONENT_LS_ACCOUNTS));
	}
	
	/**
	 * Delete the selected services from the system and remove them from the list.
	 * @param lsProviders
	 */
	private void removeServices(Object lsProviders) {
		Object[] obj = this.uiController.getSelectedItems(lsProviders);
		for (Object object : obj) {
			SmsInternetService service = (SmsInternetService) this.uiController.getAttachedObject(object);
			this.eventBus.notifyObservers(new InternetServiceEventNotification(InternetServiceEventNotification.EventType.DELETE, service));
			uiController.getSmsInternetServiceSettingsDao().deleteServiceSettings(service.getSettings());
			this.uiController.remove(object);
		}
		selectionChanged(lsProviders, find("pnButtons"));
	}
	
	/**
	 * Configure a provider given its UI component.
	 * @param lsProviders
	 */
	public void configureService(Object lsProviders) {
		Object serviceComponent = this.uiController.getSelectedItem(lsProviders);
		SmsInternetServiceSettingsHandler internetServiceSettingsHandler = new SmsInternetServiceSettingsHandler(this.uiController);
		internetServiceSettingsHandler.showConfigureService((SmsInternetService)this.uiController.getAttachedObject(serviceComponent), null);
	}
	
	
	public void showConfirmationDialog(String methodToBeCalled) {
		this.uiController.showConfirmationDialog(methodToBeCalled, this);
	}

	public void notify(FrontlineEventNotification notification) {
		if (notification instanceof InternetServiceEventNotification) {
			this.refresh();
		} else if (notification instanceof UiDestroyEvent) {
			if(((UiDestroyEvent) notification).isFor(this.uiController)) {
				this.uiController.getFrontlineController().getEventBus().unregisterObserver(this);
			}
		}
	}
	
	public String getTitle() {
		return InternationalisationUtils.getI18nString(I18N_SETTINGS_MENU_INTERNET_SERVICES);
	}

	public Object getSectionNode() {
		return createSectionNode(InternationalisationUtils.getI18nString(I18N_SETTINGS_MENU_INTERNET_SERVICES), this, "/icons/sms_http_edit.png");
	}
}