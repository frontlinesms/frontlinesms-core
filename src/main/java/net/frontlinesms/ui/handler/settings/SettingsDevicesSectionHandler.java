package net.frontlinesms.ui.handler.settings;

import java.util.List;

import net.frontlinesms.AppProperties;
import net.frontlinesms.data.domain.SmsModemSettings;
import net.frontlinesms.settings.BaseSectionHandler;
import net.frontlinesms.settings.FrontlineValidationMessage;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.settings.UiSettingsSectionHandler;

public class SettingsDevicesSectionHandler extends BaseSectionHandler implements UiSettingsSectionHandler, ThinletUiEventHandler {
	private static final String UI_SECTION_DEVICES = "/ui/core/settings/services/pnDevicesSettings.xml";
	
	private static final String UI_COMPONENT_CB_PROMPT_DEVICE_CONNECTION_PROBLEM_DIALOG = "cbPromptConnectionProblemDialog";
	private static final String UI_COMPONENT_CB_START_DETECTING = "cbDetectAtStartup";
	//private static final String UI_COMPONENT_CB_DISABLE_ALL = "cbDisableAllDevices";
	private static final String SECTION_ITEM_PROMPT_DEVICE_CONNECTION_PROBLEM_DIALOG = "SERVICES_DEVICES_PROMPT_DEVICE_CONNECTION_PROBLEM_DIALOG";
	private static final String SECTION_ITEM_START_DETECTING = "SERVICES_DEVICES_START_DETECTING";
	
	private static final String I18N_SETTINGS_MENU_DEVICES = "settings.menu.devices";

	public SettingsDevicesSectionHandler (UiGeneratorController ui) {
		super(ui);
	}
	
	protected void init() {
		this.panel = ui.loadComponentFromFile(UI_SECTION_DEVICES, this);
		
		// Populating
		AppProperties appProperties = AppProperties.getInstance();
		boolean shouldPromptDeviceConnectionProblemDialog = appProperties.shouldPromptDeviceConnectionDialog();
		//boolean disableAllDevices = appProperties.disableAllDevices();
		boolean startDetectingAtStartup = appProperties.startDetectingAtStartup();
		
		this.ui.setSelected(find(UI_COMPONENT_CB_PROMPT_DEVICE_CONNECTION_PROBLEM_DIALOG), shouldPromptDeviceConnectionProblemDialog);
		this.ui.setSelected(find(UI_COMPONENT_CB_START_DETECTING), startDetectingAtStartup);
		//this.uiController.setSelected(find(UI_COMPONENT_CB_DISABLE_ALL), disableAllDevices);

		this.originalValues.put(SECTION_ITEM_PROMPT_DEVICE_CONNECTION_PROBLEM_DIALOG, shouldPromptDeviceConnectionProblemDialog);
		this.originalValues.put(SECTION_ITEM_START_DETECTING, startDetectingAtStartup);
	}
	
//	public void disableAllDevicesChanged (boolean disableAllDevices) {
//		super.settingChanged(SECTION_ITEM_DISABLE_ALL_DEVICES, disableAllDevices);
//		
//		this.enableDevicesPanels(!disableAllDevices);
//	}

	/**
	 * Called when the "startDetectingDevicesAtStartup" Checkbox has changed state.
	 * @param startDetectingDevicesAtStartup
	 */
	public void startDetectingDevicesAtStartup (boolean startDetectingDevicesAtStartup) {
		super.settingChanged(SECTION_ITEM_START_DETECTING, startDetectingDevicesAtStartup);
	}
	
	public void save() {
		/** PROPERTIES **/
		AppProperties appProperties = AppProperties.getInstance();
		
		appProperties.setShouldPromptDeviceConnectionDialog(this.ui.isSelected(find(UI_COMPONENT_CB_PROMPT_DEVICE_CONNECTION_PROBLEM_DIALOG)));
		appProperties.shouldStartDetectingAtStartup(this.ui.isSelected(find(UI_COMPONENT_CB_START_DETECTING)));
		//appProperties.shouldDisableAllDevices(this.uiController.isSelected(find(UI_COMPONENT_CB_DISABLE_ALL)));

		appProperties.saveToDisk();
	}
	
	public List<FrontlineValidationMessage> validateFields() {
		return null;
	}

	/**
	 * The promptConnectionProblemDialog checkbox has changed state
	 */
	public void promptConnectionProblemDialogChanged (boolean shouldPromptConnectionProblemDialog) {
		super.settingChanged(SECTION_ITEM_PROMPT_DEVICE_CONNECTION_PROBLEM_DIALOG, shouldPromptConnectionProblemDialog);
	}
	
	public String getTitle() {
		return InternationalisationUtils.getI18nString(I18N_SETTINGS_MENU_DEVICES);
	}

	public Object getSectionNode() {
		Object devicesNode = createSectionNode(InternationalisationUtils.getI18nString(I18N_SETTINGS_MENU_DEVICES), this, "/icons/phone_manualConfigure.png");
		addSubDevices(devicesNode);
		ui.setExpanded(devicesNode, false);
		
		return devicesNode;
	}
	
	/**
	 * Adds as many subnodes as there is known devices
	 * @param ui
	 * @param devicesNode 
	 */
	private void addSubDevices(Object devicesNode) {
		List<SmsModemSettings> devicesSettings = this.ui.getFrontlineController().getSmsModemSettingsDao().getAll();
		
		for (SmsModemSettings deviceSettings : devicesSettings) {
			String deviceItemName = deviceSettings.getManufacturer() + " " + deviceSettings.getModel();
			if (deviceItemName.trim().isEmpty()) {
				deviceItemName = deviceSettings.getSerial();
			}
			
			SettingsDeviceSectionHandler deviceHandler = new SettingsDeviceSectionHandler(this.ui, deviceSettings);
			this.ui.add(devicesNode, createSectionNode(deviceItemName, deviceHandler, "/icons/phone_number.png"));
		}
	}
}