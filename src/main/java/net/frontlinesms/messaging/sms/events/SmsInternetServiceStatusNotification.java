/**
 * 
 */
package net.frontlinesms.messaging.sms.events;

import net.frontlinesms.messaging.sms.internet.SmsInternetService;
import net.frontlinesms.messaging.sms.internet.SmsInternetServiceStatus;

/**
 * @author Roy, Alex
 */
public class SmsInternetServiceStatusNotification extends SmsServiceStatusNotification<SmsInternetService, SmsInternetServiceStatus> {
	public SmsInternetServiceStatusNotification(SmsInternetService service, SmsInternetServiceStatus status) {
		super(service, status);
	}
}
