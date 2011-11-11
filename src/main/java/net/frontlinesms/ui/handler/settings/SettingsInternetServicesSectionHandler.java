package net.frontlinesms.ui.handler.settings;

import java.util.Collection;
import java.util.List;

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
		this.panel = ui.loadComponentFromFile(UI_SECTION_INTERNET_SERVICES, this);

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
			this.ui.removeAll(accountList);
			Collection<SmsInternetService> smsInternetServices = this.ui.getSmsInternetServices();
			for (SmsInternetService service : smsInternetServices) {
				this.ui.add(accountList, this.ui.createListItem(SmsInternetServiceSettingsHandler.getProviderName(service.getClass()) + " - " + service.getIdentifier(), service));
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
		for (Object item : this.ui.getItems(panel)) {
			String name = this.ui.getName(item); 
			if (!"btNew".equals(name)
					&& !"btCancel".equals(name)) {
				this.ui.setEnabled(item, this.ui.getSelectedItem(list) != null);
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
		SmsInternetServiceSettingsHandler internetServiceSettingsHandler = new SmsInternetServiceSettingsHandler(this.ui);
		internetServiceSettingsHandler.showNewServiceWizard();
	}
	
	/** Confirms deletes of {@link SmsInternetService}(s) from the system and removes them from the list of services */
	public void removeServices() {
		this.ui.removeConfirmationDialog();
		removeServices(find(UI_COMPONENT_LS_ACCOUNTS));
	}
	
	/**
	 * Delete the selected services from the system and remove them from the list.
	 * @param lsProviders
	 */
	private void removeServices(Object lsProviders) {
		Object[] obj = this.ui.getSelectedItems(lsProviders);
		for (Object object : obj) {
			SmsInternetService service = (SmsInternetService) this.ui.getAttachedObject(object);
			this.eventBus.notifyObservers(new InternetServiceEventNotification(InternetServiceEventNotification.EventType.DELETE, service));
			ui.getSmsInternetServiceSettingsDao().deleteServiceSettings(service.getSettings());
			this.ui.remove(object);
		}
		selectionChanged(lsProviders, find("pnButtons"));
	}
	
	/**
	 * Configure a provider given its UI component.
	 * @param lsProviders
	 */
	public void configureService(Object lsProviders) {
		Object serviceComponent = this.ui.getSelectedItem(lsProviders);
		SmsInternetServiceSettingsHandler internetServiceSettingsHandler = new SmsInternetServiceSettingsHandler(this.ui);
		internetServiceSettingsHandler.showConfigureService((SmsInternetService)this.ui.getAttachedObject(serviceComponent), null);
	}
	
	
	public void showConfirmationDialog(String methodToBeCalled) {
		this.ui.showConfirmationDialog(methodToBeCalled, this);
	}

	public void notify(FrontlineEventNotification notification) {
		if (notification instanceof InternetServiceEventNotification) {
			this.refresh();
		} else if (notification instanceof UiDestroyEvent) {
			if(((UiDestroyEvent) notification).isFor(this.ui)) {
				this.ui.getFrontlineController().getEventBus().unregisterObserver(this);
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