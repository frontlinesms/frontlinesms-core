package net.frontlinesms.serviceconfig;

public class SmsModemReference {
	public static final String BUTTON_ICON = "/icons/phone.png";
	private final String serial;
	
	public SmsModemReference(String serial) {
		this.serial = serial;
	}
	
	public String getSerial() {
		return serial;
	}
}
