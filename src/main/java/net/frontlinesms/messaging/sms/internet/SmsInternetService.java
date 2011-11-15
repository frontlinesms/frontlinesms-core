/**
 * 
 */
package net.frontlinesms.messaging.sms.internet;

import net.frontlinesms.data.domain.PersistableSettings;
import net.frontlinesms.events.EventBus;
import net.frontlinesms.listener.SmsListener;
import net.frontlinesms.messaging.sms.SmsService;
import net.frontlinesms.serviceconfig.ConfigurableService;
import net.frontlinesms.serviceconfig.StructuredProperties;

/**
 * Service allowing sending and/or receiving of SMS messages over an internet connection.
 * @author Alex
 */
public interface SmsInternetService extends SmsService, ConfigurableService {
	/**
	 * Gets an identifier for this instance of {@link SmsInternetService}.  Usually this
	 * will be the username used to login with the provider, or a similar identifer on
	 * the service.
	 * @return a text identifier for this service
	 */
	public String getIdentifier();

	/** @return the settings attached to this {@link SmsInternetService} instance. */
	public PersistableSettings getSettings();

	/**
	 * Initialise the service using the supplied properties.
	 * @param settings
	 */
	public void setSettings(PersistableSettings settings);
	
	/** Sets the {@link SmsListener} attached to this {@link SmsInternetService}. */
	public void setSmsListener(SmsListener smsListener);

	/** Sets the {@link EventBus} attached to this {@link SmsInternetService}. */
	public void setEventBus(EventBus eventBus);
	
	/**
	 * Checks if the service is currently connected.
	 * TODO could rename this isLive().
	 * @return <code>true</code> if the service is currently connected; <code>false</code> otherwise
	 */
	public boolean isConnected();
	
	/** Starts this service. */
	public void startService();
	
	/** Re-connects this service. */
	public void restartService();
	
	/** Stop this service from running */
	public void stopService();
	
	/**
	 * Check if this service is encrypted using SSL.
	 * @return <code>true</code> if this service is using SSL; <code>false</code> otherwise
	 */
	public boolean isEncrypted();
	
	/** Gets the MSISDN that numbers sent from this service will appear to be from. */
	public String getMsisdn();
	
	/**
	 * Get the properties structure for this class.
	 * TODO should probably be called getDefaultProperties()...
	 * @return gets the structure for the properties of this service type
	 */
	public StructuredProperties getPropertiesStructure();
	
	/** @see SmsService#getStatus() */
	public SmsInternetServiceStatus getStatus();
}
