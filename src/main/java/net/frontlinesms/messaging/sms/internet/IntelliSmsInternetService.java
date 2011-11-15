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
package net.frontlinesms.messaging.sms.internet;

import java.util.*;

import net.frontlinesms.*;
import net.frontlinesms.data.domain.*;
import net.frontlinesms.data.domain.FrontlineMessage.Status;
import net.frontlinesms.email.receive.*;
import net.frontlinesms.serviceconfig.OptionalSection;
import net.frontlinesms.serviceconfig.PasswordString;
import net.frontlinesms.serviceconfig.PhoneSection;
import net.frontlinesms.serviceconfig.ConfigurableServiceProperties;
import net.frontlinesms.serviceconfig.StructuredProperties;
import net.frontlinesms.ui.handler.settings.SmsInternetServiceSettingsHandler;

import org.apache.log4j.Logger;
import org.smslib.CIncomingMessage;
import org.smslib.util.GsmAlphabet;
import org.smslib.util.HexUtils;
import org.smslib.util.TpduUtils;

import IntelliSoftware.SMSGateway.SDK.IntelliSMSJavaSDK.*;

/**
 * Implements the IntelliSms Internet service.
 *
 * @author Carlos Eduardo Endler Genz
 * @date 31/01/2009
 */
@ConfigurableServiceProperties(name = "IntelliSms", icon = "/icons/sms_http.png") 
public class IntelliSmsInternetService extends AbstractSmsInternetService implements EmailReceiveProcessor {
	
//> STATIC CONSTANTS
	/** Default source port to use for an outgoing binary SMS message */
	private static final int DEFAULT_SOURCE_PORT = 0;
	
	/** IntelliSMS defines its maximum SMS parts as 10.  Specifying more than this will lead to a ERR:PARAMETER_INVALID when
	 * querying the HTTP interface. */
	private static final int MAX_SMS_PARTS = 10;

	/** Prefix attached to every property name. */
	private static final String PROPERTY_PREFIX = "smsdevice.internet.intellisms.";
	
	protected static final String PROPERTY_USERNAME = PROPERTY_PREFIX + "username";
	protected static final String PROPERTY_PASSWORD = PROPERTY_PREFIX + "password";
	protected static final String PROPERTY_FROM_MSISDN = PROPERTY_PREFIX + "from.msisdn";
	protected static final String PROPERTY_SSL = PROPERTY_PREFIX + "ssl";

	protected static final String PROPERTY_RECEIVING_EMAIL_HOST = PROPERTY_PREFIX + "email.host";
	protected static final String PROPERTY_RECEIVING_EMAIL_HOST_PORT = PROPERTY_PREFIX + "email.host.port";
	protected static final String PROPERTY_RECEIVING_EMAIL_HOST_SSL = PROPERTY_PREFIX + "email.host.ssl";
	protected static final String PROPERTY_RECEIVING_EMAIL_USERNAME = PROPERTY_PREFIX + "email.username";
	protected static final String PROPERTY_RECEIVING_EMAIL_PASSWORD = PROPERTY_PREFIX + "email.password";
	
	private static final String PROPERTY_PROXY_ENABLED = PROPERTY_PREFIX + "proxy.enabled";
	private static final String PROPERTY_PROXY_ADDRESS = PROPERTY_PREFIX + "proxy.address";
	private static final String PROPERTY_PROXY_USERNAME = PROPERTY_PREFIX + "proxy.username";
	private static final String PROPERTY_PROXY_PASSWORD = PROPERTY_PREFIX + "proxy.password";
	/**
	 * Currently the IntelliSMS code includes proxy options, but no actual implementation to make this work.
	 * If they do ever implement them, we can activate, or better remove, this constant.
	 */
	private static final boolean PROXIES_SUPPORTED = false;

	/** Logging object */
	private static Logger LOG = FrontlineUtils.getLogger(IntelliSmsInternetService.class);

//> INSTANCE PROPERTIES
	private IntelliSMS intelliSMS;
	private boolean connected;

	/**
	 * Initialises the intellisms sdk gateway.
	 */
	private synchronized void initGateway() {
		intelliSMS = new IntelliSMS();
		intelliSMS.Username = getUsername();
		intelliSMS.Password = getPassword();
		intelliSMS.MaxConCatMsgs = MAX_SMS_PARTS;
		if (isEncrypted()) {
			intelliSMS.PrimaryGateway="https://www.intellisoftware.co.uk";
			intelliSMS.BackupGateway="https://www.intellisoftware2.co.uk";
		} else {
			intelliSMS.PrimaryGateway="http://www.intellisoftware.co.uk";
			intelliSMS.BackupGateway="http://www.intellisoftware2.co.uk";
		}
	}

	/**
	 * Verify if we this services has received messages.
	 * @return whether receive was successful
	 */
	protected synchronized void receiveSms() throws SmsInternetServiceReceiveException {
		EmailReceiver receiver = new EmailReceiver(this);
		
		receiver.setHostAddress(getEmailHost());
		receiver.setHostPassword(getEmailPassword());
		receiver.setHostPort(getEmailHostPort());
		receiver.setHostUsername(getEmailUsername());
		receiver.setUseSsl(getEmailHostSSL());
		receiver.setProtocol(EmailReceiveProtocol.POP3);
		
		try {
			receiver.receive();
		} catch(EmailReceiveException ex) {
			throw new SmsInternetServiceReceiveException(ex);
		}
	}
	
	public void processMessage(javax.mail.Message message, Date date) {
		try {
			// We've got a message, so try and find it's text content.
			String messageText = EmailReceiveUtils.getMessageText(message);
			// For now, if the message text was null then set it to an empty string
			if(messageText == null) messageText = "";
			this.processPopMessage(EmailReceiveUtils.getSender(message), message.getSentDate().getTime(), message.getSubject(), messageText);
		} catch (Exception ex) {
			LOG.warn("Error processing email.");
		}
	}
	
	public void processPopMessage(String sender, long messageSentDate, String subject, String messageText) {
		if (subject.startsWith("Message from ")) {
			String from = subject.split(" ")[2];
			LOG.debug("Message from [" + from + "]");
			LOG.debug("The message content [" + messageText + "]");
			// Sender might be blank in this case.
			if (!from.equals(sender)) {
				sender = from;
			}
			CIncomingMessage cMessage = new CIncomingMessage(messageSentDate, sender, messageText);
			smsListener.incomingMessageEvent(this, cMessage);
		} else if (subject.startsWith("Status report for message sent to")) {
			// We've got a delivery report
			// TODO we need to get the msg id. Since it is not in the email.
			// We can get delivery reports polling IntelliSMS server with
			// the messages id.
			LOG.trace("Received status report.  Not currently handled.");
		} else {
			LOG.trace("Unrecognized message subject '"+subject+"'");
		}
	}

	/**
	 * Send an SMS message using this phone handler.
	 * @param message The message to be sent.
	 */
	protected synchronized void sendSmsDirect(FrontlineMessage message) {
		LOG.trace("ENTER");
		LOG.debug("Sending [" + message.getTextContent() + "] to [" + message.getRecipientMsisdn() + "]");
		try {
			ResultCodes code;
			if (message.isBinaryMessage()) {
				code = sendBinarySms(message);
			} else if(!GsmAlphabet.areAllCharactersValidGSM(message.getTextContent())) {
				code = sendUcs2Sms(message);
			} else {
				if ( (message.getTextContent().length() / FrontlineMessage.SMS_LENGTH_LIMIT) > MAX_SMS_PARTS) {
					LOG.error("Message too long. [" + message.getTextContent().length() + "] chars.");
				}
				SendStatusCollection results = intelliSMS.SendMessage(new String[] {message.getRecipientMsisdn()}, message.getTextContent(), getMsisdn());
				code = results.OverallResultCode;
			}
			LOG.debug("Code is [" + code + "]");
			if (code == ResultCodes.OK) {
				message.setStatus(Status.SENT);
				LOG.debug("Message [" + message + "] was sent!");
			} else {
				if (code == ResultCodes.InsufficientCredit) {
					setStatus(SmsInternetServiceStatus.LOW_CREDIT, Integer.toString(getRemainingCredit()));
				}
				message.setStatus(Status.FAILED);
				LOG.debug("Message [" + message + "] was not sent.  Cause: [" + code + "]");
			}
		} catch (IntelliSMSException e) {
			message.setStatus(Status.FAILED);
			LOG.debug("Failed to send message [" + message + "]: " + e.getResultCode(), e);
			LOG.info("Failed to send message: " + e.getResultCode());
			
			if(ResultCodes.InsufficientCredit.equals(e.getResultCode())) {
				int remainingCredit;
				try {
					remainingCredit = getRemainingCredit();
				} catch (IntelliSMSException e1) {
					remainingCredit = -1;
				}
				
				this.stopService();
				setStatus(SmsInternetServiceStatus.LOW_CREDIT, Integer.toString(remainingCredit));
			} else {
				this.stopService();
				this.setStatus(SmsInternetServiceStatus.DISCONNECTED, e.getResultCode() + ": " + e.getMessage());
			}
		} finally {
			if (smsListener != null) {
				smsListener.outgoingMessageEvent(this, message);
			}
		}
		LOG.trace("EXIT");
	}

	private ResultCodes sendBinarySms(FrontlineMessage message) throws IntelliSMSException {
		LOG.trace("ENTER");
		byte[][] messagePayloads = TpduUtils.getPayloads(message.getBinaryContent(), DEFAULT_SOURCE_PORT, message.getRecipientSmsPort());
		int totalParts = messagePayloads.length;
		LOG.debug("Total parts [" + totalParts + "]");
		ResultCodes ret = ResultCodes.OK;
		for (int i = 1; i <= totalParts; i++) {
			SendStatusCollection results = intelliSMS.SendBinaryMessage(
					new String[]{ message.getRecipientMsisdn() },
					HexUtils.encode(TpduUtils.generateUDH(i, totalParts, 0, DEFAULT_SOURCE_PORT, message.getRecipientSmsPort())),
					HexUtils.encode(messagePayloads[i-1]),
					getMsisdn());
			ret = results.OverallResultCode;
			LOG.debug("Executing part [" + i + "], result code is [" + ret + "].");
		}
		LOG.trace("EXIT");
		return ret;
	}

	private ResultCodes sendUcs2Sms(FrontlineMessage message) throws IntelliSMSException {
		LOG.trace("ENTER");
		String[] messageParts = TpduUtils.splitText_ucs2(message.getTextContent(), false);
		int totalParts = messageParts.length;
		LOG.debug("Total parts [" + totalParts + "]");
		SendStatusCollection results = intelliSMS.SendUnicodeMessage(new String[]{message.getRecipientMsisdn()}, message.getTextContent(), getMsisdn());
		ResultCodes ret = results.OverallResultCode;
		LOG.trace("EXIT");
		return ret;
	}

	/**
	 * Starts the service. Normally we initialise the gateway.
	 * @throws SmsInternetServiceInitialisationException 
	 */
	protected void init() throws SmsInternetServiceInitialisationException {
		initGateway();
		try {
			getRemainingCredit();
			connected = true;
			this.setStatus(SmsInternetServiceStatus.CONNECTED, null);
		} catch (IntelliSMSException e) {
			LOG.debug("Could not connect", e);
			connected = false;
			this.setStatus(SmsInternetServiceStatus.FAILED_TO_CONNECT, e.getMessage());
			throw new SmsInternetServiceInitialisationException(e);
		}
	}

	/** 
	 * Get the default properties for this class. 
	 */
	public StructuredProperties getPropertiesStructure() {
		StructuredProperties defaultSettings = new StructuredProperties();
		defaultSettings.put(PROPERTY_USERNAME, "");
		defaultSettings.put(PROPERTY_PASSWORD, new PasswordString(""));
		defaultSettings.put(PROPERTY_FROM_MSISDN, new PhoneSection(""));
		defaultSettings.put(PROPERTY_SSL, Boolean.FALSE);
		defaultSettings.put(PROPERTY_USE_FOR_SENDING, Boolean.TRUE);
		// Proxy properties
		if(PROXIES_SUPPORTED) {
			OptionalSection section = new OptionalSection();
			section.setValue(false);
			section.addDependency(PROPERTY_PROXY_ADDRESS, "");
			section.addDependency(PROPERTY_PROXY_USERNAME, "");
			section.addDependency(PROPERTY_PROXY_PASSWORD, new PasswordString(""));
			defaultSettings.put(PROPERTY_PROXY_ENABLED, section);
		}
		// Receiving properties
		OptionalSection section = new OptionalSection();
		section.setValue(false);
		section.addDependency(PROPERTY_RECEIVING_EMAIL_HOST, "");
		section.addDependency(PROPERTY_RECEIVING_EMAIL_HOST_PORT, new Integer(995));
		section.addDependency(PROPERTY_RECEIVING_EMAIL_HOST_SSL, Boolean.TRUE);
		section.addDependency(PROPERTY_RECEIVING_EMAIL_USERNAME, "");
		section.addDependency(PROPERTY_RECEIVING_EMAIL_PASSWORD, new PasswordString(""));
		defaultSettings.put(PROPERTY_USE_FOR_RECEIVING, section);
		return defaultSettings;
	}

	/** 
	 * Gets the MSISDN that numbers sent from this service will appear to be from. 
	 */
	public String getMsisdn() {
		return getPropertyValue(PROPERTY_FROM_MSISDN, PhoneSection.class).getValue();
	}

	/**
	 * Gets an identifier for this instance of {@link SmsInternetService}.  Usually this
	 * will be the username used to login with the provider, or a similar identifer on
	 * the service.
	 */
	public String getIdentifier() {
		return this.getUsername();
	}

	/**
	 * @return The property value of {@value #PROPERTY_USERNAME}
	 */
	private String getUsername() {
		return getPropertyValue(PROPERTY_USERNAME, String.class);
	}

	/**
	 * @return The property value of {@value #PROPERTY_PASSWORD}
	 */
	private String getPassword() {
		return getPropertyValue(PROPERTY_PASSWORD, PasswordString.class).getValue();
	}

	/**
	 * @return The property value of {@value #PROPERTY_RECEIVING_EMAIL_PASSWORD}
	 */
	private String getEmailPassword() {
		return getPropertyValue(PROPERTY_RECEIVING_EMAIL_PASSWORD, PasswordString.class).getValue();
	}

	/**
	 * @return The property value of {@value #PROPERTY_RECEIVING_EMAIL_HOST}
	 */
	private String getEmailHost() {
		return getPropertyValue(PROPERTY_RECEIVING_EMAIL_HOST, String.class);
	}

	/**
	 * @return The property value of {@value #PROPERTY_RECEIVING_EMAIL_HOST_PORT}
	 */
	private Integer getEmailHostPort() {
		return getPropertyValue(PROPERTY_RECEIVING_EMAIL_HOST_PORT, Integer.class);
	}

	/**
	 * @return The property value of {@value #PROPERTY_RECEIVING_EMAIL_HOST_SSL}
	 */
	private boolean getEmailHostSSL() {
		return getPropertyValue(PROPERTY_RECEIVING_EMAIL_HOST_SSL, Boolean.class);
	}

	/**
	 * @return The property value of {@value #PROPERTY_RECEIVING_EMAIL_USERNAME}
	 */
	private String getEmailUsername() {
		return getPropertyValue(PROPERTY_RECEIVING_EMAIL_USERNAME, String.class);
	}

	/**
	 * Checks if the service is currently connected.
	 * TODO could rename this isLive().
	 * @return
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * @return The remaining credits of this accounts.
	 * @throws IntelliSMSException If something goes wrong connecting to intellisms.
	 */
	private int getRemainingCredit() throws IntelliSMSException {
		return intelliSMS.GetBalance();
	}

	/** 
	 * Check if this service is encrypted using SSL. 
	 */
	public boolean isEncrypted() {
		return getPropertyValue(PROPERTY_SSL, Boolean.class);
	}

	/**
	 * Check if this device is being used to receive SMS messages.
	 * Our implementation of IntelliSms can support SMS receiving.
	 */
	public boolean isUseForReceiving() {
		return getPropertyValue(PROPERTY_USE_FOR_RECEIVING, Boolean.class);
	}

	/** 
	 * Checks if this device is being used to send SMS messages. 
	 */
	public boolean isUseForSending() {
		return getPropertyValue(PROPERTY_USE_FOR_SENDING, Boolean.class);
	}

	/** 
	 * Set this device to be used for receiving messages. 
	 */
	public void setUseForReceiving(boolean use) {
		setProperty(PROPERTY_USE_FOR_RECEIVING, new Boolean(use));
	}

	/** 
	 * Set this device to be used for sending SMS messages. 
	 */
	public void setUseForSending(boolean use) {
		this.setProperty(PROPERTY_USE_FOR_SENDING, new Boolean(use));
	}

	/**
	 * We do support receive through IntelliSMS (pop3).
	 * @return <code>true</code>
	 */
	public boolean supportsReceive() {
		return true;
	}

	/** 
	 * Check whether this device actually supports sending binary sms. 
	 */
	public boolean isBinarySendingSupported() {
		return true;
	}

	/**
	 * TODO this needs testing.  If it works, this method can return true.  If it doesn't work,
	 * this comment can be replaced with a more descriptive one.
	 */
	public boolean isUcs2SendingSupported() {
		return false;
	}

	@Override
	protected void deinit() {
		connected = false;
		this.setStatus(SmsInternetServiceStatus.DISCONNECTED, null);
	}
	
	public String getServiceName() {
		return this.getUsername() + UI_NAME_SEPARATOR + SmsInternetServiceSettingsHandler.getProviderName(getClass());
	}

	public String getPort() {
		return null;
	}

	public String getDisplayPort() {
		// TODO Auto-generated method stub
		return null;
	}

//> STATIC HELPER METHODS
}
