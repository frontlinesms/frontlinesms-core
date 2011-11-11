package net.frontlinesms.ui.handler.settings;

import java.util.List;

import net.frontlinesms.data.domain.SmsModemSettings;
import net.frontlinesms.data.repository.SmsModemSettingsDao;
import net.frontlinesms.settings.BaseSectionHandler;
import net.frontlinesms.settings.FrontlineValidationMessage;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.settings.UiSettingsSectionHandler;

public class SettingsDeviceSectionHandler extends BaseSectionHandler implements UiSettingsSectionHandler, ThinletUiEventHandler {
	private static final String UI_SECTION_DEVICE = "/ui/core/settings/services/pnDeviceSettings.xml";
	private static final String UI_FILE_PANEL_MODEM_SETTINGS = "/ui/core/phones/pnDeviceSettings.xml";
	
	private static final String UI_COMPONENT_TF_SMSC_NUMBER = "tfSmscNumber";
	private static final String UI_COMPONENT_TF_SIM_PIN = "tfPin";
	private static final String UI_COMPONENT_PHONE_SENDING = "cbSending";
	private static final String UI_COMPONENT_PHONE_RECEIVING = "cbReceiving";
	private static final String UI_COMPONENT_PHONE_DELETE = "cbDeleteMsgs";
	private static final String UI_COMPONENT_PHONE_DELIVERY_REPORTS = "cbUseDeliveryReports";
	private static final String UI_COMPONENT_PN_DEVICE_SETTINGS_CONTAINER = "pnDeviceSettingsContainer";
	private static final String UI_COMPONENT_PN_PHONE_SETTINGS = "pnPhoneSettings";
	private static final String UI_COMPONENT_RB_PHONE_DETAILS_ENABLE = "rbPhoneDetailsEnable";
	
	private static final String SECTION_ITEM_DEVICE_SMSC_NUMBER = "SERVICES_DEVICES_SMSC_NUMBER";
	private static final String SECTION_ITEM_DEVICE_SIM_PIN = "SERVICES_DEVICES_PIN";
	private static final String SECTION_ITEM_DEVICE_SETTINGS = "SERVICES_DEVICES_SETTINGS";
	private static final String SECTION_ITEM_DEVICE_USE = "SERVICES_DEVICES_USE";
	private static final String SECTION_ITEM_DEVICE_USE_FOR_SENDING = "SERVICES_DEVICES_USE_FOR_SENDING";
	private static final String SECTION_ITEM_DEVICE_USE_FOR_RECEIVING = "SERVICES_DEVICES_USE_FOR_RECEIVING";
	private static final String SECTION_ITEM_DEVICE_USE_DELIVERY_REPORTS = "SERVICES_DEVICES_USE_DELIVERY_REPORTS";
	private static final String SECTION_ITEM_DEVICE_DELETE_MESSAGES = "SERVICES_DEVICES_DELETE_MESSAGES";
	
	private static final String I18N_SETTINGS_MENU_DEVICES = "settings.menu.devices";

	private SmsModemSettingsDao smsModemSettingsDao;

	private SmsModemSettings deviceSettings;
	
	public SettingsDeviceSectionHandler (UiGeneratorController ui, SmsModemSettings deviceSettings) {
		super(ui);
		this.smsModemSettingsDao = ui.getFrontlineController().getSmsModemSettingsDao();
		this.setDeviceSettings(deviceSettings);
	}
	
	protected void init() {
		this.panel = ui.loadComponentFromFile(UI_SECTION_DEVICE, this);
		
		Object deviceSettingsPanelContainer = find(UI_COMPONENT_PN_DEVICE_SETTINGS_CONTAINER);
		this.ui.removeAll(deviceSettingsPanelContainer);
		
		Object pnDeviceSettings = this.ui.loadComponentFromFile(UI_FILE_PANEL_MODEM_SETTINGS, this);
		this.ui.add(deviceSettingsPanelContainer, pnDeviceSettings);
		
		this.populateDeviceSettingsPanel();
	}

	/**
	 * Populates the device settings in the panel.
	 */
	private void populateDeviceSettingsPanel() {
		// TODO: Merge this with the DeviceSettingsHandler to avoid duplication
		boolean supportsReceive = this.getDeviceSettings().supportsReceive();
		boolean useForSending = this.getDeviceSettings().useForSending();
		boolean useForReceiving = this.getDeviceSettings().useForReceiving();
		boolean useDeliveryReports = this.getDeviceSettings().useDeliveryReports();
		boolean deleteMessages = this.getDeviceSettings().deleteMessagesAfterReceiving();
		
		String smscNumber = this.getDeviceSettings().getSmscNumber();
		String simPin = this.getDeviceSettings().getSimPin();
		
		if(useForSending || useForReceiving) {
			this.ui.setSelected(this.find(UI_COMPONENT_PHONE_SENDING), useForSending);
			Object cbDeliveryReports = this.find(UI_COMPONENT_PHONE_DELIVERY_REPORTS);
			this.ui.setEnabled(cbDeliveryReports, useForSending);
			this.ui.setSelected(cbDeliveryReports, useDeliveryReports);
			this.ui.setSelected(this.find(UI_COMPONENT_PHONE_RECEIVING), useForReceiving);
			Object cbDeleteMessages = this.find(UI_COMPONENT_PHONE_DELETE);
			this.ui.setEnabled(cbDeleteMessages, useForReceiving);
			this.ui.setSelected(cbDeleteMessages, deleteMessages);
		} else {
			this.ui.setSelected(find("rbPhoneDetailsDisable"), true);
			this.ui.setSelected(find(UI_COMPONENT_RB_PHONE_DETAILS_ENABLE), false);
			this.ui.deactivate(find(UI_COMPONENT_PN_PHONE_SETTINGS));
		}
		
		if(!supportsReceive) {
			// If the configured device does not support SMS receiving, we need to pass this info onto
			// the user.  We also want to gray out the options for receiving.
			this.ui.setEnabled(find(UI_COMPONENT_PHONE_RECEIVING), false);
			this.ui.setEnabled(find(UI_COMPONENT_PHONE_DELETE), false);
		} else {
			// No error, so remove the error message.
			this.ui.remove(find("lbReceiveNotSupported"));
		}
		
		this.ui.setText(find(UI_COMPONENT_TF_SMSC_NUMBER), smscNumber);
		this.ui.setText(find(UI_COMPONENT_TF_SIM_PIN), simPin);
		
		// Save the original values for this device
		this.saveAndMarkUnchanged(SECTION_ITEM_DEVICE_SMSC_NUMBER, smscNumber);
		this.saveAndMarkUnchanged(SECTION_ITEM_DEVICE_SIM_PIN, simPin);
		this.saveAndMarkUnchanged(SECTION_ITEM_DEVICE_SETTINGS, this.getDeviceSettings());
		this.saveAndMarkUnchanged(SECTION_ITEM_DEVICE_USE, useForReceiving || useForSending);
		this.saveAndMarkUnchanged(SECTION_ITEM_DEVICE_USE_FOR_SENDING, useForSending);
		this.saveAndMarkUnchanged(SECTION_ITEM_DEVICE_USE_FOR_RECEIVING, useForReceiving);
		this.saveAndMarkUnchanged(SECTION_ITEM_DEVICE_USE_DELIVERY_REPORTS, useDeliveryReports);
		this.saveAndMarkUnchanged(SECTION_ITEM_DEVICE_DELETE_MESSAGES, deleteMessages);
	}
	
	private void saveAndMarkUnchanged(String sectionItem, Object value) {
		this.originalValues.put(sectionItem, value);
	//	super.settingChanged(sectionItem, value);
	}

	public void phoneManagerDetailsUse(Object radioButton) {
		Object pnPhoneSettings = find(UI_COMPONENT_PN_PHONE_SETTINGS);
		
		boolean useDevice = UI_COMPONENT_RB_PHONE_DETAILS_ENABLE.equals(this.ui.getName(radioButton));
		if(useDevice) {
			this.ui.activate(pnPhoneSettings);
			// If this phone does not support SMS receiving, we need to pass this info onto
			// the user.  We also want to gray out the options for receiving.
			if(!this.getDeviceSettings().supportsReceive()) {
				this.ui.setEnabled(find(UI_COMPONENT_PHONE_RECEIVING), false);
				this.ui.setEnabled(find(UI_COMPONENT_PHONE_DELETE), false);
			}
		} else this.ui.deactivate(pnPhoneSettings);
		
		super.settingChanged(SECTION_ITEM_DEVICE_USE, useDevice);
	}
	
	public void phoneManagerDetailsCheckboxChanged(Object checkbox) {
		boolean selected = this.ui.isSelected(checkbox);
		
		String sectionItem = null;
		if (checkbox.equals(find(UI_COMPONENT_PHONE_SENDING))) {
			sectionItem = SECTION_ITEM_DEVICE_USE_FOR_SENDING;
		} else if (checkbox.equals(find(UI_COMPONENT_PHONE_RECEIVING))) {
			sectionItem = SECTION_ITEM_DEVICE_USE_FOR_RECEIVING;
		} else if (checkbox.equals(find(UI_COMPONENT_PHONE_DELIVERY_REPORTS))) {
			sectionItem = SECTION_ITEM_DEVICE_USE_DELIVERY_REPORTS;
		} else if (checkbox.equals(find(UI_COMPONENT_PHONE_DELETE))) {
			sectionItem = SECTION_ITEM_DEVICE_DELETE_MESSAGES;
		}
		
		super.settingChanged(sectionItem, selected);
	}
	
	public void smscNumberChanged(String smscNumber) {
		settingChanged(SECTION_ITEM_DEVICE_SMSC_NUMBER, smscNumber);
	}
	
	public void pinChanged(String simPin) {
		settingChanged(SECTION_ITEM_DEVICE_SIM_PIN, simPin);
	}
	
	public void showHelpPage(String page) {
		this.ui.showHelpPage(page);
	}

	public void save() {
		boolean supportsReceive = this.getDeviceSettings().supportsReceive();
		
		boolean useForSending;
		boolean useDeliveryReports;
		boolean useForReceiving;
		boolean deleteMessagesAfterReceiving;
		if(this.ui.isSelected(find(UI_COMPONENT_RB_PHONE_DETAILS_ENABLE))) {
			useForSending = this.ui.isSelected(find(UI_COMPONENT_PHONE_SENDING));
			useDeliveryReports = this.ui.isSelected(find(UI_COMPONENT_PHONE_DELIVERY_REPORTS));
			useForReceiving = this.ui.isSelected(find(UI_COMPONENT_PHONE_RECEIVING));
			deleteMessagesAfterReceiving = this.ui.isSelected(find(UI_COMPONENT_PHONE_DELETE));
		} else {
			useForSending = false;
			useDeliveryReports = false;
			useForReceiving = false;
			deleteMessagesAfterReceiving = false;
		}
		
		this.getDeviceSettings().setUseForSending(useForSending);
		this.getDeviceSettings().setUseDeliveryReports(useDeliveryReports);
		
		if(supportsReceive) {
			this.getDeviceSettings().setUseForReceiving(useForReceiving);
			this.getDeviceSettings().setDeleteMessagesAfterReceiving(deleteMessagesAfterReceiving);
		} else {
			this.getDeviceSettings().setUseForReceiving(false);
			this.getDeviceSettings().setDeleteMessagesAfterReceiving(false);
		}
		
		this.getDeviceSettings().setSmscNumber(this.ui.getText(find(UI_COMPONENT_TF_SMSC_NUMBER)));
		this.getDeviceSettings().setSimPin(this.ui.getText(find(UI_COMPONENT_TF_SIM_PIN)));
		
		this.smsModemSettingsDao.updateSmsModemSettings(this.getDeviceSettings());
	}

	public List<FrontlineValidationMessage> validateFields() {
		return null;
	}

	public String getTitle() {
		return InternationalisationUtils.getI18nString(I18N_SETTINGS_MENU_DEVICES);
	}

	public void setDeviceSettings(SmsModemSettings deviceSettings) {
		this.deviceSettings = deviceSettings;
	}

	public SmsModemSettings getDeviceSettings() {
		return deviceSettings;
	}
}