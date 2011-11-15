/**
 * 
 */
package net.frontlinesms.ui.handler.phones;

import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_MODEM_LIST_UPDATED;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.TAB_ADVANCED_PHONE_MANAGER;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArraySet;

import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.data.domain.EmailAccount;
import net.frontlinesms.data.domain.PersistableSettings;
import net.frontlinesms.data.domain.SmsModemSettings;
import net.frontlinesms.data.events.DatabaseEntityNotification;
import net.frontlinesms.data.repository.SmsModemSettingsDao;
import net.frontlinesms.events.FrontlineEventNotification;
import net.frontlinesms.messaging.FrontlineMessagingService;
import net.frontlinesms.messaging.FrontlineMessagingServiceStatus;
import net.frontlinesms.messaging.mms.MmsService;
import net.frontlinesms.messaging.mms.MmsServiceManager;
import net.frontlinesms.messaging.mms.email.MmsEmailService;
import net.frontlinesms.messaging.mms.email.MmsEmailServiceStatus;
import net.frontlinesms.messaging.mms.events.MmsServiceStatusNotification;
import net.frontlinesms.messaging.sms.SmsService;
import net.frontlinesms.messaging.sms.SmsServiceManager;
import net.frontlinesms.messaging.sms.events.SmsInternetServiceStatusNotification;
import net.frontlinesms.messaging.sms.events.SmsModemStatusNotification;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;
import net.frontlinesms.messaging.sms.internet.SmsInternetServiceStatus;
import net.frontlinesms.messaging.sms.modem.SmsModem;
import net.frontlinesms.messaging.sms.modem.SmsModemStatus;
import net.frontlinesms.ui.HomeTabEventNotification;
import net.frontlinesms.ui.Icon;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.events.FrontlineUiUpdateJob;
import net.frontlinesms.ui.events.TabChangedNotification;
import net.frontlinesms.ui.handler.BaseTabHandler;
import net.frontlinesms.ui.handler.email.EmailAccountSettingsDialogHandler;
import net.frontlinesms.ui.handler.settings.SmsInternetServiceSettingsHandler;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.i18n.TextResourceKeyOwner;

import serial.NoSuchPortException;

/**
 * Event handler for the Phones tab and associated dialogs
 * @author Alex Anderson <alex@frontlinesms.com>
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
@TextResourceKeyOwner(prefix={"COMMON_", "I18N_", "MESSAGE_"})
public class PhoneTabHandler extends BaseTabHandler {
//> STATIC CONSTANTS
	/** {@link Comparator} used for sorting {@link FrontlineMessagingService}s into a friendly order. */
	private static final Comparator<? super FrontlineMessagingService> MESSAGING_SERVICE_COMPARATOR = new Comparator<FrontlineMessagingService>() {
			public int compare(FrontlineMessagingService one, FrontlineMessagingService tother) {
				int comparison = 0;
				
				// Always Modems first, then Internet Services, then MMS Services
				if (one instanceof SmsModem) {
					if (tother instanceof SmsModem) {
						comparison = ((SmsModem)one).getPort().compareTo(((SmsModem)tother).getPort());
					} else {
						comparison = -1;
					}
				} else if (one.getClass().equals(tother.getClass())) {
					comparison = one.getServiceName().compareTo(tother.getServiceName());
				} else if (one instanceof SmsInternetService) {
					comparison = (tother instanceof SmsModem ? 1 : -1);
				}
				
				return comparison;
			}};
	
//> THINLET UI LAYOUT FILES
	/** UI XML File Path: the Phones Tab itself */
	private static final String UI_FILE_PHONES_TAB = "/ui/core/phones/phonesTab.xml";
	
//> I18n TEXT KEYS
	/** I18n Text Key: TODO */
	private static final String COMMON_PHONE_CONNECTED = "common.phone.connected";
	/** I18n Text Key: TODO */
	private static final String COMMON_SMS_INTERNET_SERVICE_CONNECTED = "common.sms.internet.service.connected";
	/** I18n Text Key: Last checked: %0. */
	private static final String I18N_EMAIL_LAST_CHECKED = "email.last.checked";
	
//> THINLET UI COMPONENT NAMES
	/** UI Component name: TODO */
	private static final String COMPONENT_PHONE_MANAGER_MODEM_LIST = "phoneManager_modemList";
	/** UI Component name: TODO */
	private static final String COMPONENT_PHONE_MANAGER_MODEM_LIST_ERROR = "phoneManager_modemListError";

//> INSTANCE PROPERTIES
	/** The manager of {@link FrontlineMessagingService}s */
	private final SmsServiceManager phoneManager;
	/** Data Access Object for {@link SmsModemSettings}s */
	private final SmsModemSettingsDao smsModelSettingsDao;
	private final MmsServiceManager mmsServiceManager;
//> CONSTRUCTORS
	/**
	 * Create a new instance of this class.
	 * @param uiController value for {@link #ui}
	 */
	public PhoneTabHandler(UiGeneratorController ui) {
		super(ui, true);
		this.phoneManager = ui.getPhoneManager();
		this.mmsServiceManager = ui.getFrontlineController().getMmsServiceManager();
		this.smsModelSettingsDao = ui.getPhoneDetailsManager();
	}
	
	@Override
	protected Object initialiseTab() {
		// We register the observer to the UIGeneratorController, which notifies when tabs have changed
		return ui.loadComponentFromFile(UI_FILE_PHONES_TAB, this);
	}

//> ACCESSORS
	/** @return the compoenent containing the list of connected devices */
	private Object getModemListComponent() {
		return ui.find(this.getTab(), COMPONENT_PHONE_MANAGER_MODEM_LIST);
	}
	
//> THINLET UI METHODS
	
	/**
	 * Event fired when the view phone details action is fired. 
	 * @param list the thinlet UI list containing details of the devices
	 */
	public void showPhoneSettingsDialog(Object list) {
		Object selected = ui.getSelectedItem(list);
		if (selected != null) {
			FrontlineMessagingService service = ui.getAttachedObject(selected, FrontlineMessagingService.class);
			if (service instanceof SmsModem) {
				SmsModem modem = (SmsModem) service;
				if (modem.isConnected()) {
					showPhoneSettingsDialog(modem, false);
				} else {
					showPhoneConfigDialog(list);
				}
			} else if (service instanceof SmsInternetService) {
				SmsInternetServiceSettingsHandler serviceHandler = new SmsInternetServiceSettingsHandler(this.ui);
				serviceHandler.showConfigureService(((SmsInternetService) service).getSettings());
			} else if (service instanceof MmsEmailService) {
				EmailAccountSettingsDialogHandler emailAccountSettingsDialogHandler = new EmailAccountSettingsDialogHandler(ui, true);
				emailAccountSettingsDialogHandler.initDialog(((MmsEmailService) service).getEmailAccount());
				this.ui.add(emailAccountSettingsDialogHandler.getDialog());
			}
		}
	}
	
	/**
	 * Event fired when the view phone details action is chosen.
	 * @param device The device we are showing settings for
	 * @param isNewPhone <code>true</code> TODO <if this phone has previously connected (i.e. not first time it has connected)> OR <if this phone has just connected (e.g. may have connected before, but not today)> 
	 */
	public void showPhoneSettingsDialog(SmsModem device, boolean isNewPhone) {
		final DeviceSettingsDialogHandler deviceSettingsDialog = new DeviceSettingsDialogHandler(ui, device, isNewPhone);
		
		new FrontlineUiUpdateJob() {
			public void run() {
				deviceSettingsDialog.initDialog();
				ui.add(deviceSettingsDialog.getDialog());
			}
		}.execute();
	}
	
	/**
	 * Enables or disables the <b>Edit Phone Settings</b>. 
	 * <br>If the supplied list is not empty, then the option is enabled. Otherwise, it is disabled.
	 * TODO would be good to know when this event is triggered
	 * @param list
	 * @param menuItem
	 */
	public void editPhoneEnabled(Object list, Object menuItem) {
		ui.setVisible(menuItem, ui.getSelectedItem(list) != null);
	}

	/**
	 * Disconnect from a specific {@link FrontlineMessagingService}.
	 * @param list The list of connected {@link FrontlineMessagingService}s in the Phones tab.
	 */
	public void disconnectFromSelected(Object list) {
		Object selected = ui.getSelectedItem(list);
		
		if (selected != null) {
			FrontlineMessagingService service = ui.getAttachedObject(selected, FrontlineMessagingService.class);
			
			if (service instanceof SmsService) {
				phoneManager.disconnect((SmsService) service);
			} else if (service instanceof MmsEmailService) {
				this.mmsServiceManager.connectMmsEmailService((MmsEmailService) service, false);
			}
			
			refresh();
		}
	}
	
	/**
	 * Stop detection of the {@link FrontlineMessagingService} on a specific port.
	 * @param list The list of ports which are currently being probed for connected {@link FrontlineMessagingService}s.
	 */
	public void stopDetection(Object list) {
		FrontlineMessagingService dev = ui.getAttachedObject(ui.getSelectedItem(list), FrontlineMessagingService.class);
		if (dev instanceof SmsModem) {
			SmsModem modem = (SmsModem) dev;
			phoneManager.stopDetection(modem.getPort());
			
		}
		refresh();
	}
	
	/**
	 * Action triggered when an item in the unconnected phones/ports list is selected.  This method
	 * enables and disables relevant items in the contextual popupmenu for the selected port.
	 * @param popUp
	 * @param list
	 */
	public void phoneManager_enabledFields(Object popUp, Object list) {
		Object selected = ui.getSelectedItem(list);
		if (selected == null) {
			ui.setVisible(popUp, false);
		} else {
			FrontlineMessagingService service = ui.getAttachedObject(selected, FrontlineMessagingService.class);
			if (service instanceof SmsModem) {
				SmsModem modem = (SmsModem) service;
				ui.setVisible(ui.find(popUp, "miEditPhone"), false);
				ui.setVisible(ui.find(popUp, "miAutoConnect"), !modem.isDetecting() && !modem.isTryToConnect());
				ui.setVisible(ui.find(popUp, "miManualConnection"), !modem.isDetecting() && !modem.isTryToConnect());
				ui.setVisible(ui.find(popUp, "miCancelDetection"), modem.isDetecting());
			} else {
				for (Object o : ui.getItems(popUp)) {
					ui.setVisible(o, ui.getName(o).equals("miEditPhone") || ui.getName(o).equals("miAutoConnect"));
				}
			}
		}
	}
	
	/**
	 * Attempt to automatically connect to the service currently selected in the list of
	 * unconnected services.
	 */
	public void connectToSelectedPhoneHandler() {
		Object modemListError = ui.find(COMPONENT_PHONE_MANAGER_MODEM_LIST_ERROR);
		Object selected = ui.getSelectedItem(modemListError);
		
		if (selected != null) {
			FrontlineMessagingService service = ui.getAttachedObject(selected, FrontlineMessagingService.class);
			
			if (service instanceof SmsModem) {
				SmsModem modem = (SmsModem) service;
				try {
					phoneManager.requestConnect(modem.getPort());
				} catch (NoSuchPortException ex) {
					log.info("Unable to connect to requested modem/phone.", ex);
				}
			} else if (service instanceof SmsInternetService) {
				((SmsInternetService) service).restartService();
			} else if (service instanceof MmsEmailService) {
				this.mmsServiceManager.connectMmsEmailService((MmsEmailService) service, true);
			}
		}
	}
	
	/**
	 * Show the dialog for connecting a phone with manual configuration.
	 * @param list TODO what is this list, and why is it necessary?  Could surely just find it
	 * TODO FIXME XXX need to be much clearer about when this method should be available.
	 */
	public void showPhoneConfigDialog(Object list) {
		Object selected = ui.getSelectedItem(list);
		// This assumes that the attached FrontlineMessagingService is an instance of SmsModem 
		final SmsModem selectedModem = ui.getAttachedObject(selected, SmsModem.class);
		
		// We create the manual config dialog and put the display job in the AWT event queue
		final DeviceManualConfigDialogHandler configDialog = new DeviceManualConfigDialogHandler(ui, selectedModem);
		
		new FrontlineUiUpdateJob() {
			public void run() {
				ui.add(configDialog.getDialog());
			}
		}.execute();
	}

	/** Starts the phone auto-detector. */
	public void phoneManager_detectModems() {
		phoneManager.refreshPhoneList(true);
	}
	
//> INSTANCE HELPER METHODS
	/** 
	 * Refreshes the list of PhoneHandlers displayed on the PhoneManager tab.
	 */
	public void refresh() {
		new FrontlineUiUpdateJob() {
			public void run() {
				Object modemListError = ui.find(COMPONENT_PHONE_MANAGER_MODEM_LIST_ERROR);
				// cache the selected item so we can reselect it when we've finished!
				int index = ui.getSelectedIndex(modemListError);
				
				int indexTop = ui.getSelectedIndex(getModemListComponent());
				
				ui.removeAll(getModemListComponent());
				ui.removeAll(modemListError);
	
				Collection<FrontlineMessagingService> messagingServices = new CopyOnWriteArraySet<FrontlineMessagingService>(); 
				messagingServices.addAll(phoneManager.getAll());
				messagingServices.addAll(mmsServiceManager.getAll());
				
				FrontlineMessagingService[] messagingServicesArray = messagingServices.toArray(new FrontlineMessagingService[0]);
				Arrays.sort(messagingServicesArray, MESSAGING_SERVICE_COMPARATOR);
				
				for (FrontlineMessagingService messagingService : messagingServicesArray) {
					if (messagingService.isConnected()) {
						ui.add(getModemListComponent(), getTableRow(messagingService, true));
					} else {
						ui.add(modemListError, getTableRow(messagingService, false));
					}
				}
	
				ui.setSelectedIndex(getModemListComponent(), indexTop);
				ui.setSelectedIndex(modemListError, index);
				ui.updateActiveConnections();
			}
		}.execute();
	}
	
	private Object getTableRow(FrontlineMessagingService service, boolean isConnected) {
		Object row = ui.createTableRow(service);
		this.ui.setAttachedObject(row, service);

		/** TYPE CELL (Icon) */
		final String typeIcon;
		if (service instanceof SmsModem) {
			typeIcon = Icon.PHONE_NUMBER;
		} else {
			typeIcon = Icon.SMS_HTTP;
		}

		Object typeCell = ui.createTableCell("");
		ui.setIcon(typeCell, typeIcon);
		ui.add(row, typeCell);
		
		/** MESSAGE TYPE CELL (Icon) */
		Object messageTypeCell = ui.createTableCell("");
		if (service instanceof MmsService) {
			ui.setIcon(messageTypeCell, Icon.MMS);
		} else if (service.isConnected() || service instanceof SmsInternetService) {
			ui.setIcon(messageTypeCell, Icon.SMS);
		}
		ui.add(row, messageTypeCell);
		
		
		/** PORT CELL */
		Object portCell = ui.createTableCell(service.getDisplayPort());
		ui.add(row, portCell);
		
		/** NAME CELL */
		Object nameCell = ui.createTableCell(service.getServiceName());
		ui.add(row, nameCell);
		
		/** ID CELL */
		Object idCell = ui.createTableCell(service.getServiceIdentification());
		ui.add(row, idCell);
		
		
		/** USE FOR RECEIVING/SENDING CELLS */
		Object useForSendingCell = ui.createTableCell("");
		Object useForReceiveCell = ui.createTableCell("");
		if (isConnected) {
			if (service.isUseForSending()) ui.setIcon(useForSendingCell, Icon.CIRLCE_TICK);
			if (service.isUseForReceiving()) ui.setIcon(useForReceiveCell, Icon.CIRLCE_TICK);
			ui.add(row, useForSendingCell);
			ui.add(row, useForReceiveCell);
		}
		
		
		/** STATUS CELL */
		// Add "status cell" - "traffic light" showing how functional the device is
		final String statusIcon;
		FrontlineMessagingServiceStatus status = service.getStatus();
		if (status.equals(SmsModemStatus.CONNECTING) ||
			status.equals(SmsModemStatus.DETECTED) ||
			status.equals(SmsModemStatus.TRY_TO_CONNECT) ||
			status.equals(MmsEmailServiceStatus.FETCHING)) {
			statusIcon = Icon.LED_AMBER;	
		} else if (service.isConnected()){
			statusIcon = Icon.LED_GREEN;
		} else {
			statusIcon = Icon.LED_RED;
		}
	
		Object statusCell = ui.createTableCell(getServiceStatusAsString(service));
		ui.setIcon(statusCell, statusIcon);
		ui.add(row, statusCell);
		
		return row;
	}

//> STATIC FACTORIES

//> STATIC HELPER METHODS
	/**
	 * Gets the status of an {@link FrontlineMessagingService} as an internationalised {@link String}.
	 * @param service
	 * @return An internationalised {@link String} describing the status of the {@link FrontlineMessagingService}.
	 */
	private static String getServiceStatusAsString(FrontlineMessagingService service) {
		String statusString = InternationalisationUtils.getI18nString(service.getStatus().getI18nKey(), service.getStatusDetail());
		
		if (service instanceof MmsEmailService && service.getStatus().equals(MmsEmailServiceStatus.READY)) {
			Long lastChecked = ((MmsEmailService) service).getEmailAccount().getLastCheck();
			if (lastChecked != null) {
				statusString += " - " + InternationalisationUtils.getI18nString(I18N_EMAIL_LAST_CHECKED, InternationalisationUtils.getDatetimeFormat().format(new Date (lastChecked)));
			}
		}
		
		return statusString;
	}
	
	/**
	 * UI event called when the user changes tab
	 */
	public void notify(final FrontlineEventNotification notification) {
		super.notify(notification);
		// This object is registered to the UIGeneratorController and get notified when the users changes tab
		if (notification instanceof TabChangedNotification) {
			String newTabName = ((TabChangedNotification) notification).getNewTabName();
			if (newTabName.equals(TAB_ADVANCED_PHONE_MANAGER)) {
				threadSafeRefresh();
				ui.setStatus(InternationalisationUtils.getI18nString(MESSAGE_MODEM_LIST_UPDATED));
			}
		} else if (notification instanceof SmsModemStatusNotification) {
			new FrontlineUiUpdateJob() {
				public void run() {
					SmsModem activeService = ((SmsModemStatusNotification) notification).getService();
					SmsModemStatus serviceStatus = ((SmsModemStatusNotification) notification).getStatus();
					// FIXME re-implement status update on the AWT Event Queue - doing it here causes splitpanes to collapse
					// ui.setStatus(InternationalisationUtils.getI18NString(MESSAGE_PHONE) + ": " + activeDevice.getPort() + ' ' + getSmsDeviceStatusAsString(device));
					if (serviceStatus.equals(SmsModemStatus.CONNECTED)) {
						log.debug("Phone is connected. Try to read details from database!");
						String serial = activeService.getSerial();
						SmsModemSettings settings = smsModelSettingsDao.getSmsModemSettings(serial);
						
						// If this is the first time we've attached this phone, or no settings were
						// saved last time, we should show the settings dialog automatically
						if(settings == null) {
							log.debug("User need to make setting related this phone.");
							showPhoneSettingsDialog(activeService, true);
						} else {
							// Let's update the Manufacturer & Model for this device, if it wasn't previously set
							if (settings.getManufacturer() == null || settings.getModel() == null) {
								settings.setManufacturer(activeService.getManufacturer());
								settings.setModel(activeService.getModel());
								
								smsModelSettingsDao.updateSmsModemSettings(settings);
							}
							
							boolean supportsReceive = activeService.supportsReceive();
							if (settings.supportsReceive() != supportsReceive) {
								settings.setSupportsReceive(supportsReceive);
								
								smsModelSettingsDao.updateSmsModemSettings(settings);
							}
							
							activeService.setUseForSending(settings.useForSending());
							activeService.setUseDeliveryReports(settings.useDeliveryReports());
		
							if(activeService.supportsReceive()) {
								activeService.setUseForReceiving(settings.useForReceiving());
								activeService.setDeleteMessagesAfterReceiving(settings.deleteMessagesAfterReceiving());
							}
						}
		
						eventBus.notifyObservers(new HomeTabEventNotification(HomeTabEventNotification.Type.PHONE_CONNECTED, InternationalisationUtils.getI18nString(COMMON_PHONE_CONNECTED) + ": " + activeService.getModel()));
					}
					refresh();
				}
			}.execute();
		} else if(notification instanceof SmsInternetServiceStatusNotification) {
			SmsInternetService service = ((SmsInternetServiceStatusNotification) notification).getService();
			SmsInternetServiceStatus serviceStatus = ((SmsInternetServiceStatusNotification) notification).getStatus();
			if (serviceStatus.equals(SmsInternetServiceStatus.CONNECTED)) {
				eventBus.notifyObservers(new HomeTabEventNotification(
						HomeTabEventNotification.Type.SMS_INTERNET_SERVICE_CONNECTED,
						InternationalisationUtils.getI18nString(COMMON_SMS_INTERNET_SERVICE_CONNECTED) 
						+ ": " + SmsInternetServiceSettingsHandler.getProviderName(service.getClass()) + " - " + service.getIdentifier()));
			} else if (serviceStatus.equals(SmsInternetServiceStatus.RECEIVING_FAILED)) {
				eventBus.notifyObservers(new HomeTabEventNotification(
						HomeTabEventNotification.Type.SMS_INTERNET_SERVICE_RECEIVING_FAILED,
						SmsInternetServiceSettingsHandler.getProviderName(service.getClass()) + " - " + service.getIdentifier()
						+ ": " + InternationalisationUtils.getI18nString(FrontlineSMSConstants.COMMON_SMS_INTERNET_SERVICE_RECEIVING_FAILED)));
			}
			threadSafeRefresh();
		} else if (notification instanceof MmsServiceStatusNotification) {
			threadSafeRefresh();
		} else if (notification instanceof DatabaseEntityNotification<?>) {
			// Database notification
			Object entity = ((DatabaseEntityNotification<?>) notification).getDatabaseEntity();
			if (entity instanceof EmailAccount
					|| entity instanceof SmsModemSettings
					|| entity instanceof PersistableSettings) {
				// If there is any change in the E-Mail accounts, we refresh the list of Messaging Services
				threadSafeRefresh();
			}
		}
	}
}
