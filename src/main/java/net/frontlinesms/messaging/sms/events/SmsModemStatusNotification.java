/**
 * 
 */
package net.frontlinesms.messaging.sms.events;

import net.frontlinesms.messaging.sms.modem.SmsModem;
import net.frontlinesms.messaging.sms.modem.SmsModemStatus;

/**
 * @author Roy, Alex
 */
public class SmsModemStatusNotification extends SmsServiceStatusNotification<SmsModem, SmsModemStatus> {
	public SmsModemStatusNotification(SmsModem modem, SmsModemStatus status) {
		super(modem, status);
	}
}
