package net.frontlinesms.messaging.sms.events;

import net.frontlinesms.events.FrontlineEventNotification;
import net.frontlinesms.messaging.sms.SmsService;
import net.frontlinesms.messaging.sms.SmsServiceStatus;
import net.frontlinesms.messaging.sms.modem.SmsModem;
/**
 * A superclass for notifications involving device connections.
 * In the fullness of time, this notification type should be used to replace {@link SmsServiceEventListener}.
 * TODO to replace {@link SmsServiceEventListener}, this class will need to contain a reference to the {@link SmsService} which has triggered each notification.
 * 
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 * @author Alex Anderson <alex@frontlinesms.com>
 */
public abstract class SmsServiceStatusNotification<Service extends SmsModem, Status extends SmsServiceStatus<Service>> implements FrontlineEventNotification {
	private Service service;
	private Status status;
	
	public SmsServiceStatusNotification(Service service, Status status) {
		this.service = service;
		this.status = status;
	}

	public Status getStatus() {
		return status;
	}
	
	public Service getService() {
		return service;
	}
}
