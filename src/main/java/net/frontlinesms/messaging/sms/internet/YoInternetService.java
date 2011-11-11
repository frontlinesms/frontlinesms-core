/*
 * FrontlineSMS <http://www.frontlinesms.com>
 * Copyright 2011 kiwanja
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

import java.io.IOException;
import java.util.LinkedHashMap;

import javax.xml.parsers.ParserConfigurationException;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.data.domain.FrontlineMessage.Status;
import net.frontlinesms.serviceconfig.PasswordString;
import net.frontlinesms.serviceconfig.PhoneSection;
import net.frontlinesms.serviceconfig.ConfigurableServiceProperties;
import net.frontlinesms.serviceconfig.StructuredProperties;
import net.frontlinesms.ui.handler.settings.SmsInternetServiceSettingsHandler;

import org.apache.log4j.Logger;
import org.smslib.ReceiveNotSupportedException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import yo.sms.service.HttpConnection;
import yo.sms.service.MTService;
import yo.sms.service.XmlBuilder;
import yo.sms.service.XmlEntityBuilder;

/**
 * Implements Yo! internet SMS service
 * @author Eric <elwanga@yo.co.ug>
 *
 */
@ConfigurableServiceProperties(name = "Yo! (beta)", icon = "/icons/sms_http.png")
public class YoInternetService extends AbstractSmsInternetService {
    /**
     * Prefix attached to every property name. Used in application internet
     * service settings.
     */
    protected static final String PROPERTY_PREFIX = "smsdevice.internet.yo.";
    protected static final String PROPERTY_USERNAME = PROPERTY_PREFIX + "username";
    protected static final String PROPERTY_PASSWORD = PROPERTY_PREFIX + "password";
    protected static final String PROPERTY_FROM_MSISDN = PROPERTY_PREFIX + "from.msisdn";
    protected static final String PROPERTY_SSL = PROPERTY_PREFIX + "ssl";

    private static final String YBSSMGW_TAG = "YbsSmgw";
    private static final String REQUEST_TAG = "Request";
    private static final String METHOD_TAG = "Method";
    private static final String ACCOUNT_TAG = "Account";
    private static final String USERNAME_TAG = "Username";
    private static final String PASSWORD_TAG = "Password";
    private static final String STATUS_TAG = "Status";
    private static final String ERROR_MESSAGE = "ErrorMessage";
    private static final String AUTHENTICITY_TAG = "Authenticity";
    private static final String AUTHENTICATE = "/authenticate";
    private static final String METHOD = "CheckClientAuthenticity";
    /** Logging object */
    private static Logger LOG = FrontlineUtils.getLogger(YoInternetService.class);

    @Override
    protected void deinit() {
	this.setStatus(SmsInternetServiceStatus.DISCONNECTED, null);
    }

    @Override
    protected void init() throws SmsInternetServiceInitialisationException {
	if (verifyCredentials(getUsername(), getUsername(), getPassword(), isEncrypted())) {
	    this.setStatus(SmsInternetServiceStatus.CONNECTED, null);
	} else {
	    LOG.info("[DEBUG] init: Failed to connect ");
	    this.setStatus(SmsInternetServiceStatus.FAILED_TO_CONNECT, "Invalid username/password or verify connection");
	}
    }

    @Override
    protected void receiveSms() throws ReceiveNotSupportedException {
	throw new ReceiveNotSupportedException();
    }

    @Override
    protected void sendSmsDirect(FrontlineMessage message) {
	LOG.debug("Sending [" + message.getTextContent() + "] to [" + message.getRecipientMsisdn() + "]");
	MTService mTService = new MTService();
	String xmlTextRequest = mTService.buildXmlRequestEntity(getUsername(), getUsername(), getPassword(), message);
	try {
	    String response = mTService.postXmlRequest(xmlTextRequest, isEncrypted());
	    String messageStatus = processMTResponse(response);
	    if (messageStatus != null) {
		if (messageStatus.equals("INSUFFICIENT")) {
		    LOG.info("Insufficient Credit");
		    this.setStatus(SmsInternetServiceStatus.LOW_CREDIT, "");
		    message.setStatus(Status.FAILED);
		} else {
		    LOG.info("Message sent");
		    message.setStatus(Status.SENT);
		}
	    } else {
		LOG.info("Null response");
		message.setStatus(Status.FAILED);
	    }
	} catch (IOException e) {
	    LOG.debug("[DEBUG] Failed to send message: " + e.getMessage());
	    message.setStatus(Status.FAILED);
	} finally {
	    if (smsListener != null) {
		smsListener.outgoingMessageEvent(this, message);
	    }
	}
    }

    private String processMTResponse(String response) {
	try {
	    Document document = XmlBuilder.parseXml(response);
	    NodeList nodeList = document.getElementsByTagName(STATUS_TAG);
	    if (nodeList.getLength() > 0) {
		return nodeList.item(0).getTextContent();
	    }
	} catch (SAXException e) {
	    LOG.debug("Error: " + e.getMessage());
	} catch (IOException e) {
	    LOG.debug("IO Error: " + e.getMessage());
	} catch (ParserConfigurationException e) {
	    LOG.debug("Parse Error: " + e.getMessage());
	}
	return null;
    }

    public String getIdentifier() {
	return getPropertyValue(PROPERTY_USERNAME, String.class);
    }

    public String getMsisdn() {
	return getPropertyValue(PROPERTY_FROM_MSISDN, PhoneSection.class).getValue();
    }

    public StructuredProperties getPropertiesStructure() {
		StructuredProperties defaultSettings = new StructuredProperties();
		defaultSettings.put(PROPERTY_USERNAME, "");
		defaultSettings.put(PROPERTY_PASSWORD, new PasswordString(""));
		defaultSettings.put(PROPERTY_FROM_MSISDN, new PhoneSection(""));
		// defaultSettings.put(PROPERTY_SSL, Boolean.FALSE);
		defaultSettings.put(PROPERTY_USE_FOR_SENDING, Boolean.TRUE);
		// defaultSettings.put(PROPERTY_USE_FOR_RECEIVING, Boolean.FALSE);
		return defaultSettings;
    }

    public boolean isConnected() {
	/*
	 * try { InetAddress host =
	 * InetAddress.getByName(HttpConnection.PRIMARY_GATEWAY_ADDRESS);
	 * this.setStatus(SmsInternetServiceStatus.CONNECTED, null); return
	 * host.isReachable(1000); } catch (UnknownHostException e) {
	 * this.setStatus(SmsInternetServiceStatus.DISCONNECTED, null); return
	 * false; } catch (IOException e) {
	 * this.setStatus(SmsInternetServiceStatus.DISCONNECTED, null); return
	 * false; }
	 */
	return true;
    }

    public boolean isEncrypted() {
	// return getPropertyValue(PROPERTY_SSL, Boolean.class);
	return false;
    }

    public boolean isBinarySendingSupported() {
	return false;
    }

    public boolean isUcs2SendingSupported() {
	return false;
    }

    public void setUseForReceiving(boolean use) {
	this.setProperty(PROPERTY_USE_FOR_RECEIVING, new Boolean(use));
    }

    public void setUseForSending(boolean use) {
	this.setProperty(PROPERTY_USE_FOR_SENDING, new Boolean(use));
    }

    public boolean supportsReceive() {
	return true;
    }

    public String getDisplayPort() {
	return null;
    }

    public String getServiceName() {
	return getPropertyValue(PROPERTY_USERNAME, String.class) + UI_NAME_SEPARATOR + SmsInternetServiceSettingsHandler.getProviderName(getClass());
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

    public boolean isUseForReceiving() {
	return false;
	// return getPropertyValue(PROPERTY_USE_FOR_RECEIVING, Boolean.class);
    }

    public boolean isUseForSending() {
	return getPropertyValue(PROPERTY_USE_FOR_SENDING, Boolean.class);
    }

    private boolean verifyCredentials(String accountNumber, String username, String password, boolean secure) {
	final int METHOD_TYPE = 2;
	final int ACCOUNT_NUMBER = 3;
	final int USERNAME = 4;
	final int PASSWORD = 5;

	this.setStatus(SmsInternetServiceStatus.CONNECTING, null);
	XmlEntityBuilder xmlEntityBuilder = new XmlEntityBuilder();
	String[] startTags = { YBSSMGW_TAG, REQUEST_TAG, METHOD_TAG, ACCOUNT_TAG, USERNAME_TAG, PASSWORD_TAG };
	for (int i = 0; i < 6; i++) {
	    xmlEntityBuilder.writeStartElement(startTags[i]);
	    switch (i) {
	    case METHOD_TYPE:
		xmlEntityBuilder.writeText(METHOD);
		xmlEntityBuilder.writeEndElement();
		break;
	    case ACCOUNT_NUMBER:// A/c# or username
		xmlEntityBuilder.writeText(accountNumber);
		xmlEntityBuilder.writeEndElement();
		break;
	    case PASSWORD:
		xmlEntityBuilder.writeText(password);
		xmlEntityBuilder.writeEndElement();
		break;
	    case USERNAME:
		xmlEntityBuilder.writeText(username);
		xmlEntityBuilder.writeEndElement();
		break;
	    }
	}
	// Close "YbsSmgw" and "Request" tags
	xmlEntityBuilder.writeEndElement();
	xmlEntityBuilder.writeEndElement();
	String authenticationRequest = xmlEntityBuilder.getStringEntity();
	try {
	    String response = HttpConnection.postData(authenticationRequest, AUTHENTICATE, secure);
	    Document doc;
	    doc = XmlBuilder.parseXml(response);
	    NodeList nodeList = doc.getElementsByTagName(STATUS_TAG);
	    if (nodeList.item(0).getTextContent().equals("ERROR")) {
		nodeList = doc.getElementsByTagName(ERROR_MESSAGE);
		LOG.debug("Authenticate Error: " + nodeList.item(0).getTextContent());
		return false;
	    }
	    nodeList = doc.getElementsByTagName(ACCOUNT_TAG);
	    if (nodeList.item(0).getTextContent().equals(accountNumber)) {
		nodeList = doc.getElementsByTagName(AUTHENTICITY_TAG);
		if (nodeList.item(0).getTextContent().equals("VALID")) {
		    return true;
		}
	    } else {
		return false;
	    }
	} catch (IOException e) {
	    LOG.debug("IO Error: " + e.getMessage());
	    return false;
	} catch (SAXException e) {
	    LOG.debug("Error: " + e.getMessage());
	    return false;
	} catch (ParserConfigurationException e) {
	    LOG.debug("Parse Error: " + e.getMessage());
	    return false;
	}
	return false;
    }

}
