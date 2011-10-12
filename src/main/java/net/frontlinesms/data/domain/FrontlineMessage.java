/*
 * FrontlineSMS <http://www.frontlinesms.com>
 * Copyright 2007, 2008 kiwanja
 * 
 * This file is part of FrontlineSMS.
 * 
 * FrontlineSMS is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * FrontlineSMS is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FrontlineSMS. If not, see <http://www.gnu.org/licenses/>.
 */
package net.frontlinesms.data.domain;

import java.util.Arrays;

import javax.persistence.*;

import org.hibernate.annotations.DiscriminatorFormula;
import org.smslib.util.GsmAlphabet;
import org.smslib.util.HexUtils;
import org.smslib.util.TpduUtils;

import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.data.EntityField;
import net.frontlinesms.ui.i18n.Internationalised;

/**
 * Object representing an SMS message in our data structure.
 * @author Alex
 */
@Entity
// This class is mapped to the database table called "message", as this class used to be called "Message"
@Table(name="message")
@DiscriminatorFormula("(CASE WHEN dtype IS NULL THEN 'FrontlineMessage' ELSE dtype END)")
public class FrontlineMessage {
	/** Discriminator column for this class.  This was only implemented when {@link FrontlineMultimediaMessage} was
	 * added.  Setting it to null will result in a plain {@link FrontlineMessage} being instantiated, as per the
	 * {@link DiscriminatorFormula} annotation on this class. */
	@SuppressWarnings("unused")
	private String dtype = this.getClass().getSimpleName();
	
//> DATABASE COLUMN NAMES
	/** Database column name for field {@link #textMessageContent} */
	private static final String COLUMN_TEXT_CONTENT = "textContent";
	
//> CONSTANTS
	public enum Type {
		/** This is a pseudo-message type, used as a blanket for all types. */
		ALL,
		/** Message type: unknown */
		UNKNOWN,
		/** Message type: received */
		RECEIVED,
		/** Message type: outbound */
		OUTBOUND,
		/** Message type: delivery report */
		DELIVERY_REPORT;
	}
	
	public enum Status implements Internationalised {
		/** Message status: DRAFT - nothing has been done with this message yet */
		DRAFT(FrontlineSMSConstants.COMMON_DRAFT),
		/** messages of TYPE_RECEIVED should always be STATUS_RECEIVED */
		RECEIVED(FrontlineSMSConstants.COMMON_RECEIVED),
		/** outgoing message that is created, and will be sent to a phone as soon as one is available */
		OUTBOX(FrontlineSMSConstants.COMMON_OUTBOX),
		/** outgoing message given to a phone, which the phone is trying to send */
		PENDING(FrontlineSMSConstants.COMMON_PENDING),
		/** outgoing message successfully delivered to the GSM network*/
		SENT(FrontlineSMSConstants.COMMON_SENT),
		/** outgoing message that has had delivery confirmed by the GSM network */
		DELIVERED(FrontlineSMSConstants.COMMON_DELIVERED),
		/** Outgoing message that had status KEEP TRYING returned by the GSM network */
		KEEP_TRYING(FrontlineSMSConstants.COMMON_RETRYING),
		@Deprecated ABORTED(null),
		@Deprecated UNKNOWN(null),
		/** Outgoing message that had status FAILED returned by the GSM network */
		FAILED(FrontlineSMSConstants.COMMON_FAILED);
		
		private final String i18nKey;
		
		private Status(String i18nKey) {
			this.i18nKey = i18nKey;
		}
		
		public String getI18nKey() {
			return i18nKey;
		}
	}
	
	/** Number of times a failed message send is retried before status is set to STATUS_FAILED */
	public static final int MAX_RETRIES = 2;
	
	/** The maximum number of parts in an SMS message.  TODO rename this SMS_PART_LIMIT */
	public static final int SMS_LIMIT = 255;
	/** Maximum number of characters that can be fit into a single 7-bit GSM SMS message. TODO this value should probably be fetched from {@link TpduUtils}. */
	public static final int SMS_LENGTH_LIMIT = 160;
	/** Maximum number of characters that can be fit in one part of a multipart 7-bit GSM SMS message.  TODO this number is incorrect, I suspect.  The value should probably be fetched from {@link TpduUtils}. */
	public static final int SMS_MULTIPART_LENGTH_LIMIT = 135;
	/** Maximum number of characters that can be fit into a single UCS-2 SMS message. TODO this value should probably be fetched from {@link TpduUtils}. */
	public static final int SMS_LENGTH_LIMIT_UCS2 = 70;
	/** Maximum number of characters that can be fit in one part of a multipart UCS-2 SMS message.  TODO this number is incorrect, I suspect.  The value should probably be fetched from {@link TpduUtils}. */
	public static final int SMS_MULTIPART_LENGTH_LIMIT_UCS2 = 60;
	/** Maximum number of characters that can be fit into a single binary SMS message. TODO this value should probably be fetched from {@link TpduUtils}. */
	public static final int SMS_LENGTH_LIMIT_BINARY = 140;
	/** Maximum number of characters that can be fit in one part of a binary SMS message.  TODO this number is incorrect, I suspect.  The value should probably be fetched from {@link TpduUtils}. */
	public static final int SMS_MULTIPART_LENGTH_LIMIT_BINARY = 120;
	
	/** Maximum number of characters that can be fit into a 255-part GSM 7bit message */
	public static final int SMS_MAX_CHARACTERS = 255 * SMS_MULTIPART_LENGTH_LIMIT;
	


//> ENTITY FIELDS
	/** Details of the fields that this class has. */
	public enum Field implements EntityField<FrontlineMessage> {
		ID("id"),
		TYPE("type"),
		DATE("date"),
		STATUS("status"),
		SENDER_MSISDN("senderMsisdn"),
		RECIPIENT_MSISDN("recipientMsisdn"),
		ENDPOINT_ID("endpointId"),
		MESSAGE_CONTENT("textMessageContent"),
		SMSC_REFERENCE("smscReference");
		/** name of a field */
		private final String fieldName;
		/**
		 * Creates a new {@link Field}
		 * @param fieldName name of the field
		 */
		Field(String fieldName) { this.fieldName = fieldName; }
		/** @see EntityField#getFieldName() */
		public String getFieldName() { return this.fieldName; }
	}
	
//> INSTANCE PROPERTIES
	/** Unique id for this entity.  This is for hibernate usage. */
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(unique=true,nullable=false,updatable=false) @SuppressWarnings("unused")
	private long id;
	private Type type;
	private int retriesRemaining;
	private Status status;
	private String recipientMsisdn;
	private int recipientSmsPort;
	private int smsPartsCount;
	private long date;
	private Integer smscReference;
	private String senderMsisdn;
	/** Optional variable for recording the ID of the local endpoint which sent or received this message.  For 
	 * failed outgoing messages, this should be the last device sending was attempted with.
	 * 
	 * For GSMmodems, this should be a combination of IMSI and device serial number, preceded by an identifier
	 * e.g. smslib:<IMSI>@<device-serial> */
	private String endpointId;
	/** Text content of this message. */
	@Column(name=COLUMN_TEXT_CONTENT, length=SMS_MAX_CHARACTERS)
	private String textMessageContent;
	/** Binary content of this message. */
	private byte[] binaryMessageContent;
	
//> CONSTRUCTOR
	/** Default constructor empty for hibernate */
	FrontlineMessage() {}
	
	protected FrontlineMessage(Type type, String textContent) {
		this.type = type;
		this.setTextMessageContent(textContent);
		this.setSmsPartsCount(getExpectedSmsCount());
	}
	
//> ACCESSOR METHODS
	/**
	 * Gets the type of this Message.  Should be one of the Message.TYPE_ constants.
	 * @return
	 */
	public Type getType() {
		return this.type;
	}
	
	/**
	 * Gets the status of this Message.  Should be one of the Message.STATUS_ constants.
	 * @return
	 */
	public Status getStatus() {
		return this.status;
	}
	
	/**
	 * sets the type of this Message.  Should be one of the Message.STATUS_ constants.
	 * only allows you to change the status of an outgoing message
	 * @param messageStatus
	 */
	public void setStatus(Status messageStatus) {
		this.status = messageStatus;
	}

	/**
	 * Gets the MSISDN (phone number) of the sender of this message.
	 * @return
	 */
	public String getSenderMsisdn() {
		return this.senderMsisdn;
	}
	
	/**
	 * sets the sender number of an outgoing message, 
	 * usually done once it is assigned to an outgoing device, 
	 * if the MSISDN is known, or manually assigned to the device.
	 * @param senderPhoneNumber new value for {@link #senderMsisdn}
	 */
	public void setSenderMsisdn(String senderPhoneNumber) {
		this.senderMsisdn = senderPhoneNumber;
	}
	
	/**
	 * Sets {@link #recipientMsisdn}
	 * @param recipientPhoneNumber new value for {@link #recipientMsisdn}
	 */
	public void setRecipientMsisdn(String recipientPhoneNumber) {
		this.recipientMsisdn = recipientPhoneNumber;
	}
	
	/**
	 * Sets {@link #recipientSmsPort}
	 * @param recipientSmsPort new value for {@link #recipientSmsPort}
	 */
	public void setRecipientSmsPort(int recipientSmsPort) {
		this.recipientSmsPort = recipientSmsPort;
	}
	
	/**
	 * Gets the MSISDN (phone number) of the recipient of this message.
	 * @return
	 */
	public String getRecipientMsisdn() {
		return this.recipientMsisdn;
	}
	
	/**
	 * Gets the sms port of the recipient of this message, or -1
	 * if none is specified.
	 * @return {@link #recipientSmsPort}
	 */
	public int getRecipientSmsPort() {
		return this.recipientSmsPort;
	}
	
	/**
	 * Gets the text content of this message.
	 * @return {@link #textMessageContent}
	 */
	public String getTextContent() {
		return this.getTextMessageContent();
	}
	
	/**
	 * Gets the binary content of this message.
	 * @return {@link #binaryMessageContent}
	 */
	public byte[] getBinaryContent() {
		return this.binaryMessageContent;
	}
	
	/**
	 * Gets the number of SMS sent.
	 * @return the number of parts this message was sent as
	 */
	public int getNumberOfSMS() {
		return this.getSmsPartsCount() == 0 ? this.getExpectedSmsCount() : this.getSmsPartsCount();
	}
	
	/**
	 * Gets the date at which this message was sent (messages of TYPE_SENT)
	 * or received (messages of TYPE_RECEIVED).
	 * @return
	 */
	public long getDate() {
		return this.date;
	}

	/**
	 * @return the SMSC reference number of this Message.  this appears after a message is sent, so that 
	 * delivery reciepts can be matched up to previous messages.
	 */
	public Integer getSmscReference() {
		return this.smscReference;
	}
	/**
	 * sets the SMSC reference number of this Message.  this appears after a message is sent, so that 
	 * delivery reciepts can be matched up to previous messages.
	 * Don't set this for incoming messages
	 * @param smscReference
	 */
	public void setSmscReference(int smscReference) {
		this.smscReference = smscReference;
	}
	
	/** @return the retries left for this message */
	public int getRetriesRemaining() {
		return this.retriesRemaining;
	}
	/** sets the retries left for this message */
	public void setRetriesRemaining(int retries) {
		this.retriesRemaining = retries;
	}
	
	public String getEndpointId() {
		return endpointId;
	}
	public void setEndpointId(String endpointId) {
		this.endpointId = endpointId;
	}

	/**
	 * Check whether the content of this message is binary or text
	 * @return <code>true</code> if the content of this message is binary; <code>false</code> otherwise.
	 */
	public boolean isBinaryMessage() {
		return this.binaryMessageContent != null;
	}

	/** @return the number of SMS parts that we'd expect this message to take */
	private int getExpectedSmsCount() {
		if(this.isBinaryMessage()) {
			int octetCount = this.getBinaryContent().length;
			if(octetCount <= SMS_LENGTH_LIMIT_BINARY) {
				return 1;
			} else {
				return (int) Math.ceil(octetCount / (double)SMS_MULTIPART_LENGTH_LIMIT_BINARY);
			}
		} else {
			int expectedNumberOfSmsParts = FrontlineMessage.getExpectedNumberOfSmsParts(this.getTextContent());
			if(expectedNumberOfSmsParts == 0) {
				// the method used above can return 0 in some cases.  An empty message will still cost money.
				expectedNumberOfSmsParts = 1;
			}
			return expectedNumberOfSmsParts;
		}
	}
	
//> STATIC FACTORY METHODS
	/**
	 * Creates an binary incoming message in the internal data structure.
	 * @param dateReceived The date this message was received.
	 * @param senderMsisdn The MSISDN (phone number) of the sender of this message.
	 * @param recipientMsisdn The MSISDN (phone number) of the recipient of this message.
	 * @param recipientPort 
	 * @param content 
	 * @return Message object representing the sent message.
	 */
	public static FrontlineMessage createBinaryIncomingMessage(long dateReceived, String senderMsisdn, String recipientMsisdn, int recipientPort, byte[] content) {
		FrontlineMessage m = new FrontlineMessage();
		m.type = Type.RECEIVED;
		m.status = Status.RECEIVED;
		m.setDate(dateReceived);
		m.senderMsisdn = senderMsisdn;
		m.recipientMsisdn = recipientMsisdn;
		m.recipientSmsPort = recipientPort;
		m.binaryMessageContent = content;
		m.setTextMessageContent(HexUtils.encode(content));
		return m;
	}
	
	/**
	 * Creates an binary incoming message in the internal data structure.
	 * @param dateReceived The date this message was received.
	 * @param senderMsisdn The MSISDN (phone number) of the sender of this message.
	 * @param recipientMsisdn The MSISDN (phone number) of the recipient of this message.
	 * @param recipientPort 
	 * @param content 
	 * @return Message object representing the sent message.
	 */
	public static FrontlineMessage createBinaryIncomingMessage(long dateReceived, String senderMsisdn, String recipientMsisdn, 
			int recipientPort, byte[] content, String imsiNumber, String serialNumber) {
		FrontlineMessage m = new FrontlineMessage();
		m.type = Type.RECEIVED;
		m.status = Status.RECEIVED;
		m.setDate(dateReceived);
		m.senderMsisdn = senderMsisdn;
		m.recipientMsisdn = recipientMsisdn;
		m.recipientSmsPort = recipientPort;
		m.binaryMessageContent = content;
		m.endpointId = serialNumber + "@" + imsiNumber ;
		m.setTextMessageContent(HexUtils.encode(content));
		return m;
	}


	/**
	 * Creates an binary outgoing message in the internal data structure.
	 * @param dateSent The date at which this message was sent.
	 * @param senderMsisdn The MSISDN (phone number) of the sender of this message
	 * @param recipientMsisdn The MSISDN (phone number) of the recipient of this message
	 * @param recipientPort 
	 * @param content
	 * @return a Message object representing the received message.
	 * 
	 * FIXME rename this to createOutgoingFormMessage as that is what it is.
	 */
	public static FrontlineMessage createBinaryOutgoingMessage(long dateSent, String senderMsisdn, String recipientMsisdn, int recipientPort, byte[] content) {
		FrontlineMessage m = new FrontlineMessage();
		m.type = Type.OUTBOUND;
		m.status = Status.DRAFT;
		m.setDate(dateSent);
		m.senderMsisdn = senderMsisdn;
		m.recipientMsisdn = recipientMsisdn;
		m.recipientSmsPort = recipientPort;
		m.binaryMessageContent = content;
		m.setTextMessageContent(HexUtils.encode(content));
		return m;
	}
	
	/**
	 * Creates an outgoing message in the internal data structure.
	 * @param dateSent The date at which this message was sent.
	 * @param senderMsisdn The MSISDN (phone number) of the sender of this message
	 * @param recipientMsisdn The MSISDN (phone number) of the recipient of this message
	 * @param messageContent The text content of this message.
	 * @return a Message object representing the received message.
	 */
	public static FrontlineMessage createOutgoingMessage(long dateSent, String senderMsisdn, String recipientMsisdn, String messageContent) {
		FrontlineMessage m = new FrontlineMessage();
		m.type = Type.OUTBOUND;
		m.status = Status.DRAFT;
		m.setDate(dateSent);
		m.senderMsisdn = senderMsisdn;
		m.recipientMsisdn = recipientMsisdn;
		m.setTextMessageContent(messageContent);
		return m;
	}

	/**
	 * Creates an incoming message in the internal data structure.
	 * @param dateReceived The date this message was received.
	 * @param senderMsisdn The MSISDN (phone number) of the sender of this message.
	 * @param recipientMsisdn The MSISDN (phone number) of the recipient of this message.
	 * @param messageContent The text content of this message.
	 * @returna Message object representing the sent message.
	 */
	public static FrontlineMessage createIncomingMessage(long dateReceived, String senderMsisdn, String recipientMsisdn, String messageContent) {
		FrontlineMessage m = new FrontlineMessage();
		m.type = Type.RECEIVED;
		m.status = Status.RECEIVED;
		m.setDate(dateReceived);
		m.senderMsisdn = senderMsisdn;
		m.recipientMsisdn = recipientMsisdn;
		m.setTextMessageContent(messageContent);
		return m;
	}
	
	/**
	 * Creates an incoming message in the internal data structure.
	 * @param dateReceived The date this message was received.
	 * @param senderMsisdn The MSISDN (phone number) of the sender of this message.
	 * @param recipientMsisdn The MSISDN (phone number) of the recipient of this message.
	 * @param messageContent The text content of this message.
	 * @param imsiNumber The imsi number of the linked modem.
	 * @param serialNumber The serial number of the linked modem.
	 * @returna Message object representing the sent message.
	 */
	public static FrontlineMessage createIncomingMessage(long dateReceived, String senderMsisdn, String recipientMsisdn, String messageContent, String imsiNumber, String serialNumber) {
		FrontlineMessage m = new FrontlineMessage();
		m.type = Type.RECEIVED;
		m.status = Status.RECEIVED;
		m.setDate(dateReceived);
		m.senderMsisdn = senderMsisdn;
		m.recipientMsisdn = recipientMsisdn;
		m.endpointId = serialNumber + "@" + imsiNumber;
		m.setTextMessageContent(messageContent);
		return m;
	}
	
//> GENERATED METHODS
	/**
	 * {@link #status} and {@link #smscReference} are not included in {@link #equals(Object)} or {@link #hashCode()}
	 * as they are liable to change throughout a message's lifetime.  Likewise, {@link #senderMsisdn} is ignored for
	 * {@link Type#OUTBOUND} and {@link #recipientMsisdn} is ignored for {@link Type#RECEIVED} and {@link Type#DELIVERY_REPORT}.
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (getDate() ^ (getDate() >>> 32));
		result = prime * result + Arrays.hashCode(binaryMessageContent);
		result = prime * result
				+ ((getTextMessageContent() == null) ? 0 : getTextMessageContent().hashCode());
		
		if(!(type == Type.RECEIVED || type == Type.DELIVERY_REPORT)) {
			result = prime * result
					+ ((recipientMsisdn == null) ? 0 : recipientMsisdn.hashCode());
		}
		
		result = prime * result + recipientSmsPort;
		result = prime * result + retriesRemaining;
		
		if(type != Type.OUTBOUND) {
			result = prime * result
					+ ((senderMsisdn == null) ? 0 : senderMsisdn.hashCode());
		}
		
		result = prime * result + getSmsPartsCount();
		result = prime * result + (type==null ? 0 : type.hashCode());
		return result;
	}

	/** 
	 * {@link #status} and {@link #smscReference} are not included in {@link #equals(Object)} or {@link #hashCode()}
	 * as they are liable to change throughout a message's lifetime.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FrontlineMessage other = (FrontlineMessage) obj;
		if (getDate() != other.getDate())
			return false;
		if (!Arrays.equals(binaryMessageContent, other.binaryMessageContent))
			return false;
		if (getTextMessageContent() == null) {
			if (other.getTextMessageContent() != null)
				return false;
		} else if (!getTextMessageContent().equals(other.getTextMessageContent()))
			return false;
		
		if(!(type == Type.RECEIVED || type == Type.DELIVERY_REPORT)) {
			if (recipientMsisdn == null) {
				if (other.recipientMsisdn != null)
					return false;
			} else if (!recipientMsisdn.equals(other.recipientMsisdn))
				return false;
		}
		
		if (recipientSmsPort != other.recipientSmsPort)
			return false;
		if (retriesRemaining != other.retriesRemaining)
			return false;
		
		if(type != Type.OUTBOUND) {
			if (senderMsisdn == null) {
				if (other.senderMsisdn != null)
					return false;
			} else if (!senderMsisdn.equals(other.senderMsisdn))
				return false;
		}
		
		if (getSmsPartsCount() != other.getSmsPartsCount())
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	
	/**
	 * Calculate the expected number of SMS parts required to send a text message.
	 * This method <strong>will not work</strong> for <em>binary</em> messages.
	 * @param message the text content of the message
	 * @return the number of SMS parts that we'd expect the supplied message to use, or <code>0</code> if no supplied message has zero length.
	 */
	public static int getExpectedNumberOfSmsParts(String message) {
		int messageLength = message.length();
		
		boolean areAllCharactersValidGSM = GsmAlphabet.areAllCharactersValidGSM(message);
		int singleMessageCharacterLimit, multipartMessageCharacterLimit;
		
		if(areAllCharactersValidGSM) {
			singleMessageCharacterLimit = FrontlineMessage.SMS_LENGTH_LIMIT;
			multipartMessageCharacterLimit = FrontlineMessage.SMS_MULTIPART_LENGTH_LIMIT;
		} else {
			// It appears there are some unicode-only characters here.  We should therefore
			// treat this message as if it will be sent as unicode.
			singleMessageCharacterLimit = FrontlineMessage.SMS_LENGTH_LIMIT_UCS2;
			multipartMessageCharacterLimit = FrontlineMessage.SMS_MULTIPART_LENGTH_LIMIT_UCS2;
		}

		if (messageLength > getTotalLengthAllowed(message)) {
			return (int)Math.ceil((double)messageLength / (double)multipartMessageCharacterLimit);
		} else {
			if (messageLength <= singleMessageCharacterLimit) {
				return messageLength == 0 ? 0 : 1;
			} else {
				return (int)Math.ceil(messageLength / (double)multipartMessageCharacterLimit);
			}
		}
	}

	public void setDate(long date) {
		this.date = date;
	}

	public static int getTotalLengthAllowed(String message) {
		boolean areAllCharactersValidGSM = GsmAlphabet.areAllCharactersValidGSM(message);
		if (areAllCharactersValidGSM) {
			return FrontlineMessage.SMS_LENGTH_LIMIT + FrontlineMessage.SMS_MULTIPART_LENGTH_LIMIT * (FrontlineMessage.SMS_LIMIT - 1);
		} else {
			return FrontlineMessage.SMS_LENGTH_LIMIT_UCS2 + FrontlineMessage.SMS_MULTIPART_LENGTH_LIMIT_UCS2 * (FrontlineMessage.SMS_LIMIT - 1);
		}
	}

	public void setTextMessageContent(String textMessageContent) {
		this.textMessageContent = textMessageContent;
	}

	// FIXME what does this method provide which getTextContent() does not?  N.B. obviously don't rename the field unless appropriate hibernate mapping is applied
	private String getTextMessageContent() {
		return textMessageContent;
	}

	public void setSmsPartsCount(int smsPartsCount) {
		this.smsPartsCount = smsPartsCount;
	}

	public int getSmsPartsCount() {
		return smsPartsCount;
	}
}
