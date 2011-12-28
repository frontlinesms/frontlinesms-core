/**
 * 
 */
package net.frontlinesms.messaging.sms.internet;

import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.messaging.sms.SmsServiceStatus;
import net.frontlinesms.messaging.sms.modem.SmsModem;

/**
 * Statuses for {@link SmsModem}
 * @author Alex Anderson <alex@frontlinesms.com>
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public enum SmsInternetServiceStatus implements SmsServiceStatus<SmsInternetService> {
	CONNECTED(FrontlineSMSConstants.SMS_DEVICE_STATUS_CONNECTED),
	CONNECTING(FrontlineSMSConstants.SMS_DEVICE_STATUS_CONNECTING),
	DISCONNECTED(FrontlineSMSConstants.SMS_DEVICE_STATUS_DISCONNECT),
	DORMANT(FrontlineSMSConstants.SMS_DEVICE_STATUS_DORMANT),
	FAILED_TO_CONNECT(FrontlineSMSConstants.SMS_DEVICE_STATUS_FAILED_TO_CONNECT),
	LOW_CREDIT(FrontlineSMSConstants.SMS_DEVICE_STATUS_LOW_CREDIT),
	RECEIVING_FAILED(FrontlineSMSConstants.SMS_DEVICE_STATUS_RECEIVING_FAILED),
	TRYING_TO_RECONNECT(FrontlineSMSConstants.SMS_DEVICE_STATUS_TRYING_RECONNECT);

//> PROPERTIES
	/** Key for getting relelvant message from language bundle */
	private final String i18nKey;
	
//> CONSTRUCTORS
	/** @param i18nKey value for {@link #i18nKey} */
	private SmsInternetServiceStatus(String i18nKey) {
		this.i18nKey = i18nKey;
	}
	
//> ACCESSOR METHODS
	/** @see SmsServiceStatus#getI18nKey() */
	public String getI18nKey() {
		return this.i18nKey;
	}
}
