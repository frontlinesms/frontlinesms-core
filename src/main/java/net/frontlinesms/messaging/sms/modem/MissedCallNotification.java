package net.frontlinesms.messaging.sms.modem;

import org.smslib.CIncomingCall;

import net.frontlinesms.events.FrontlineEventNotification;

public class MissedCallNotification implements FrontlineEventNotification {
	private SmsModem smsModem;
	CIncomingCall call;

	public MissedCallNotification(SmsModem smsModem, CIncomingCall call) {
		this.smsModem = smsModem;
	}
	
	public CIncomingCall getCall() {
		return call;
	}

	public SmsModem getSmsModem() {
		return smsModem;
	}
}
