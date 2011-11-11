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

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.ConfigurableService;
import net.frontlinesms.data.StructuredProperties;
import net.frontlinesms.data.domain.*;
import net.frontlinesms.data.domain.FrontlineMessage.Status;
import net.frontlinesms.listener.SmsListener;
import net.frontlinesms.messaging.sms.properties.OptionalRadioSection;
import net.frontlinesms.messaging.sms.properties.OptionalSection;

import org.apache.log4j.Logger;

/**
 * Abstract class containing all default information needed by 
 * Sms Internet Services (e.g Clickatell, IntelliSMS, etc).
 * 
 * @author Carlos Eduardo Endler Genz
 * @date 26/01/2009
 */
public abstract class AbstractSmsInternetService implements SmsInternetService {
	
//> CONSTANTS
	/** Logging object */
	private static Logger LOG = FrontlineUtils.getLogger(AbstractSmsInternetService.class);
	/** Property name: use this service for sending SMS */
	protected static final String PROPERTY_USE_FOR_SENDING = "common.use.for.sending";
	/** Property name: use this service for receiving SMS */
	protected static final String PROPERTY_USE_FOR_RECEIVING = "common.use.for.receiving";
	/** Separator used while displaying the name in the UI */
	protected static final String UI_NAME_SEPARATOR = "@";
	
//> INSTANCE PROPERTIES
	/** The active thread running this service */
	private SmsInternetServiceThread thread;
	/** Queue of SMS messages waiting to be sent with this service */
	protected ConcurrentLinkedQueue<FrontlineMessage> outbox = new ConcurrentLinkedQueue<FrontlineMessage>();
	/** The SmsListener to which this phone handler should report SMS Message events. */
	protected SmsListener smsListener;
	/** Settings for this service */
	private PersistedSettings settings;
	/** The status of this device */
	private SmsInternetServiceStatus status = SmsInternetServiceStatus.DORMANT;
	/** Extra info relating to the current status. */
	private String statusDetail;

//> ACCESSOR METHODS
	
	/** @return This internet service outbox. */
	public ConcurrentLinkedQueue<FrontlineMessage> getOutbox() {
		return outbox;
	}
	
	/** @return the settings attached to this {@link SmsInternetService} instance. */
	public PersistedSettings getSettings() {
		return settings;
	}
	
	/** @param smsListener new vlue for {@link SmsListener} */
	public void setSmsListener(SmsListener smsListener) {
		this.smsListener = smsListener;
	}
	
	/** @return {@link #statusDetail} */
	public String getStatusDetail() {
		return this.statusDetail;
	}
	
	/** @return {@link #status} */
	public SmsInternetServiceStatus getStatus() {
		return this.status;
	}
	
	public String getServiceIdentification() {
		return this.getMsisdn();
	}

	/**
	 * Set the status of this {@link SmsInternetService}, and fires an event to {@link #smsListener}
	 * @param status the status
	 * @param detail detail relating to the status
	 */
	protected void setStatus(SmsInternetServiceStatus status, String detail) {
		if(this.status == null || !this.status.equals(status) || this.statusDetail == null || this.statusDetail.equals(detail)) {
			this.status = status;
			this.statusDetail = detail;
			LOG.debug("Status [" + status.name()
					+ (detail == null?"":": "+detail)
					+ "]");
	
			if (smsListener != null) {
				smsListener.smsDeviceEvent(this, status);
			}
		}
	}
	
	public Class<? extends ConfigurableService> getSuperType() {
		return SmsInternetService.class;
	}
	
//> OTHER METHODS
	/** 
	 * Adds the supplied message to the outbox. 
	 */
	public void sendSMS(FrontlineMessage outgoingMessage) {
		LOG.trace("ENTER");
		outgoingMessage.setStatus(Status.PENDING);
		outgoingMessage.setSenderMsisdn(getMsisdn());
		
		outbox.add(outgoingMessage);
		if (smsListener != null) {
			smsListener.outgoingMessageEvent(this, outgoingMessage);
		}
		LOG.debug("Message added to outbox. Size is [" + outbox.size() + "]");
		LOG.trace("EXIT");
	}
	
	/**
	 * Sets a property in {@link #settings}.
	 * @param key
	 * @param value
	 */
	protected void setProperty(String key, Object value) {
		this.settings.set(key, value);
	}
	
	/**
	 * @param key The key of the property
	 * @param clazz The class of the property's value
	 * @param <T> The class of the property's value
	 * @return The property value, either the one stored on db (if any) or the default value.
	 */
	@SuppressWarnings("unchecked")
	protected <T extends Object> T getPropertyValue(String key, Class<T> clazz) {
		T defaultValue = (T) getValue(key, getPropertiesStructure());
		if (defaultValue == null) throw new IllegalArgumentException("No default value could be found for key: " + key);
		
		PersistedSettingValue setValue = this.settings.get(key);
		if(setValue == null) return defaultValue;
		else return (T) PersistedSettings.fromValue(defaultValue, setValue);
	}
	
	/** Stop this service from running */
	public void stopRunning() {
		LOG.trace("ENTER");
		setStatus(SmsInternetServiceStatus.DISCONNECTED, null);
		this.thread.running = false;
		LOG.trace("EXIT");
	}
	
	/**
	 * Initialise the service using the supplied properties.
	 * @see SmsInternetService#setSettings(PersistedSettings)
	 */
	public void setSettings(PersistedSettings settings) {
		this.settings = settings;
	}

	/** Starts this service. */
	public synchronized void startThisThing() {
		try {
			setStatus(SmsInternetServiceStatus.CONNECTING, null);
			init();
			SmsInternetServiceThread newThread = new SmsInternetServiceThread(this);
			this.thread = newThread;
			newThread.start();
		} catch(SmsInternetServiceInitialisationException ex) {
			LOG.error("There was an error starting SMS Internet Service.", ex);
			
			// stopThisThing() updates the status to a meaningless "not connected" message, so 
			// we need to keep the old, meaningul message from before
			SmsInternetServiceStatus status = getStatus();
			String statusDetail = getStatusDetail();
			this.stopThisThing();
			this.setStatus(status, statusDetail);
		}
	}
	
	/** Re-connects this service. */
	public void restartThisThing() {
		this.stopThisThing();
		this.startThisThing();
	}
	
	/** Stop this service from running */
	public void stopThisThing() {
		deinit();
		if(this.thread != null) this.thread.running = false;
	}
	
	private class SmsInternetServiceThread extends Thread {
		/** Indicates whether this {@link SmsInternetServiceThread} is running. */ 
		protected boolean running;
		
		SmsInternetServiceThread(AbstractSmsInternetService owner) {
			super(owner.getClass().getSimpleName() + " :: " + owner.getIdentifier());
		}
		
		/**
		 * Sends and receives SMS messages.
		 */
		public void run() {
			LOG.trace("ENTER");
			running = true;
			while (running) {
				boolean sleep = true;
				if (isConnected() && isUseForSending()) {
					FrontlineMessage m = outbox.poll();
					if (m != null) {
						LOG.debug("Sending message [" + m.toString() + "]");
						long startTime = System.currentTimeMillis();
						sendSmsDirect(m);
						LOG.debug("Send messages took [" + (System.currentTimeMillis() - startTime) + "]");
						sleep = false;
					}
				}
				if (running && isConnected() && isUseForReceiving()) {
					LOG.debug("Receiving messages...");
					try {
						long startTime = System.currentTimeMillis();
						receiveSms();
						LOG.debug("Receiving messages took " + (System.currentTimeMillis() - startTime) + "ms");
					} catch (SmsInternetServiceReceiveException e) {
						LOG.error("Failed to receive messages.", e);
						// Should this really be a status?
						setStatus(SmsInternetServiceStatus.RECEIVING_FAILED, null);
					}
				}
				// TODO verify delivery reports?
				// If this thread is still running, we should have a little snooze
				if (running) {
					if (sleep) FrontlineUtils.sleep_ignoreInterrupts(5000); /* 5 seconds */
					else FrontlineUtils.sleep_ignoreInterrupts(100); /* 0.1 seconds */
				}
			}
			LOG.trace("EXIT");
		}
	}

//> ABSTRACT METHODS
	/**
	 * Initialise all settings related to this service's {@link SmsInternetServiceThread}
	 * @throws SmsInternetServiceInitialisationException
	 */
	protected abstract void init() throws SmsInternetServiceInitialisationException;

	/** De-initialise all settings related to this service's {@link SmsInternetServiceThread} */
	protected abstract void deinit();
	
	/**
	 * Send an SMS message using this phone handler.
	 * @param message The message to be sent.
	 */
	protected abstract void sendSmsDirect(FrontlineMessage message);
	
	/**
	 * Attempt to receive SMS messages from this service.
	 * @throws SmsInternetServiceReceiveException If there was a problem receiving SMS
	 */
	protected abstract void receiveSms() throws SmsInternetServiceReceiveException;
	
//> STATIC HELPER METHODS
	/**
	 * Deep-searches nested maps for a propertt's value.  Maps may be nested as values
	 * inside other maps by wrapping them in either an {@link OptionalSection} or an
	 * {@link OptionalRadioSection}.
	 * @param key
	 * @param map
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static Object getValue(String key, StructuredProperties map) {
		if (map == null) {
			// TODO when would map be null?  perhaps we should just be clear that the result is undefined when this is the case?
			return null;
		} else if (map.containsKey(key)) {
			return map.get(key);
		} else {
			for(Object mapValue : map.values()) {
				if(mapValue instanceof OptionalSection) {
					Object value = getValue(key, ((OptionalSection)mapValue).getDependencies());
					if(value != null) return value;
				} else if(mapValue instanceof OptionalRadioSection) {
					Collection<StructuredProperties> dependencies = ((OptionalRadioSection) mapValue).getAllDependencies();
					for(StructuredProperties dependencyMap : dependencies) {
						Object value = getValue(key, dependencyMap);
						if(value != null) return value;
					}
				}
			}
		}
		return null;
	}
}
