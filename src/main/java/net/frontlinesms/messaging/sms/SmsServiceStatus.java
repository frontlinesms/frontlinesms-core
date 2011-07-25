/**
 * 
 */
package net.frontlinesms.messaging.sms;

import net.frontlinesms.messaging.FrontlineMessagingServiceStatus;

/**
 * A status for an {@link SmsService}
 * @author Alex Anderson <alex@frontlinesms.com>
 */
public interface SmsServiceStatus<E extends SmsService> extends FrontlineMessagingServiceStatus {
	/** @return the internationalisation key for this status - this key gets an appropriate message for this status from the language bundle */
	public String getI18nKey();
}
