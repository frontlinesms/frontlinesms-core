/*
 * FrontlineSMS <http://www.frontlinesms.com>
 * Copyright 2007, 2008 kiwanja
 * 
 * This file is part of FrontlineSMS.
 * 
 * FrontlineSMS is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * FrontlineSMS is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FrontlineSMS. If not, see <http://www.gnu.org/licenses/>.
 */
package net.frontlinesms.messaging.sms.modem;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import serial.*;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.domain.*;
import net.frontlinesms.data.domain.FrontlineMessage.Status;
import net.frontlinesms.listener.SmsListener;
import net.frontlinesms.messaging.CatHandlerAliasMatcher;
import net.frontlinesms.messaging.sms.SmsService;

import org.apache.log4j.Logger;
import org.smslib.*;
import org.smslib.CService.MessageClass;

/**
 * Class for handling the serial connection to an individual SMS device.
 * 
 * @author Ben Whitaker ben(at)masabi(dot)com
 * @author Alex Anderson alex(at)masabi(dot)com
 * @author Carlos Eduardo Genz kadu(at)masabi(dot)com
 * @author james@tregaskis.org
 */
public class SmsModem extends Thread implements SmsService {
	
//> CONSTANTS
	private static final int SMS_BULK_LIMIT = 10;

	/** The time, in millis, that this phone handler must have been unresponsive for before it is deemed TIMED OUT
	 * As far as I know there is no basis for the time chosen for this timeout. */
	private static final int TIMEOUT = 80 * 1000; // = 80 seconds;

	/**
	 * The different baud rates that a PhoneHandler may connect at.
	 * We do not go above a baud rate of 115200bps here, as higher baud rates are not beneficial due to speed limits
	 * of the GSM network, and are also not recommended by the SMS Lib project for being unstable.
	 */
	private static final int[] COMM_SPEEDS = new int[] {
		9600,
		19200,
		38400,
		57600,
		115200
	};

	/** Logging object */
	private final Logger LOG = FrontlineUtils.getLogger(this.getClass());
		
//> PROPERTIES
	
	/**
	 * Watchdog to monitor when a phone handler has lost communication with the phone
	 */
	private long timeOfLastResponseFromPhone;

	private final ConcurrentLinkedQueue<CIncomingMessage> inbox = new ConcurrentLinkedQueue<CIncomingMessage>();
	private final ConcurrentLinkedQueue<FrontlineMessage> outbox = new ConcurrentLinkedQueue<FrontlineMessage>();
	/** The SmsListener to which this phone handler should report SMS Message events. */
	private final SmsListener smsListener;

	/** The name of the COM port that this PhoneHandler controls. */
	private final String portName;
	/**
	 * Indicates whether this PhoneHandler's serial communication
	 * thread is running.  This should *only* be set false in run()
	 * when certain other conditions are fulfilled - when
	 * smsLibConnected is false AND autoReconnect is false.
	 */ 
	private boolean running;
	private boolean phonePresent;
	private boolean smsLibConnected;
	private boolean tryToConnect;
	/** The baud rate, in bps, that this phone handler will connect at. */
	private int baudRate;
	private CService cService;
	private boolean autoDetect;
	private boolean autoReconnect;
	/** true if this is another port into a phone that is already discovered */
	private boolean duplicate;
	/** true when this PhoneHandler has been disconnected using disconnect() */
	private boolean disconnected;
	/** true if this phone is or will be used for sending SMS messages */ 
	private boolean useForSending;
	/** true if this phone is or will be used for receiving SMS messages */
	private boolean useForReceiving;
	/** true if this thread has timed out */
	private boolean timedOut;

	private boolean detecting;
	private boolean disconnecting;

	private boolean deleteMessagesAfterReceiving;
	private boolean useDeliveryReports;

	private String manufacturer = "";
	private String model = "";
	private String preferredCATHandler = "";
	private String serialNumber = "";
	private String imsiNumber;
	private int batteryPercent;
	private int signalPercent;
	private String msisdn;
	private String smscNumber;
	private String simPin;

	/** The status of this device */
	private SmsModemStatus status = SmsModemStatus.DORMANT;
	/** Extra info relating to the current status. */
	private String statusDetail;
	
	
//> CONSTRUCTORS
	/**
	 * Create a new instance {@link SmsModem}
	 * @param portName the name of the port which this modem is found on.  Value for {@link #portName}
	 * @param smsListener the value for {@link #smsListener} 
	 * @throws NoSuchPortException 
	 */
	public SmsModem(String portName, SmsListener smsListener) throws NoSuchPortException {
		super("SmsModem :: " + portName);
		/*
		 * Make this into a daemon thread - we never know when it may get blocked in native code.  Indeed this can be
		 * witnessed by connecting to a port without a device attached, which can then block as such (I think this one 
		 * was a bluetooth port):
		 *	Win32SerialPort.nwrite(byte[], int, int) line: not available [native method]	
		 *	Win32SerialPort.write(byte[], int, int) line: 672	
		 *	Win32SerialPort.write(int) line: 664	
		 *	Win32SerialOutputStream.write(int) line: 34
		 *  ...
		 */	
		super.setDaemon(true);

		assert(smsListener != null);
		this.smsListener = smsListener;
		this.portName = portName;

		resetWatchdog();

		//sets up the phone handler on a certain port, it will attempt to auto-detect the phone
		try {
			CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(this.portName);
			if(portIdentifier.isCurrentlyOwned()) {
				String currentOwner = portIdentifier.getCurrentOwner();
				if(currentOwner == null) {
					currentOwner = "?";
				}
				this.setStatus(SmsModemStatus.OWNED_BY_SOMEONE_ELSE, currentOwner);
			}
		} catch (NoSuchPortException ex) {
			LOG.debug("Error getting owner from port", ex);
			//doesn't matter if it doesn't get this message
			throw ex;
		}
	}
	
//> ACCESSOR METHODS
	/** @return {@link #status} */
	public SmsModemStatus getStatus() {
		return this.status;
	}
	
	/**
	 * Set the status of this {@link SmsModem}, and fires an event to {@link #smsListener}
	 * @param status the status
	 * @param detail detail relating to the status
	 */
	private void setStatus(SmsModemStatus status, String detail) {
		this.status = status;
		this.statusDetail = detail;
		LOG.debug("Status [" + status.name()
				+ (detail == null?"":": "+detail)
				+ "]");
		
		smsListener.smsDeviceEvent(this, this.status);
	}
	
	/** @return {@link #statusDetail} */
	public String getStatusDetail() {
		return this.statusDetail;
	}

	/** @return the next incoming message, or <code>null</code> if none is available */
	public CIncomingMessage nextIncomingMessage() {
		return inbox.poll();
	}

	public String getPort() {
		return portName;
	}

	/**
	 * Checks if this PhoneHandler is currently active.  Returns true if either the thread
	 * has stopped running OR the handler has been forcibly disconnected.  This extra check
	 * is necessary as PhoneHandler threads can sometimes go to sleep and never wake up
	 * (it seems).
	 * @return true if this phone is currently active, false otherwise.
	 */
	public boolean isRunning() {
		return !disconnected && running;
	}

	/**
	 * Checks if this instance of PhoneHandler has timed out.  Once a PhoneHandler has
	 * timed out once, it becomes unusable.
	 * @return true if this phone has timeout, false otherwise.
	 */
	public boolean isTimedOut() {
		if (!timedOut) timedOut = (System.currentTimeMillis() - timeOfLastResponseFromPhone) > TIMEOUT;
		return timedOut;
	}

	public int getBaudRate() {
		return baudRate;
	}

	public int getBatteryPercent() {
		if (smsLibConnected) return cService.getDeviceInfo().getBatteryLevel();
		else return batteryPercent;
	}
	
	/** @return the smscNumber */
	public String getSmscNumber() {
		if (smsLibConnected) this.smscNumber = cService.getSmscNumber();
		return this.smscNumber;
	}
	/** @param smscNumber the smscNumber to set */
	public void setSmscNumber(String smscNumber) {
		if (smsLibConnected) cService.setSmscNumber(smscNumber);
		this.smscNumber = smscNumber;
	}
	
	/** @return the SIM PIN */
	public String getSimPin() {
		if (smsLibConnected) this.simPin = cService.getSimPin();
		return this.simPin;
	}
	/** @param simPin the SIM PIN to set */
	public void setSimPin(String simPin) {
		if (smsLibConnected) cService.setSimPin(simPin);
		this.simPin = simPin;
	}

	public String getMsisdn() {
		return msisdn;
	}
	public int getSignalPercent() {
		if (smsLibConnected) return cService.getDeviceInfo().getSignalLevel();
		else return signalPercent;
	}

	/**
	 * Checks if this instance of PhoneHandler is connected to a device.
	 * @return true if this phone is connected, false otherwise.
	 */
	public boolean isConnected() {
		return smsLibConnected;
	}

	/** @see SmsService#isUseForSending() */
	public boolean isUseForSending() {
		return useForSending;
	}
	public void setUseForSending(boolean useForSend) {
		useForSending = useForSend;
	}

	/** @see SmsService#isUseForReceiving() */
	public boolean isUseForReceiving() {
		return useForReceiving;
	}

	/**
	 * This throws a {@link RuntimeException}, so {@link SmsService#supportsReceive()} should be checked before calling this.
	 */
	public void setUseForReceiving(boolean useForReceive) {
		if(!supportsReceive()) throw new ReceiveNotSupportedException();
		useForReceiving = useForReceive;
	}

	public boolean supportsReceive() {
		return cService.supportsReceive();
	}

	public boolean isDuplicate() {
		return duplicate;
	}
	
	/**
	 * Sets the status of this modem.  If the status is {@link SmsModemStatus#DUPLICATE}, an
	 * event will be triggered with {@link #smsListener}.
	 * @param newDuplicate new value for {@link #duplicate}
	 */
	public void setDuplicate(boolean newDuplicate) {
		duplicate = newDuplicate;
		if (duplicate) {
			this.setStatus(SmsModemStatus.DUPLICATE, null);
		}
	}
	
	/**
	 * Checks if this instance of PhoneHandler has a phone present.
	 * @return true if this has a phone present, false otherwise.
	 */
	public boolean isPhonePresent() {
		return phonePresent;
	}

	/** @return {@link #manufacturer} */
	public String getManufacturer() {
		return manufacturer;
	}

	/** @return {@link #model} */
	public String getModel() {
		return model;
	}

	/** @return {@link #serialNumber} */
	public String getSerial() {
		return serialNumber;
	}
	
	/** @return {@link #imsi} */
	public String getImsiNumber() {
		return imsiNumber;
	}
	
	public String getServiceName() {
		return FrontlineUtils.getManufacturerAndModel(getManufacturer(), getModel());
	}

	public void connect(){
		if (!phonePresent || duplicate || manufacturer.length() == 0) return;
		tryToConnect = true;
		resetWatchdog();
	}

	/**
	 * Try to connect to the device attached to this port, using the speed informed 
	 * as parameter.
	 * 
	 * @param maxSpeedRequested The max speed for this device.
	 * @param manufacturerName The device's manufacturer name.
	 * @param modelName The device's model name.
	 * @param preferredCATHandler TODO
	 * @return true if the connection was successful, false otherwise.
	 */
	private boolean connect(int maxSpeedRequested, String manufacturerName, String modelName, String preferredCATHandler) {
		LOG.trace("ENTER");
		LOG.debug("Attempting to connect:" 
				+ "\n - Speed [" + maxSpeedRequested + "]" 
				+ "\n - Manufacturer [" + manufacturerName + "]"
				+ "\n - Model [" + modelName + "]"
				+ "\n - CAT Handler Alias [" + preferredCATHandler + "]");

		this.setStatus(SmsModemStatus.TRY_TO_CONNECT, Integer.toString(maxSpeedRequested));
		
		resetWatchdog();
		cService = new CService(this.portName, maxSpeedRequested, manufacturerName, modelName, preferredCATHandler);
		LOG.debug("Created service [" + cService + "]");

		try {
			// If the GSM device is PIN protected, enter the PIN here.
			// PIN information will be used only when the GSM device reports that it needs a PIN in order to continue.
			if(this.simPin != null) {
				cService.setSimPin(this.simPin);
			} else {
				// If we don't have a PIN, then don't set it!
			}

			// Some modems may require a SIM PIN 2 to unlock their full functionality.
			// Like the Vodafone 3G/GPRS PCMCIA card.
			// If you have such a modem, you should also define the SIM PIN 2.
			// We don't have a SIM PIN2 set, so we don't set anything in the CService.  Previously
			// this code set PIN2 to 0000, but that seems foolish (see comments re: PIN1)

			// Normally, you would want to set the SMSC number to blank. GSM
			// devices normally get the SMSC number information from their SIM card.
			cService.setSmscNumber(this.smscNumber == null ? "" : this.smscNumber);

			FrontlineUtils.sleep_ignoreInterrupts(500);

			resetWatchdog();
			cService.connect();
			resetWatchdog();

			// Lets get info about the GSM device...
			setManufacturer(cService.getManufacturer());
			setModel(cService.getModel());

			this.msisdn = cService.getMsisdn();
			LOG.debug("Msisdn [" + this.msisdn + "]");

			this.serialNumber = cService.getSerialNo();
			LOG.debug("Serial Number [" + this.serialNumber + "]");

			this.imsiNumber = cService.getImsi();
			LOG.debug("Imsi Number [" + this.imsiNumber + "]");

			LOG.debug("Mobile Device Information: "
					+ "\n - Manufacturer [" + manufacturerName + "]"
					+ "\n - Model [" + modelName + "]"
					+ "\n - Preferred CAT Handler [" + preferredCATHandler + "]"
					+ "\n - Used CAT Handler [" + cService.getAtHandlerName() + "]"
					+ "\n - Serial Number [" + cService.getDeviceInfo().getSerialNo() + "]"
					+ "\n - IMSI [" + cService.getDeviceInfo().getImsi() + "]"
					+ "\n - S/W Version [" + cService.getDeviceInfo().getSwVersion() + "]"
					+ "\n - Battery Level [" + cService.getDeviceInfo().getBatteryLevel() + "%]"
					+ "\n - Signal Level [" + cService.getDeviceInfo().getSignalLevel() + "%]"
					+ "\n - Baud Rate [" + baudRate + "]");

			this.setStatus(SmsModemStatus.CONNECTING, null);
			
			if (isDuplicate()) {
				disconnect(false);
				return false;
			}
			phonePresent = true;
			autoReconnect = true;
			smsLibConnected = true;
			
			this.setStatus(SmsModemStatus.CONNECTED, Integer.toString(maxSpeedRequested));
			
			resetWatchdog();
			LOG.debug("Connection successful!");
			LOG.trace("EXIT");
			return true;
		} catch (GsmNetworkRegistrationException e) {
			this.setStatus(SmsModemStatus.GSM_REG_FAILED, null);
		} catch (PortInUseException ex) {
			this.setStatus(SmsModemStatus.OWNED_BY_SOMEONE_ELSE, ex.getClass().getSimpleName() + " : " + ex.getMessage());
		} catch (Exception ex) {
			this.setStatus(SmsModemStatus.FAILED_TO_CONNECT, ex.getClass().getSimpleName() + " : " + ex.getMessage());
		}
		LOG.debug("Connection failed!");
		LOG.trace("EXIT");
		return false;
	}

	/**
	 * Start the modem handler listening to the serial port with the requested connection settings.
	 * @param baudRate
	 * @param manufacturer
	 * @param model
	 * @param preferredCATHAndler
	 */
	public void start(int baudRate, String preferredCATHAndler) {
		this.autoDetect = false;
		this.tryToConnect = true;
		this.baudRate = baudRate;
		this.preferredCATHandler = preferredCATHAndler;
		super.start();
	}

	/**
	 * Start the sms modem listening to the serial port, and autodetect the settings to use.
	 */
	@Override
	public synchronized void start() {
		this.autoDetect = true;
		super.start();
	}


	public void run() {
		LOG.trace("ENTER");
		
		if(autoDetect) running = _doDetection();
		else running = true;
		while (running) {
			boolean noActivity = true;

			resetWatchdog();

			if(tryToConnect) {
				smsLibConnected = connect(baudRate, manufacturer, model, preferredCATHandler);
				tryToConnect = false;
			}

			if(smsLibConnected) {
				try {
					//check for incoming messages
					if (useForReceiving) {
						long startTime = System.currentTimeMillis();
						LOG.debug("Checking for received messages...");
						int newMessages = checkForMessages();
						if(newMessages > 0) noActivity = false;
						LOG.debug("Check for messages took [" + (System.currentTimeMillis() - startTime) + "]");
					}
					// If there are any messages waiting to be sent, send them now.
					if (useForSending) {
						if(LOG.isDebugEnabled()) LOG.debug("Sending some pending messages. Outbox size is [" + outbox.size() + "]");
						resetWatchdog();
						long startTime = System.currentTimeMillis();

						//create SMS list
						LinkedList<FrontlineMessage> messageList = new LinkedList<FrontlineMessage>();
						FrontlineMessage m;
						while(messageList.size() < SMS_BULK_LIMIT
								&& (m = outbox.poll()) != null) {
							messageList.add(m);
						}
						if(messageList.size() > 0) {
							LOG.debug("Sending bulk of [" + messageList.size() + "] message(s)");
							sendSmsListDirect(messageList);
						}
						LOG.debug("Send messages took [" + (System.currentTimeMillis() - startTime) + "]");
						resetWatchdog();
					}
				} catch(UnrecognizedHandlerProtocolException ex) {
					LOG.debug("Invalid message protocol specified for device.", ex);
					disconnect(true);
				} catch(UnableToReconnectException ex) {
					LOG.debug("Fatal exception in device communication.", ex);
					this.setAutoReconnect(false);
					setStatus(SmsModemStatus.DISCONNECT_FORCED, ex.getMessage());
					disconnect(false);
				} catch(SMSLibDeviceException ex) {
					LOG.debug("Phone not connected", ex);
					disconnect(true);
				} catch(IOException ex) {
					LOG.error("Communication failed", ex);
					disconnect(true);
				}
			} else if (autoReconnect) {
				LOG.debug("Trying to reconnect...");
				// Disconnect and relinquish ownership of the COM port.
				disconnect(true);
				// Reconnect on the next loop.
				tryToConnect = true;
			} else {
				running = false;
			}
			// If this thread is still running, we should have a little snooze as
			// checking the phone continuously for messages will:
			//  - waste a lot of processor cycles on PC
			//  - waste phone battery
			//  - be unnecessary as phones are unlikely to be able to receive messages quicker than ~every 3s
			if(running) {
				if (noActivity) {
					try {
						if(smsLibConnected) cService.keepGsmLinkOpen();
						FrontlineUtils.sleep_ignoreInterrupts(5000); /* 5 seconds */
					} catch (Throwable t) {
						LOG.debug("", t);
						tryToConnect = false;
						disconnect(true);
					}
				} else {
					// Changed this from 100ms to 500ms in an attempt to improve modem stability.  There was no explanation
					// for the original duration.
					FrontlineUtils.sleep_ignoreInterrupts(500);
				}
			}
		}
		LOG.trace("EXIT");
	}

	private final void setManufacturer(String manufacturer) {
		LOG.debug("Manufacturer before translation [" + manufacturer + "]");
		this.manufacturer = CatHandlerAliasMatcher.getInstance().translateManufacturer(manufacturer);
		LOG.debug("Manufacturer after translation [" + this.manufacturer + "]");
	}

	private final void setModel(String model) {
		LOG.debug("Model before translation [" + model + "]");
		this.model = CatHandlerAliasMatcher.getInstance().translateModel(manufacturer, model);
		LOG.debug("Model after translation [" + this.model + "]");
		setPreferredCatHandler();
	}

	private final void setPreferredCatHandler() {
		this.preferredCATHandler = CatHandlerAliasMatcher.getInstance().translateCATHandlerModel(manufacturer, model);
		LOG.debug("Preferred CAT Handler [" + this.preferredCATHandler + "]");
	}

	/**
	 * Discover the fastest speed at which we can connect to the AT device on this port.
	 * 
	 * N.B. THIS SHOULD ONLY BE CALLED FROM WITHIN run() - it is put here for readability.
	 * 
	 * FIXME this needs to fire the correct events.
	 * 
	 * @return true if a phone was found, or false otherwise
	 */
	private boolean _doDetection() {
		LOG.trace("ENTER");
		detecting = true;
		int maxBaudRate = 0;
		boolean phoneFound = false;
		
		this.setStatus(SmsModemStatus.SEARCHING, null);

		// Set this if there was a problem connecting and you'd like to report the detail.
		String lastDetail = null;
		for (int currentBaudRate : COMM_SPEEDS) {
			if (!isDetecting()) {
				disconnect(true);
				return false;
			}
			LOG.debug("Testing baud rate [" + currentBaudRate + "]");
			if (maxBaudRate == 0) {
				this.setStatus(SmsModemStatus.SEARCHING, Integer.toString(currentBaudRate));
			}

			resetWatchdog();
			cService = new CService(portName, currentBaudRate, "", "", "");
			// set this flag to false if the status has been updated within the error handling.  It is only
			// checked if no phone is found.
			try {
				cService.serialDriver.open();
				// wait for port to open and AT handler to awake
				FrontlineUtils.sleep_ignoreInterrupts(500);
				cService.serialDriver.send("AT\r");
				FrontlineUtils.sleep_ignoreInterrupts(500); // Wait here just in case the phone does not respond very quickly, e.g Nokia 6310i throws IOException without this line.
				String response = cService.serialDriver.getResponse();

				// If the phone returns an OK, then it looks like it works at this baud
				// rate.  Save this as the fastest speed, and then search at the next speed.
				if (response.contains("OK")) {
					maxBaudRate = currentBaudRate;
					setStatus(SmsModemStatus.DETECTED, Integer.toString(currentBaudRate));
					phoneFound = true;
				} 
			} catch(IOException ex) {
				// TODO Here, we've caught an IOException.  This could be something
				// quite fatal, like the port disappearing, or perhaps it could be
				// something less bad?  Let's assume it's fatal.
				LOG.error("Communication failed", ex);
				phoneFound = false;
				break;
			} catch(TooManyListenersException ex) {
				LOG.debug("Too Many Listeners", ex);
				lastDetail = ex.getClass().getSimpleName();
				if(ex.getMessage() != null) lastDetail += " :: " + ex.getMessage();
			} catch(UnsupportedCommOperationException ex) {
				LOG.debug("Unsupported Operation", ex);
				lastDetail = ex.getClass().getSimpleName();
				if(ex.getMessage() != null) lastDetail += " :: " + ex.getMessage();
			} catch(NoSuchPortException ex) {
				LOG.debug("Port does not exist", ex);
				lastDetail = ex.getClass().getSimpleName();
				if(ex.getMessage() != null) lastDetail += " :: " + ex.getMessage();
			} catch(PortInUseException ex) {
				LOG.debug("Port already in use", ex);
				lastDetail = ex.getClass().getSimpleName();
				if(ex.getMessage() != null) lastDetail += " :: " + ex.getMessage();
			} finally {
				disconnect(!phoneFound);
			}
		}

		if (!phoneFound) {
			disconnect(false);
			setStatus(SmsModemStatus.NO_PHONE_DETECTED, lastDetail);
		} else {
			try {
				baudRate = maxBaudRate;
				phonePresent = true;
				LOG.debug("Phone found, max speed is [" + baudRate + "]");
				cService = new CService(portName, maxBaudRate, "", "", "");
				
				cService.serialDriver.open();
				// wait for port to open and AT handler to awake
				FrontlineUtils.sleep_ignoreInterrupts(500);
					
				setManufacturer(cService.getManufacturer());
				setModel(cService.getModel());
				
				this.msisdn = cService.getMsisdn();
				LOG.debug("Msisdn [" + this.msisdn + "]");

				this.serialNumber = cService.getSerialNo();
				LOG.debug("Serial Number [" + this.serialNumber + "]");

				this.imsiNumber = cService.getImsi();
				LOG.debug("Imsi Number [" + this.imsiNumber + "]");

				try { 
					this.batteryPercent = cService.getBatteryLevel();
					LOG.debug("Battery Percent [" + this.batteryPercent + "]");
				} catch(NumberFormatException ex) {
					LOG.debug("Invalid Battery value [" + this.batteryPercent + "]", ex);
				}

				this.setStatus(SmsModemStatus.MAX_SPEED_FOUND, Integer.toString(maxBaudRate));
				if(!duplicate) {
					tryToConnect = true;
				}
			} catch (Exception ex) {
				// It's surprising, but we failed to connect to the phone even though we detected it successfully!
				LOG.error("Error while connecting to detected phone", ex);
				baudRate = 0;
				phonePresent = false;
				tryToConnect = false;
				this.setStatus(SmsModemStatus.FAILED_TO_CONNECT, ex.getClass().getSimpleName() + ": " + ex.getMessage());
			} finally {
				disconnect(false);				
			}
		}
		LOG.trace("EXIT");
		detecting = false;
		return phoneFound;
	}

	/** NB. Currently resets LAST ACTIVE time rather than the time left, or time to die */
	/** Resets the watchdog timer - used for calculating timeouts. */
	private final void resetWatchdog() {
		timeOfLastResponseFromPhone = System.currentTimeMillis();
	}

	/**
	 * Checks if there is new messages ready to be read from the attached device.
	 * 
	 * @throws IOException
	 * @return The number of new messages retrieved
	 * @throws SMSLibDeviceException 
	 */
	private int checkForMessages() throws IOException, SMSLibDeviceException {	
		LOG.trace("ENTER");
		resetWatchdog();
		LinkedList<CIncomingMessage> messageList = new LinkedList<CIncomingMessage>();
		cService.readMessages(messageList, MessageClass.UNREAD); // TODO make this changeable in settings - UNREAD vs ALL
		resetWatchdog();

		LOG.debug("[" + messageList.size() + "] message(s) received.");
		for(CIncomingMessage msg : messageList) {
			resetWatchdog();
			log(msg);
			setId(msg);

			addToInboxIfAppropriate(msg);
			deleteIfAppropriate(msg);
			resetWatchdog();
		}

		LOG.trace("EXIT");
		return messageList.size();
	}

	private void addToInboxIfAppropriate(CIncomingMessage msg) {
		if (useDeliveryReports
				|| msg.getType() != CIncomingMessage.MessageType.StatusReport) {
			inbox.add(msg);
		}
	}

	private void deleteIfAppropriate(CIncomingMessage msg) throws NotConnectedException, IOException {
		if (isDeleteMessagesAfterReceiving()
				|| msg.getType() == CIncomingMessage.MessageType.StatusReport) {
			//delete msg if is supposed to do it, or if it is a delivery report.
			LOG.debug("Removing message [" + msg.getId() + "] from phone.");
			cService.deleteMessage(msg);
		}
	}

	private void setId(CIncomingMessage msg) {
		if (msisdn != null && msisdn.length() != 0) {
			msg.setId(msisdn);
		} else if (serialNumber != null && serialNumber.length() != 0) {
			msg.setId(serialNumber);
		} else if (imsiNumber != null) {
			msg.setId(imsiNumber);
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("Changed ID [" + msg.getId() + "]");
		}
	}

	/** @see SmsService#sendSMS(net.frontlinesms.data.FrontlineMessage) */
	public void sendSMS(FrontlineMessage outgoingMessage) {
		LOG.trace("ENTER");
		outgoingMessage.setStatus(Status.PENDING);

		if (msisdn != null && !msisdn.equals("")) {
			outgoingMessage.setSenderMsisdn(msisdn);
		} else if (serialNumber != null && !serialNumber.equals("")) {
			outgoingMessage.setSenderMsisdn(serialNumber);
		} // Otherwise it will go with blank sender.

		outbox.add(outgoingMessage);
		smsListener.outgoingMessageEvent(this, outgoingMessage);
		LOG.debug("Message added to outbox. Size is [" + outbox.size() + "]");

		LOG.trace("EXIT");
	}

	/** @param msisdn new value for {@link #msisdn} */
	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

	public boolean isDeleteMessagesAfterReceiving() {
		return deleteMessagesAfterReceiving;
	}

	public void setDeleteMessagesAfterReceiving(
			boolean deleteMessagesAfterReceiving) {
		this.deleteMessagesAfterReceiving = deleteMessagesAfterReceiving;
	}

	public boolean isUseDeliveryReports() {
		return useDeliveryReports;
	}

	public void setUseDeliveryReports(boolean useDeliveryReports) {
		this.useDeliveryReports = useDeliveryReports;
	}

	public void setAutoReconnect(boolean autoReconnect) {
		this.autoReconnect = autoReconnect;
	}

	public boolean isDetecting() {
		return detecting;
	}

	public void setDetecting(boolean detecting) {
		this.detecting = detecting;
	}

	public boolean isTryToConnect() {
		return tryToConnect;
	}

	/**
	 * Set the baud rate that this phone should connect at.
	 * @param baudRate new value for {@link #baudRate}
	 */
	public void setBaudRate(int baudRate) {
		this.baudRate = baudRate;
	}

	/** @see SmsService#isBinarySendingSupported() */
	public boolean isBinarySendingSupported() {
		return cService.supportsBinarySmsSending();
	}
	
	/** @see SmsService#isUcs2SendingSupported() */
	public boolean isUcs2SendingSupported() {
		return cService.supportsUcs2SmsSending();
	}
	
	public boolean isDisconnecting() {
		return disconnecting;
	}
	
//> OTHER METHODS
	
	protected void disconnecting() {
		disconnecting = true;
		if(this.supportsReceive())
			this.setUseForReceiving(false);
		setUseForSending(false);
		
		this.setStatus(SmsModemStatus.DISCONNECTING, null);
		
		disconnect(true);
		disconnecting = false;
	}

	/**
	 * Forces the phone to disconnect from the COM port and close all listeners.
	 */
	public synchronized void disconnect() {
		if (isConnected()) {
			// Actually disconnecting the phone!
			new Thread("Disconnecting [" + this.getServiceName() + "]") {
				public void run() {
					disconnecting();
				}
			}.start();
		} else {
			disconnect(true);
		}
	}

	/**
	 * Forces the phone to disconnect from the COM port and close all listeners.
	 * @param setStatus set <code>true</code> if status should be updated.  This will likely only be set <code>false</code> when doing phone detection. 
	 */
	private void disconnect(boolean setStatus) {
		LOG.trace("ENTER");
		try {
			cService.disconnect();
		} catch(Throwable t) {
			// If anything goes wrong in the disconnect, we want to make
			// sure we still kill the serialDriver.  Any throwables can
			// be ignored.
			LOG.debug("Error disconnecting", t);
			try { 
				cService.serialDriver.close(); 
			} catch(Throwable th) {
				LOG.debug("Error disconnecting", th);
			}
		}
		timeOfLastResponseFromPhone = 0;
		disconnected = true;
		smsLibConnected = false;
		if(setStatus && !isDuplicate()) {
			this.setStatus(SmsModemStatus.DISCONNECTED, null);
		}
		
		for (FrontlineMessage m : outbox) {
			m.setStatus(Status.FAILED);
			smsListener.outgoingMessageEvent(this, m);
		}
		LOG.trace("EXIT");
	}

	/**
	 * Sends directly the informed message list.
	 * 
	 * @param smsMessages
	 * @return true if there was no problem sending messages, false otherwise.
	 * @throws IOException 
	 */
	private void sendSmsListDirect(List<FrontlineMessage> smsMessages) throws IOException {
		LOG.trace("ENTER");

		try {
			cService.keepGsmLinkOpen();
			for (FrontlineMessage message : smsMessages) {
				LOG.debug("Sending [" + message.getTextContent() + "] to [" + message.getRecipientMsisdn() + "]");
				COutgoingMessage cMessage;

				// If it's a binary message, we set the encoding to send it.
				if (message.isBinaryMessage()) {
					cMessage = new COutgoingMessage(message.getRecipientMsisdn(), message.getBinaryContent());
					cMessage.setDestinationPort(message.getRecipientSmsPort());
				} else {
					cMessage = new COutgoingMessage(message.getRecipientMsisdn(), message.getTextContent());
				}

				// Do we require a Delivery Status Report?
				cMessage.setStatusReport(this.useDeliveryReports);

				// Ok, finished with the message parameters, now send it!
				try {
					cService.sendMessage(cMessage);
					if (cMessage.getRefNo() != -1) {
						message.setSmscReference(cMessage.getRefNo());
						message.setStatus(Status.SENT);
						if(LOG.isDebugEnabled()) LOG.debug("Message [" + message.getTextContent() + "] was sent to [" + message.getRecipientMsisdn() + "]");
					} else {
						//message not sent
						//failed to send
						message.setStatus(Status.FAILED);
						if(LOG.isDebugEnabled()) LOG.debug("Message [" + message + "] failed to send to [" + message.getRecipientMsisdn() + "]");
					}
				} catch(Exception ex) {
					message.setStatus(Status.FAILED);
					ex.printStackTrace();
					if(LOG.isInfoEnabled()) LOG.info("Message [" + message + "] failed to send to [" + message.getRecipientMsisdn() + "]", ex);
				} finally {
					smsListener.outgoingMessageEvent(this, message);
				}
			}
		} finally {
			for (FrontlineMessage m : smsMessages) {
				if (m.getStatus() == Status.PENDING) {
					outbox.add(m);
				}
			}
		}
		LOG.trace("EXIT");
	}

	public String getServiceIdentification() {
		return getMsisdn();
	}

	public String getDisplayPort() {
		return this.getPort();
	}

	private void log(CIncomingMessage msg) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("- From [" + msg.getOriginator() + "]"
					+ "\n -Message [" + msg.getText() + "]"
					+ "\n -ID [" + msg.getId() + "]"
					+ "\n -Mem Index [" + msg.getMemIndex() + "]"
					+ "\n -Mem Location [" + msg.getMemLocation() + "]"
					+ "\n -Message Encoding [" + msg.getMessageEncoding() + "]"
					+ "\n -Protocol ID [" + msg.getPid() + "]"
					+ "\n -Reference Number [" + msg.getRefNo() + "]"
					+ "\n -Type [" + msg.getType() + "]"
					+ "\n -Date [" + msg.getDate() + "]");
		}
	}
//> STATIC HELPER METHODS

	public CService getCService() {
		// FIXME there may be a better solution that this long term... have a good think about whether this should be accessible.
		return this.cService;
	}
	

}
