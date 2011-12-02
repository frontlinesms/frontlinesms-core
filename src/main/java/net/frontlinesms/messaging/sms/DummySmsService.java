/**
 * 
 */
package net.frontlinesms.messaging.sms;

import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.listener.SmsListener;

/**
 * An {@link SmsService} which doesn't really do anything.
 * @author aga
 */
public class DummySmsService implements SmsService {
	/**
	 * The phone number of this device.
	 */
	private final String phoneNumber;

	/**
	 * Create a new {@link DummySmsService}. 
	 * @param phoneNumber value for {@link #phoneNumber}
	 */
	public DummySmsService(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	/** @see net.frontlinesms.messaging.sms.SmsService#getMsisdn() */
	public String getMsisdn() {
		return this.phoneNumber;
	}

	/** @see net.frontlinesms.messaging.sms.SmsService#getStatus() */
	public SmsServiceStatus<?> getStatus() {
		/* do nothing */
		return null;
	}

	/** @see net.frontlinesms.messaging.sms.SmsService#getStatusDetail() */
	public String getStatusDetail() {
		/* do nothing */
		return null;
	}

	/** 
	 * @see net.frontlinesms.messaging.sms.SmsService#isBinarySendingSupported()
	 */
	public boolean isBinarySendingSupported() {
		/* do nothing */
		return false;
	}

	/** 
	 * @see net.frontlinesms.messaging.sms.SmsService#isConnected()
	 */
	public boolean isConnected() {
		/* do nothing */
		return false;
	}

	/** 
	 * @see net.frontlinesms.messaging.sms.SmsService#isUcs2SendingSupported()
	 */
	public boolean isUcs2SendingSupported() {
		/* do nothing */
		return false;
	}

	/** 
	 * @see net.frontlinesms.messaging.sms.SmsService#isUseForReceiving()
	 */
	public boolean isUseForReceiving() {
		/* do nothing */
		return false;
	}

	/** 
	 * @see net.frontlinesms.messaging.sms.SmsService#isUseForSending()
	 */
	public boolean isUseForSending() {
		/* do nothing */
		return false;
	}

	/** 
	 * @see net.frontlinesms.messaging.sms.SmsService#sendSMS(net.frontlinesms.data.domain.FrontlineMessage)
	 */
	public void sendSMS(FrontlineMessage outgoingMessage) {
		/* do nothing */

	}

	/** 
	 * @see net.frontlinesms.messaging.sms.SmsService#setSmsListener(net.frontlinesms.listener.SmsListener)
	 */
	public void setSmsListener(SmsListener smsListener) {
		/* do nothing */

	}

	/** 
	 * @see net.frontlinesms.messaging.sms.SmsService#setUseForReceiving(boolean)
	 */
	public void setUseForReceiving(boolean use) {
		/* do nothing */

	}

	/** 
	 * @see net.frontlinesms.messaging.sms.SmsService#setUseForSending(boolean)
	 */
	public void setUseForSending(boolean use) {
		/* do nothing */

	}

	/** 
	 * @see net.frontlinesms.messaging.sms.SmsService#supportsReceive()
	 */
	public boolean supportsReceive() {
		/* do nothing */
		return false;
	}

	public String getServiceIdentification() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getServiceName() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getPort() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getDisplayPort() {
		// TODO Auto-generated method stub
		return null;
	}

}
