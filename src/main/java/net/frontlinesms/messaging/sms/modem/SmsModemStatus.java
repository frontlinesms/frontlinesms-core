/**
 * 
 */
package net.frontlinesms.messaging.sms.modem;

import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.messaging.sms.SmsServiceStatus;

/**
 * Statuses for {@link SmsModem}
 * @author Alex
 */
public enum SmsModemStatus implements SmsServiceStatus<SmsModem> {
	CONNECTED(FrontlineSMSConstants.SMS_DEVICE_STATUS_CONNECTED),
	CONNECTING(FrontlineSMSConstants.SMS_DEVICE_STATUS_CONNECTING),
	/**
	 * Searching for device, and something has been found at a lower speed
	 * {@link SmsModem#statusDetail} will contain the max speed at which a device has been found.
	 */
	DETECTED(FrontlineSMSConstants.SMS_DEVICE_STATUS_DETECTED),
	DISCONNECTED(FrontlineSMSConstants.SMS_DEVICE_STATUS_DISCONNECT),
	/** The Handler was forced to disconnect for some reason. */
	DISCONNECT_FORCED(FrontlineSMSConstants.SMS_DEVICE_STATUS_DISCONNECT_FORCED),
	DISCONNECTING(FrontlineSMSConstants.SMS_DEVICE_STATUS_DISCONNECTING),
	DORMANT(FrontlineSMSConstants.SMS_DEVICE_STATUS_DORMANT),
	DUPLICATE(FrontlineSMSConstants.SMS_DEVICE_STATUS_DUPLICATE),
	/**
	 * Connection failed.
	 * {@link SmsModem#statusDetail} will contain the name and message of the exception which caused the failure thrown.
	 */
	FAILED_TO_CONNECT(FrontlineSMSConstants.SMS_DEVICE_STATUS_FAILED_TO_CONNECT),
	/** The SIM card has been refused by the GSM network. */
	GSM_REG_FAILED(FrontlineSMSConstants.SMS_MODEM_STATUS_GSM_REG_FAILED),
	/**
	 * Max device speed has been found.  Soon we will try to connect.
	 * {@link SmsModem#statusDetail} will contain the max speed at which a device has been found.
	 */
	MAX_SPEED_FOUND(FrontlineSMSConstants.SMS_DEVICE_STATUS_MAX_SPEED),
	/**
	 * Trying to connect to the device at a specific speed.
	 * {@link SmsModem#statusDetail} will contain the speed we are trying to connect at.
	 */
	TRY_TO_CONNECT(FrontlineSMSConstants.SMS_DEVICE_STATUS_TRYING_TO_CONNECT),
	NO_PHONE_DETECTED(FrontlineSMSConstants.SMS_DEVICE_STATUS_NO_PHONE_DETECTED),
	/**
	 * Searching for device
	 * {@link SmsModem#statusDetail} will contain the max speed at which a device has been found.
	 */
	SEARCHING(FrontlineSMSConstants.SMS_DEVICE_STATUS_SEARCHING),
	/**
	 * Owned by someone else.
	 * {@link SmsModem#statusDetail} will contain the name of the port owner.
	 */
	OWNED_BY_SOMEONE_ELSE(FrontlineSMSConstants.SMS_MODEM_STATUS_ALREADY_OWNED),;
	
//> PROPERTIES
	/** Key for getting relevant message from language bundle */
	private final String i18nKey;
	
//> CONSTRUCTORS
	/** @param i18nKey value for {@link #i18nKey} */
	private SmsModemStatus(String i18nKey) {
		this.i18nKey = i18nKey;
	}
	
//> ACCESSOR METHODS
	/** @see SmsServiceStatus#getI18nKey() */
	public String getI18nKey() {
		return this.i18nKey;
	}
}
