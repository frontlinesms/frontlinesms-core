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
package net.frontlinesms.messaging.sms;

import java.util.*;
import java.util.concurrent.*;

import serial.*;

import net.frontlinesms.CommUtils;
import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.data.domain.FrontlineMessage.Status;
import net.frontlinesms.data.domain.PersistableSettings;
import net.frontlinesms.events.EventBus;
import net.frontlinesms.events.EventObserver;
import net.frontlinesms.events.FrontlineEventNotification;
import net.frontlinesms.listener.SmsListener;
import net.frontlinesms.messaging.CommProperties;
import net.frontlinesms.messaging.sms.events.*;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;
import net.frontlinesms.messaging.sms.modem.SmsModem;
import net.frontlinesms.messaging.sms.modem.SmsModemStatus;

import org.apache.log4j.Logger;
import org.smslib.CIncomingMessage;
import org.smslib.handler.CATHandler;
import org.smslib.util.GsmAlphabet;

/**
 * SmsHandler should be run as a separate thread.
 * 
 * It handles the discovery of phones available on the system's COM ports, 
 * and also manages a pool of threads that handle the communication with as many phones as are found 
 * attached to the system.
 * 
 * Autodetection should take 30 seconds.
 * 
 * OUTGOING MESSAGES
 * When you send a new outgoing message through SmsHandler it is added to a stack of waiting messages, 
 * which it will then send to the waiting phones by turn, unless the messages are marked as being 
 * for a specific phone.
 * 
 * INCOMING MESSAGES
 * If you create SmsHandler and pass it an SmsListener, incoming messages will be reported as events 
 * to that listener. If you create the SmsHandler without the listener, the messages will just appear 
 * on the linked list of IncomingMessages, and the calling program must poll it for new messages.
 * 
 * Incoming messages are immediately removed from active phones, so if you close the program without 
 * storing the message, then you will have lost the message.
 * 
 * PHONE STATE:
 * When a phone handler is created on a port, it will attempt AT commands until it gets an OK from a modem.
 * A valid OK will make the phoneHandler set phonePresent to TRUE.
 * The PhoneHanler will then attempt to connect the full SMSLIB tools to it, to take it into 
 * connected=true state, from which you can actually send and recieve messages.
 * 
 * HTTP services:
 * In the future this will be extended to be able to interface with 
 * internet based SMS services via HTTP, to handle bulk messaging.
 * 
 * @author Ben Whitaker ben(at)masabi(dot)com
 * @author Alex Anderson alex(at)masabi(dot)com
 */
public class SmsServiceManager extends Thread implements SmsListener, EventObserver  {
	/** List of GSM 7bit text messages queued to be sent. */
	private final ConcurrentLinkedQueue<FrontlineMessage> gsm7bitOutbox = new ConcurrentLinkedQueue<FrontlineMessage>();
	/** List of UCS2 text messages queued to be sent. */
	private final ConcurrentLinkedQueue<FrontlineMessage> ucs2Outbox = new ConcurrentLinkedQueue<FrontlineMessage>();
	/** List of binary messages queued to be sent. */
	private final ConcurrentLinkedQueue<FrontlineMessage> binOutbox = new ConcurrentLinkedQueue<FrontlineMessage>();
	/** List of phone handlers that this manager is currently looking after. */
	private final ConcurrentMap<String, SmsModem> phoneHandlers = new ConcurrentHashMap<String, SmsModem>();
	/** List of SMS internet services, the map key being the database ID of their settings */
	private Map<Long, SmsInternetService> smsInternetServices = new  ConcurrentHashMap<Long, SmsInternetService>();

	/** Listener to be passed SMS Listener events from this */
	private final SmsListener smsListener;
	/** Listener for application events */
	private final EventBus eventBus;
	/** Flag indicating that the thread should continue running. */
	private boolean running;	
	/** If set TRUE, then thread will automatically try to connect to newly-detected devices. */ 
	private boolean autoConnectToNewPhones;
	private boolean refreshPhoneList;


	/**
	 * Set containing all serial numbers of discovered phones.  Necessary because bluetooth/USB
	 * devices may present multiple virtual COM ports to the app. 
	 */
	private final HashSet<String> connectedSerials = new HashSet<String>();
	private String[] portIgnoreList;
	/** Counter used for choosing which SMS device to send messages with next.
	 * TODO we should use different counters for different types of messages, and also
	 * for SMS internet services vs. phones. */
	private int globalDispatchCounter;

	private static Logger LOG = FrontlineUtils.getLogger(SmsServiceManager.class);

//> CONSTRUCTORS
	/**
	 * Create a polling-variant SMS Handler.
	 * To add a message listener, setSmsListener() should be called.
	 */
	public SmsServiceManager(SmsListener smsListener, EventBus eventBus) {
		super("SmsDeviceManager");
		this.eventBus = eventBus;
		this.eventBus.registerObserver(this);
		this.smsListener = smsListener;

		// Load the COMM properties file, and extract the IGNORE list from
		// it - this is a list of COM ports that should be ignored.		
		this.portIgnoreList = CommProperties.getInstance().getIgnoreList();
	}

//> INTERNAL/THREAD HANDLING METHODS
	public void run() {
		LOG.trace("ENTER");
		running = true;
		while (running) {
			// Sleep for a second to ensure lists are not constantly being reshuffled.  Processing dispatch
			// and received messages is not really time-critical, otherwise it might be worth sleeping for
			// less time.
			FrontlineUtils.sleep_ignoreInterrupts(1000);
			
			doRun();
		}
		LOG.trace("EXIT");
	}

	/**
	 * Run the looped behaviour from {@link #run()} once.
	 * This method is separated for simple, unthreaded unit testing.
	 * THREAD: SmsDeviceManager 
	 */
	void doRun() {
		if (refreshPhoneList) {
			LOG.debug("Refreshing phone list...");
			// N.B. why is this not using the value from autoConnectToNewPhones? 
			listComPortsAndOwners(autoConnectToNewPhones);
			refreshPhoneList = false;
		} else {
			dispatchSms(MessageType.GSM7BIT_TEXT);
			dispatchSms(MessageType.UCS2_TEXT);
			dispatchSms(MessageType.BINARY);
			processModemReceiving();
		}
	}

	/** Handle the steps necessary when disconnecting a modem. */
	private void handleDisconnect(SmsModem modem) {
		modem.disconnect();
	}
	
	/**
	 * list com ports, optionally find phones, and optionally connect to them
	 * @param autoDiscoverPhones - if false, the call will only enumerate COM ports and find the owners - not try to auto-detect phones
	 * @param connectToAllDiscoveredPhones - only works if findPhoneNames is true, and will try to not connect to duplicate connections to the same phone.
	 */
	public void refreshPhoneList(boolean autoConnectToNewPhones) {
		this.autoConnectToNewPhones = autoConnectToNewPhones;
		refreshPhoneList = true;
	}

	/**
	 * Scan through the COM ports this computer is displaying.
	 * When an unowned port is found, we initiate a PhoneHandler
	 * detect an AT device on this port. Ignore all non-serial
	 * ports and all ports whose names' appear on our "ignore"
	 * list.
	 * @param findPhoneNames
	 * @param connectToAllDiscoveredPhones
	 */
	public void listComPortsAndOwners(boolean connectToAllDiscoveredPhones) {
		LOG.trace("ENTER");
		Enumeration<CommPortIdentifier> portIdentifiers = CommUtils.getPortIdentifiers();
		
		if (!portIdentifiers.hasMoreElements()) {
			if(this.eventBus != null) {
				this.eventBus.notifyObservers(new NoSmsServicesConnectedNotification(false, false));
			}
		} else {
			LOG.debug("Getting ports...");
			while (portIdentifiers.hasMoreElements()) {
				requestConnect(portIdentifiers.nextElement(), connectToAllDiscoveredPhones);
			}
		}
		LOG.trace("EXIT");
	}

	/**
	 * Checks if a COM port should be ignored (rather than connected to).
	 * @param comPortName
	 * @return
	 */
	private boolean shouldIgnore(String comPortName) {
		for (String ig : portIgnoreList) {
			if (ig.equalsIgnoreCase(comPortName)) return true;
		}
		return false;
	}


	/**
	 * Request that an SMS with the specified text be sent to the requested
	 * number.
	 * @param targetNumber
	 * @param smsMessage
	 * @return the Message object 
	 */
	public void sendSMS(FrontlineMessage outgoingMessage) {
		LOG.trace("ENTER");
		outgoingMessage.setStatus(Status.OUTBOX);
		switch(MessageType.get(outgoingMessage)) {
		case BINARY:
			binOutbox.add(outgoingMessage);
			LOG.debug("Message added to binOutbox. Size is [" + binOutbox.size() + "]");
			break;
		case GSM7BIT_TEXT:
			gsm7bitOutbox.add(outgoingMessage);
			LOG.debug("Message added to gsm7bitOutbox. Size is [" + gsm7bitOutbox.size() + "]");
			break;
		case UCS2_TEXT:
			ucs2Outbox.add(outgoingMessage);
			LOG.debug("Message added to ucs2Outbox. Size is [" + ucs2Outbox.size() + "]");
			break;
		default: throw new IllegalStateException();
		}
		
		if (smsListener != null) smsListener.outgoingMessageEvent(null, outgoingMessage);
		LOG.trace("EXIT");
	}

	/**
	 * Remove the supplied message from outbox.
	 * @param deleted
	 */
	public void removeFromOutbox(FrontlineMessage deleted) {
		if(gsm7bitOutbox.remove(deleted)) {
			if(LOG.isDebugEnabled()) LOG.debug("Message [" + deleted + "] removed from gsm7bitOutbox. Size is [" + gsm7bitOutbox.size() + "]");
		} else if(ucs2Outbox.remove(deleted)) {
			if(LOG.isDebugEnabled()) LOG.debug("Message [" + deleted + "] removed from uc2Outbox. Size is [" + ucs2Outbox.size() + "]");
		} else if(binOutbox.remove(deleted)) {
			if(LOG.isDebugEnabled()) LOG.debug("Message [" + deleted + "] removed from binOutbox. Size is [" + binOutbox.size() + "]");
		} else {
			if(LOG.isInfoEnabled()) LOG.info("Attempt to delete message found in no outbox.");
		}
	}

	/**
	 * Flags the internal thread to stop running.
	 */
	public void stopRunning() {
		this.running = false;

		// Disconnect all phones.
		for(SmsModem p : phoneHandlers.values()) {
			p.setDetecting(false);
			p.setAutoReconnect(false);
			handleDisconnect(p);
		}
		
		// Stop all SMS Internet Services
		for(SmsInternetService service : this.smsInternetServices.values()) {
			service.stopService();
		}
		
		this.eventBus.unregisterObserver(this);
	}

//> EVENT HANDLING METHODS
	public void notify(FrontlineEventNotification notification) {
		if(notification instanceof SmsModemStatusNotification) {
			SmsModemStatus deviceStatus = ((SmsModemStatusNotification) notification).getStatus();
			LOG.debug("Event [" + deviceStatus + "]");
			
			SmsModem activeDevice = ((SmsModemStatusNotification) notification).getService();
			if(deviceStatus.equals(SmsModemStatus.DISCONNECTED)) {
				// A device has just disconnected.  If we aren't using the device for sending or receiving,
				// then we should just ditch it.  However, if we *are* actively using the device, then we
				// would probably want to attempt to reconnect.  Also, if we were previously connected to 
				// this device then we should now remove its serial number from the list of connected serials.
				if(!activeDevice.isDuplicate()) connectedSerials.remove(activeDevice.getSerial());
			} else if(deviceStatus.equals(SmsModemStatus.CONNECTING)) {
				// The max speed for this connection has been found.  If this connection
				// is a duplicate, we should set the duplicate flag to true.  Otherwise,
				// we may wish to reconnect.
				if (autoConnectToNewPhones) {
					boolean isDuplicate = !connectedSerials.add(activeDevice.getSerial());
					activeDevice.setDuplicate(isDuplicate);
					if(!isDuplicate) activeDevice.connect();
				}
			}
			
			if (isFailedStatus(deviceStatus)) {
				eventBus.notifyObservers(createNoSmsDevicesConnectedNotification());
			}
		}
	}
	
	public void incomingMessageEvent(SmsService receiver, CIncomingMessage msg) {
		// If we've got a higher-level listener attached to this, pass the message 
		// up to there.  Otherwise, add it to our internal list
		if (smsListener != null) smsListener.incomingMessageEvent(receiver, msg);
	}

	public void outgoingMessageEvent(SmsService sender, FrontlineMessage msg) {
		if (smsListener != null) smsListener.outgoingMessageEvent(sender, msg);
		if (msg.getStatus() == Status.FAILED) {
			if (msg.getRetriesRemaining() > 0) {
				msg.setRetriesRemaining(msg.getRetriesRemaining() - 1);
				msg.setSenderMsisdn("");
				sendSMS(msg);
			}
		}
	}

	public boolean hasPhoneConnected(String port) {
		SmsService phoneHandler = phoneHandlers.get(port);
		return phoneHandler != null && phoneHandler.isConnected();
	}

	/**
	 * Creates a {@link NoSmsServicesConnectedNotification} based on the current status of attached.  If any devices
	 * are connected or still processing, a notification is not created.
	 * {@link SmsService}s.
	 * @return a {@link NoSmsServicesConnectedNotification} describing the current lack of connected devices, or <code>null</code> if there are devices connected or in the process of connecting.
	 */
	private NoSmsServicesConnectedNotification createNoSmsDevicesConnectedNotification() {
		// Check if all other devices have finished detecting.  If that's the case, and no
		// devices have been detected, we throw a NoSmsDevicesDetectedNotification.
		boolean incompatibleDevicesDetected = false;
		boolean ownedPortsDetected = false;
		boolean deviceDetectedOrDetectionInProgress = false;
		
		checkAll:for (SmsService device : getAll()) {
			if(device instanceof SmsModem) {
				SmsModemStatus status = ((SmsModem)device).getStatus();
				switch(status) {
				case FAILED_TO_CONNECT:
				case GSM_REG_FAILED:
				case DISCONNECTED:
					incompatibleDevicesDetected = true;
					break;
					
				case OWNED_BY_SOMEONE_ELSE:
					ownedPortsDetected = true;
					break;
					
				case CONNECTED:
				case CONNECTING:
				case DETECTED:
				case DISCONNECTING:
				case DORMANT:
				case MAX_SPEED_FOUND:
				case SEARCHING:
				case TRY_TO_CONNECT:
					deviceDetectedOrDetectionInProgress = true;
					break checkAll;

				case DISCONNECT_FORCED:
				case NO_PHONE_DETECTED:
				case DUPLICATE:
					// ignore this
					break;
				}
			} else if(device instanceof SmsInternetService) {
				switch(((SmsInternetService)device).getStatus()) {
				case CONNECTED:
				case CONNECTING:
				case DORMANT:
				case LOW_CREDIT:
				case TRYING_TO_RECONNECT:
					deviceDetectedOrDetectionInProgress = true;
					break checkAll;

				case RECEIVING_FAILED:
					// ignore this as it's not really relevant here
					break;
					
				case DISCONNECTED:
				case FAILED_TO_CONNECT:
					// ignore this - we only prompt to help connect phones, not internet services
					break;
				}
			}
		}
		
		if(deviceDetectedOrDetectionInProgress) {
			return null;
		} else {
			return new NoSmsServicesConnectedNotification(incompatibleDevicesDetected, ownedPortsDetected);
		}
	}

	/**
	 * Check if the given {@link SmsServiceStatus} belongs to the failed statuses which should show the device connection problem dialog 
	 * @param deviceStatus
	 * @return <code>true</code> if the {@link SmsService} is a failed status, <code>false</code> otherwise
	 */
	private static boolean isFailedStatus(SmsServiceStatus<?> deviceStatus) {
		return deviceStatus.equals(SmsModemStatus.OWNED_BY_SOMEONE_ELSE)
				|| deviceStatus.equals(SmsModemStatus.NO_PHONE_DETECTED)
				|| deviceStatus.equals(SmsModemStatus.GSM_REG_FAILED)
				|| deviceStatus.equals(SmsModemStatus.FAILED_TO_CONNECT);
	}

	/**
	 * Get's all {@link SmsService}s that this manager is currently connected to
	 * or investigating.
	 * @return
	 */
	public Collection<SmsService> getAll() {
		Set<SmsService> ret = new HashSet<SmsService>();
		ret.addAll(phoneHandlers.values());
		ret.addAll(smsInternetServices.values());
		return ret;
	}

	/**
	 * Request the phone manager to attempt a connection to a particular COM port.
	 * @param port
	 */
	public boolean requestConnect(String port) throws NoSuchPortException {
		return requestConnect(CommPortIdentifier.getPortIdentifier(port), null, true);
	}

	/**
	 * Request the phone manager to attempt a connection to a particular COM port.
	 * @param port
	 * @param simPin the PIN to use when connecting to the phone
	 */
	public boolean requestConnect(String port, String simPin) throws NoSuchPortException {
		return requestConnect(CommPortIdentifier.getPortIdentifier(port), simPin, true);
	}

	/**
	 * <p>Attempt to connect to an {@link SmsModem} on a particular COM port.  This method allows you to specify
	 * the preffered {@link CATHandler} to be used.</p>
	 * <p>If the port is already in use then connection to the port will not be attempted.</p>
	 * @param portName
	 * @param baudRate
	 * @param preferredCATHandler
	 * @return <code>true</code> if connection is being attempted to the port; <code>false</code> if the port is already in use.
	 * @throws NoSuchPortException
	 */
	public boolean requestConnect(String portName, String simPin, int baudRate, String preferredCATHandler) throws NoSuchPortException {
		CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);

		if(LOG.isInfoEnabled()) LOG.info("Requested connection to port: '" + portName + "'");
		if(!portIdentifier.isCurrentlyOwned()) {
			LOG.info("Connecting to port...");
			SmsModem phoneHandler = new SmsModem(portName, this, eventBus);
			phoneHandler.setSimPin(simPin);
			phoneHandlers.put(portName, phoneHandler);
			phoneHandler.start(baudRate, preferredCATHandler);
			return true;
		} else {
			LOG.info("Port currently owned by another process: '" + portIdentifier.getCurrentOwner() + "'");
			// If we don't have a handle on this port, but it's owned by someone else,
			// then we add it to the phoneHandlers list anyway so that we can see its
			// status.
			phoneHandlers.putIfAbsent(portName, new SmsModem(portName, this, eventBus));
			return false;
		}
	}

	public void addSmsInternetService(PersistableSettings settings) {
		try {
			SmsInternetService service = (SmsInternetService) settings.getServiceClass().newInstance();
			service.setSettings(settings);
			service.setSmsListener(this);
			service.setEventBus(eventBus);
			smsInternetServices.put(settings.getId(), service);
			service.startService();
		} catch(IllegalAccessException ex) {
			handleServiceLoadException(settings, ex);
		} catch (InstantiationException ex) {
			handleServiceLoadException(settings, ex);
		}
	}
	
	public void restartSmsInternetService(PersistableSettings settings) {
		SmsInternetService service = smsInternetServices.get(settings.getId());
		service.setSettings(settings);
		service.restartService();
	}
	
	private void handleServiceLoadException(PersistableSettings settings, Exception ex) {
		LOG.warn("Could not create srvice with class " + settings.getServiceClass() + " for settings with ID " + settings.getId(), ex);
	}

	/**
	 * Remove a service from this {@link SmsServiceManager}.
	 * @param settings
	 */
	public void removeSmsInternetService(PersistableSettings settings) {
		SmsInternetService service = smsInternetServices.remove(settings.getId());
		disconnectSmsInternetService(service);
	}
	
	public void disconnect(SmsService service) {
		if(service instanceof SmsModem) disconnectPhone((SmsModem) service);
		else if(service instanceof SmsInternetService) disconnectSmsInternetService((SmsInternetService) service);
	}

	private void disconnectPhone(SmsModem modem) {
		modem.setAutoReconnect(false);
		handleDisconnect(modem);
	}

	public void stopDetection(String port) {
		SmsModem smsModem = phoneHandlers.get(port);
		if(smsModem != null) {
			smsModem.setDetecting(false);
			smsModem.setAutoReconnect(false);
		}
	}

	private void disconnectSmsInternetService(SmsInternetService service) {
		service.stopService();
	}

	/**
	 * Attempts to connect to the supplied comm port
	 * @param portIdentifier
	 * @param connectToDiscoveredPhone
	 * @param findPhoneName
	 * @return <code>true</code> if connection is being attempted to the port; <code>false</code> if the port is already in use or does not exist or is not a serial port.
	 */
	private boolean requestConnect(CommPortIdentifier portIdentifier, boolean connectToDiscoveredPhones) {
		return requestConnect(portIdentifier, null, connectToDiscoveredPhones);
	}
	
	/**
	 * Attempts to connect to the supplied comm port
	 * @param portIdentifier
	 * @param connectToDiscoveredPhone
	 * @param findPhoneName
	 * @return <code>true</code> if connection is being attempted to the port; <code>false</code> if the port is already in use or does not exist or is not a serial port.
	 */
	private boolean requestConnect(CommPortIdentifier portIdentifier, String simPin, boolean connectToDiscoveredPhones) {
		String portName = portIdentifier.getName();
		LOG.debug("Port Name [" + portName + "]");
		if(!shouldIgnore(portName) && portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
			LOG.debug("It is a suitable port.");
			try {
				SmsModem modem = new SmsModem(portName, this, eventBus);
				modem.setSimPin(simPin);
				if(!portIdentifier.isCurrentlyOwned()) {
					LOG.debug("Connecting to port...");
					phoneHandlers.put(portName, modem);
					if(connectToDiscoveredPhones) modem.start();
					return true;
				} else {
					// If we don't have a handle on this port, but it's owned by someone else,
					// then we add it to the phoneHandlers list anyway so that we can see its
					// status.
					LOG.debug("Port currently owned by another process.");
					phoneHandlers.putIfAbsent(portName, modem);
					return false;
				}
			} catch(NoSuchPortException ex) {
				LOG.warn("Port is no longer available.", ex);
				return false;
			}
		} else {
			// Requesting to connect to a parallel port.  Not possible, apparently.
			// TODO throw a BadPortException or something
			return false;
		}
	}

	public Collection<SmsInternetService> getSmsInternetServices() {
		return this.smsInternetServices.values();
	}
	
	public Collection<SmsModem> getSmsModems() {
		return this.phoneHandlers.values();
	}

	/**
	 * Polls all {@link SmsModem}s that are set to receive messages, and processes any
	 * messages they've received.
	 * THREAD: SmsDeviceManager
	 */
	private void processModemReceiving() {
		Collection<SmsModem> receiveModems = getSmsModemsForReceiving();
		for(SmsModem modem : receiveModems) {
			CIncomingMessage receivedMessage;
			while((receivedMessage = modem.nextIncomingMessage()) != null) {
				incomingMessageEvent(modem, receivedMessage);
			}
		}
	}
	
	/** @return all {@link SmsModem}s that are currently connected and receiving messages.
	 * THREAD: SmsDeviceManager */
	private Collection<SmsModem> getSmsModemsForReceiving() {
		HashSet<SmsModem> receivers = new HashSet<SmsModem>();
		for(SmsModem modem : this.phoneHandlers.values()) {
			if(modem.isRunning() && modem.isTimedOut()) {
				// The phone's being unresponsive.  Attempt to disconnect from the phone, remove the serial
				// number from the duplicates list and then add the phone to the reconnect list so we can
				// reconnect to it later.  We should also remove the unresponsive phone from the phoneHandlers
				// list.
				if(LOG.isDebugEnabled()) LOG.debug("Watchdog from phone [" + modem.getPort() + "] has timed out! Disconnecting...");
				handleDisconnect(modem);
			} else if(modem.isConnected() && modem.isUseForReceiving()) {
				receivers.add(modem);
			}
		}
		return receivers;
	}

//> SMS DISPATCH METHODS
	
	/**
	 * @param messageType The type of messages which should be dispatched.
	 * The right list is chosen using this type.
	 * THREAD: SmsDeviceManager
	 */
	private void dispatchSms(MessageType messageType) {
		ConcurrentLinkedQueue<FrontlineMessage> outboxFromType = getOutboxFromType(messageType);
		List<FrontlineMessage> messages = removeAll(outboxFromType);
		if(messages.size() > 0) {
			// Try dispatching to SmsInternetServices
			List<SmsInternetService> internetServices = getSmsInternetServicesForSending(messageType);
			int serviceCount = internetServices.size();
			if(serviceCount > 0) {
				// We have some SMS Internet services to send with.  These are assumed to be higher priority
				// than Sms Modems, so send all messages with the internet services.
				dispatchSms(internetServices, messages);
			} else {
				// There are no available SMS Internet Services, so dispatch to SmsModems
				List<SmsModem> sendingModems = getSmsModemsForSending(messageType);
				if(sendingModems.size() > 0) {
					dispatchSms(sendingModems, messages);
				} else {
					// The messages cannot be sent
					// We put them back in their outbox 
					outboxFromType.addAll(messages);
				}
			}		
		}
	}
	
	/**
	 * 
	 * @param messageType The {@link MessageType}
	 * @return The outbox corresponding to the {@link MessageType}
	 */
	private ConcurrentLinkedQueue<FrontlineMessage> getOutboxFromType(MessageType messageType) {
		switch (messageType) {
			case BINARY:
				return binOutbox;
			case UCS2_TEXT:
				return ucs2Outbox;
			case GSM7BIT_TEXT:
				return gsm7bitOutbox;
			default: throw new IllegalStateException("Unrecognized message type: " + messageType);
		}
	}

	/**
	 * Dispatch some SMS {@link FrontlineMessage}s to some {@link SmsService}s. 
	 * @param devices
	 * @param messages
	 * THREAD: SmsDeviceManager
	 */
	private void dispatchSms(List<? extends SmsService> devices, List<FrontlineMessage> messages) {
		int deviceCount = devices.size();
		for(FrontlineMessage m : messages) {
			SmsService device = devices.get(++globalDispatchCounter % deviceCount);
			// Presumably the device will complain somehow if it is no longer connected
			// etc.  TODO we should actually check what happens!
			device.sendSMS(m);
			outgoingMessageEvent(device, m);
		}
	}

	/** Removes and returns all messages currently available in a list. */
	private List<FrontlineMessage> removeAll(ConcurrentLinkedQueue<FrontlineMessage> outbox) {
		LinkedList<FrontlineMessage> retrieved = new LinkedList<FrontlineMessage>();
		FrontlineMessage m;
		while((m=outbox.poll())!=null) retrieved.add(m);
		return retrieved;
	}

	/** @return all {@link SmsInternetService} which are available for sending messages. */
	private List<SmsInternetService> getSmsInternetServicesForSending(MessageType messageType) {
		ArrayList<SmsInternetService> senders = new ArrayList<SmsInternetService>();
		for(SmsInternetService service : this.smsInternetServices.values()) {
			if(service.isConnected() && service.isUseForSending()) {
				boolean addService;
				switch(messageType) {
					case BINARY:
						addService = service.isBinarySendingSupported();
						break;
					case UCS2_TEXT:
						addService = service.isUcs2SendingSupported();
						break;
					case GSM7BIT_TEXT:
						addService = true;
						break;
					default: throw new IllegalStateException();
				}
				if(addService) senders.add(service);
			}
		}
		return senders;
	}
	
	/** @return all {@link SmsModem} which are available for sending messages. */
	private List<SmsModem> getSmsModemsForSending(MessageType messageType) {
		ArrayList<SmsModem> senders = new ArrayList<SmsModem>();
		for(SmsModem modem : this.phoneHandlers.values()) {
			if(modem.isRunning() && modem.isTimedOut()) {
				// The phone's being unresponsive.  Attempt to disconnect from the phone, remove the serial
				// number from the duplicates list and then add the phone to the reconnect list so we can
				// reconnect to it later.  We should also remove the unresponsive phone from the phoneHandlers
				// list.
				if(LOG.isDebugEnabled()) LOG.debug("Watchdog from phone [" + modem.getPort() + "] has timed out! Disconnecting...");
				handleDisconnect(modem);
			} else if(modem.isConnected() && modem.isUseForSending()) {
				boolean addModem;
				switch(messageType) {
					case BINARY:
						addModem = modem.isBinarySendingSupported();
						break;
					case UCS2_TEXT:
						addModem = modem.isUcs2SendingSupported();
						break;
					case GSM7BIT_TEXT:
						addModem = true;
						break;
					default: throw new IllegalStateException();
				}
				if(addModem) senders.add(modem);
			}
		}
		return senders;
	}

	/**
	 * @return The number of active SMS connections running
	 */
	public int getNumberOfActiveConnections() {
		int total = 0;

		for(SmsModem modem : this.phoneHandlers.values()) {
			if (modem.isConnected()) {
				++total;
			}
		}
		
		for (SmsInternetService service : this.smsInternetServices.values()) {
			if (service.isConnected()) {
				++total;
			}
		}		
		
		// NB: this may be cleaner if using FrontlineMessagingServices,
		// but it doesn't sound really useful right now.
		return total;
	}
}

enum MessageType {
	GSM7BIT_TEXT,
	UCS2_TEXT,
	BINARY;
	
	public static MessageType get(FrontlineMessage message) {
		if(message.isBinaryMessage()) {
			return BINARY;
		} else if(GsmAlphabet.areAllCharactersValidGSM(message.getTextContent())) {
			return GSM7BIT_TEXT;
		} else {
			return UCS2_TEXT;
		}
	}
}
