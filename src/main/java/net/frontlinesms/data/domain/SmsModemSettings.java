/**
 * 
 */
package net.frontlinesms.data.domain;

import javax.persistence.*;

/**
 * @author Alex
 */
@Entity
public class SmsModemSettings {
	/** Field names */
	public static final String FIELD_SERIAL = "serial";
	
//> INSTANCE PROPERTIES
	/** Unique id for this entity.  This is for hibernate usage. */
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(unique=true,nullable=false,updatable=false) @SuppressWarnings("unused")
	private long id;
	@Column(name=FIELD_SERIAL)
	/** the serial number of the device */
	private String serial;
	private String manufacturer;
	private String model;
	/** Whether or not the device was supporting receiving last time it was connected **/
	private Boolean supportingReceive;
	/** The SMSC number for this device. */
	private String smscNumber;
	/** The PIN number for this device. */
	private String simPin;
	/** @param useForSending whether the device should be used for sending SMS */
	private boolean useForSending;
	/** whether the device should be used for receiving SMS */
	private boolean useForReceiving;
	/** whether messages should be deleted from the device after being read by FrontlineSMS */
	private boolean deleteMessagesAfterReceiving;
	/** whether the device should supply all incoming messages, or only those not previously read. */
	private boolean readOnlyUnreadMessages;
	/** whether delivery reports should be used with this device */
	private boolean useDeliveryReports;
	/** whether phone calls should be monitored on this device */
	private boolean monitorCalls;
	
//> CONSTRUCTORS
	/** Empty constructor for hibernate */
	SmsModemSettings() {}
	
	/**
	 * Sets the details for the supplied SMS device
	 * @param serial The serial number of the device
	 * @param useForSending whether the device should be used for sending SMS
	 * @param useForReceiving whether the device should be used for receiving SMS
	 * @param deleteMessagesAfterReceiving whether messages should be deleted from the device after being read by FrontlineSMS 
	 * @param useDeliveryReports whether delivery reports should be used with this device
	 */
	public SmsModemSettings(String serial, String manufacturer, String model, boolean supportsReceive, boolean useForSending, boolean useForReceiving, boolean deleteMessagesAfterReceiving, boolean useDeliveryReports) {
		this.serial = serial;
		this.manufacturer = manufacturer;
		this.model = model;
		this.supportingReceive = supportsReceive;
		this.useForSending = useForSending;
		this.useForReceiving = useForReceiving;
		this.deleteMessagesAfterReceiving = deleteMessagesAfterReceiving;
		this.useDeliveryReports = useDeliveryReports;
	}
	public SmsModemSettings(String serial){
		this.serial = serial;	
	}
	
//> ACCESSOR METHODS
	public long getId() {
		return id;
	}
	public String getSerial() {
		return serial;
	}
	public String getManufacturer() {
		return manufacturer;
	}
	public void setManufacturer(String make) {
		this.manufacturer = make;
	}
	public String getModel() {
		return model;
	}
	public void setModel(String model) {
		this.model = model;
	}
	public boolean useForSending() {
		return useForSending;
	}
	public void setUseForSending(boolean useForSending) {
		this.useForSending = useForSending;
	}
	public boolean useForReceiving() {
		return useForReceiving;
	}
	public void setUseForReceiving(boolean useForReceiving) {
		this.useForReceiving = useForReceiving;
	}
	public boolean deleteMessagesAfterReceiving() {
		return deleteMessagesAfterReceiving;
	}
	public void setDeleteMessagesAfterReceiving(boolean deleteMessagesAfterReceiving) {
		this.deleteMessagesAfterReceiving = deleteMessagesAfterReceiving;
	}
	public boolean readOnlyUnreadMessages() {
		return readOnlyUnreadMessages;
	}
	public void setReadOnlyUnreadMessages(boolean readOnlyUnreadMessages) {
		this.readOnlyUnreadMessages = readOnlyUnreadMessages;
	}
	public boolean useDeliveryReports() {
		return useDeliveryReports;
	}
	public void setUseDeliveryReports(boolean useDeliveryReports) {
		this.useDeliveryReports = useDeliveryReports;
	}
	/** @return the smscNumber */
	public String getSmscNumber() {
		return smscNumber;
	}
	/** @param smscNumber the smscNumber to set */
	public void setSmscNumber(String smscNumber) {
		this.smscNumber = smscNumber;
	}
	/** @return the PIN for the device's SIM */
	public String getSimPin() {
		return simPin;
	}
	/** @param simPin the PIN for the device's SIM */
	public void setSimPin(String simPin) {
		this.simPin = simPin;
	}
	/** @return <code>true</code> if calls should be monitored on this device; <code>false</code> otherwise */
	public boolean monitorCalls() {
		return monitorCalls;
	}
	/** @param monitorCalls new value for {@link #monitorCalls} */
	public void setMonitorCalls(boolean monitorCalls) {
		this.monitorCalls = monitorCalls;
	}
	public void setSupportsReceive(Boolean supportsReceive) {
		this.supportingReceive = (supportsReceive == null ? true : supportsReceive);
	}
	public boolean supportsReceive() {
		return supportingReceive == null ? true : supportingReceive;
	}

//> GENERATED METHODS
	/** @see java.lang.Object#hashCode() */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((serial == null) ? 0 : serial.hashCode());
		return result;
	}

	/** @see java.lang.Object#equals(java.lang.Object) */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SmsModemSettings other = (SmsModemSettings) obj;
		if (serial == null) {
			if (other.serial != null)
				return false;
		} else if (!serial.equals(other.serial))
			return false;
		return true;
	}
}
