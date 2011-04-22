package net.frontlinesms.ui.handler.phones;

import java.util.Enumeration;

import net.frontlinesms.CommUtils;
import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.messaging.FrontlineMessagingService;
import net.frontlinesms.messaging.sms.SmsServiceManager;
import net.frontlinesms.messaging.sms.modem.SmsModem;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.i18n.TextResourceKeyOwner;

import org.apache.log4j.Logger;
import org.smslib.handler.ATHandler;
import org.smslib.handler.CATHandler;
import org.smslib.handler.CATHandlerUtils;

import serial.CommPortIdentifier;
import serial.NoSuchPortException;

/**
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 * @author Alex Anderson <alex@frontlinesms.com>
 */
@TextResourceKeyOwner
public class DeviceManualConfigDialogHandler implements ThinletUiEventHandler {

//> STATIC CONSTANTS
	/** The fully-qualified name of the default {@link CATHandler} class. */
	private static final String DEFAULT_CAT_HANDLER_CLASS_NAME = CATHandler.class.getName();
	
//> UI LAYOUT FILES
	/** UI XML File Path: phone config dialog TODO what is this dialog for? */
	private static final String UI_FILE_MODEM_MANUAL_CONFIG_DIALOG = "/ui/core/phones/dgModemManualConfig.xml";
	
//> UI COMPONENT NAMES
	/** UI component: panel containing manual settings. */
	private static final String COMPONENT_MANUAL_SETTINGS_PANEL = "pnManualSettings";
	/** UI component: radio checkbox specifying that config should be detected rather than specified. */
	private static final String COMPONENT_DETECT_CONFIG_CHECKBOX = "cbDetectConfig";
	/** UI component: Textfield containing the PIN to use for the connection. */
	private static final String COMPONENT_PIN_TEXTFIELD = "tfPin";
	/** UI component: Combobox containing the name of the port to connect to. */
	private static final String COMPONENT_PORT_NAME_COMBOBOX = "cbPortName";
	/** UI component: Combobox containing the baud rate for manual connection */
	private static final String COMPONENT_BAUD_RATE_COMBOBOX = "cbBaudRate";
	/** UI component: Combobox containing the name of the CAT Handler to connect with. */
	private static final String COMPONENT_CAT_HANDLER_COMBOBOX = "cbCatHandler";

//> I18N KEYS
	/** I18n Text Key: TODO */
	private static final String MESSAGE_INVALID_BAUD_RATE = "message.invalid.baud.rate";
	/** I18n Text Key: TODO */
	private static final String MESSAGE_PORT_NOT_FOUND = "message.port.not.found";
	/** I18n Text Key: TODO */
	private static final String MESSAGE_PORT_ALREADY_CONNECTED = "message.port.already.connected";
	/** I18n Text Key: The requested port is already in use. */
	private static final String I18N_PORT_IN_USE = "com.port.inuse";

//> INSTANCE PROPERTIES
	/** Logger */
	private Logger log = FrontlineUtils.getLogger(this.getClass());
	private UiGeneratorController ui;
	/** The manager of {@link FrontlineMessagingService}s */
	private final SmsServiceManager phoneManager;
	/** The dialog that this class handles events for */
	private Object dialogComponent;
	/** The device that we are trying to connect to. */
	private SmsModem device;

	/**
	 * @param ui
	 * @param device an instance of {@link SmsModem}, or <code>null</code> if none is specified. FIXME i think this should just be a {@link String} specifying the port
	 */
	public DeviceManualConfigDialogHandler(UiGeneratorController ui, SmsModem device) {
		this.ui = ui;
		this.phoneManager = ui.getPhoneManager();
		assert(device == null || device instanceof SmsModem) : "This class should only be created for handling connections to an SMS Modem.";
		this.device = device;
	}
	
	/**
	 * Initialize the statistics dialog
	 */
	private void initDialog() {
		log.trace("INIT DEVICE MANUAL CONFIG DIALOG");
		this.dialogComponent = ui.loadComponentFromFile(UI_FILE_MODEM_MANUAL_CONFIG_DIALOG, this);
		
		Object portList = find(COMPONENT_PORT_NAME_COMBOBOX);
		Enumeration<CommPortIdentifier> commPortEnumeration = CommUtils.getPortIdentifiers();
		while (commPortEnumeration.hasMoreElements()) {
			CommPortIdentifier commPortIdentifier = commPortEnumeration.nextElement();
			ui.add(portList, ui.createComboboxChoice(commPortIdentifier.getName(), null));
		}
		
		Object handlerList = find(COMPONENT_CAT_HANDLER_COMBOBOX);
		int trimLength = DEFAULT_CAT_HANDLER_CLASS_NAME.length() + 1;
		
		for (Class<? extends ATHandler> handler : CATHandlerUtils.getHandlers()) {
			String handlerName = handler.getName();
			if(handlerName.equals(DEFAULT_CAT_HANDLER_CLASS_NAME)) handlerName = "<default>";
			else handlerName = handlerName.substring(trimLength);
			ui.add(handlerList, ui.createComboboxChoice(handlerName, handler));
		}
		
		if (device instanceof SmsModem) {
			SmsModem modem = (SmsModem) device;
			ui.setText(find(COMPONENT_PORT_NAME_COMBOBOX), modem.getPort());
			ui.setText(find(COMPONENT_BAUD_RATE_COMBOBOX), String.valueOf(modem.getBaudRate()));
		}
		
		ui.setSelected(find(COMPONENT_DETECT_CONFIG_CHECKBOX), true);
		setDetectManual(false);
		
		log.trace("EXIT");
	}
	
//> PUBLIC ACCESSORS
	public Object getDialog() {
		initDialog();
		return this.dialogComponent;
	}
	
//> UI EVENT METHODS
	/** Event method fired when the radio button selection for manual vs automatic detection is changed. */
	public void setDetectManual(String detectManual) {
		assert (detectManual.equals("true") || detectManual.equals("false")) : "detectManual value must be 'true' or 'false'";
		setDetectManual(detectManual.equals("true"));
	}
	/** Event method fired when the radio button selection for manual vs automatic detection is changed. */
	private void setDetectManual(boolean detectManual) {
		// Enable/disable the manual settings boxes depending on the supplied setting
		ui.setEnabledRecursively(find(COMPONENT_MANUAL_SETTINGS_PANEL), detectManual);
	}
	
	/**
	 * Event: "connect" button clicked.
	 * Validate the form, and if it is OK, initiate the connection.
	 */
	public void doConnect() {
		boolean detectConfig = ui.isSelected(find(COMPONENT_DETECT_CONFIG_CHECKBOX));
		
		// check the port is free
		String requestedPortName = ui.getText(find(COMPONENT_PORT_NAME_COMBOBOX));
		if(phoneManager.hasPhoneConnected(requestedPortName)) {
			ui.alert(InternationalisationUtils.getI18nString(MESSAGE_PORT_ALREADY_CONNECTED, requestedPortName));
		} else {
			String pin = ui.getText(find(COMPONENT_PIN_TEXTFIELD)).trim();
			if(pin.length() == 0) pin = null;
			
			try {
				boolean connectingOk;
				if(detectConfig) {
					connectingOk = phoneManager.requestConnect(requestedPortName, pin);
				} else {
					String baudRateAsString = ui.getText(find(COMPONENT_BAUD_RATE_COMBOBOX));
					String preferredCATHandler = ui.getText(find(COMPONENT_CAT_HANDLER_COMBOBOX));
					try {
						connectingOk = phoneManager.requestConnect(requestedPortName,
								pin,
								Integer.parseInt(baudRateAsString),
								preferredCATHandler);
	
					} catch (NumberFormatException e) {
						// The specified baud is not a valid number
						ui.alert(InternationalisationUtils.getI18nString(MESSAGE_INVALID_BAUD_RATE, baudRateAsString));
						connectingOk = false;
					}
				}
				if(connectingOk) {
					removeDialog();
				} else {
					ui.alert(InternationalisationUtils.getI18nString(I18N_PORT_IN_USE));
				}
			} catch (NoSuchPortException e) {
				ui.alert(InternationalisationUtils.getI18nString(MESSAGE_PORT_NOT_FOUND, requestedPortName));
			}
		}
	}

	/** @see UiGeneratorController#removeDialog(Object) */
	public void removeDialog() {
		this.ui.removeDialog(this.dialogComponent);
	}
	public void showHelpPage(String page) {
		ui.showHelpPage(page);
	}
	
//> UI HELPER METHODS
	/** @return UI component with the supplied name, or <code>null</code> if none could be found */
	private Object find(String componentName) {
		return ui.find(this.dialogComponent, componentName);
	}
}