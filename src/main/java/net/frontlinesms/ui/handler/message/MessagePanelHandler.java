/**
 * 
 */
package net.frontlinesms.ui.handler.message;

// TODO Remove static imports
import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_NO_CONTACT_SELECTED;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_COST;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_FIRST;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_HELP;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_MSG_NUMBER;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_REMAINING_CHARS;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_SECOND;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_THIRD;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_TOO_MANY_MESSAGES;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_TF_MESSAGE;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_TF_RECIPIENT;

import java.awt.Color;
import java.util.List;
import java.util.regex.Pattern;

import net.frontlinesms.AppProperties;
import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.data.domain.Group;
import net.frontlinesms.ui.Icon;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.UiGeneratorControllerConstants;
import net.frontlinesms.ui.handler.contacts.ContactSelecter;
import net.frontlinesms.ui.handler.contacts.GroupSelecterDialog;
import net.frontlinesms.ui.handler.contacts.SingleGroupSelecterDialogOwner;
import net.frontlinesms.ui.handler.keyword.BaseActionDialog;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

import org.apache.log4j.Logger;
import org.smslib.util.GsmAlphabet;

import thinlet.Thinlet;

/**
 * Controller for a panel which allows sending of text SMS messages
 * @author Alex
 */
public class MessagePanelHandler implements ThinletUiEventHandler, SingleGroupSelecterDialogOwner {
//> STATIC CONSTANTS
	/** UI XML File Path: the panel containing the messaging controls */
	private static final String UI_FILE_MESSAGE_PANEL = "/ui/core/messages/pnComposeMessage.xml";
	
//> THINLET COMPONENTS
	/** Thinlet component name: Button to send message */
	private static final String COMPONENT_BT_SEND = "btSend";
	private static final String COMPONENT_LB_ICON = "lbIcon";

//> INSTANCE PROPERTIES
	private final Logger LOG = FrontlineUtils.getLogger(this.getClass());
	/** The {@link UiGeneratorController} that shows the tab. */
	private final UiGeneratorController uiController;
	/** The parent component */
	private Object messagePanel;
	/** The number of people the current SMS will be sent to */
	private int numberToSend = 1;
	/** The boolean stipulating whether the recipient field should be displayed */
	private boolean shouldDisplayRecipientField;
	/** The boolean stipulating whether the recipient field should be cleared after a message is sent*/
	private boolean clearRecipientFieldOnSend;
	/** The boolean stipulating whether we should check the length of the message (we don't in the auto-reply, for example) */
	private boolean shouldCheckMaxMessageLength;
	/** The number of recipients, used to estimate the cost of the message */
	private int numberOfRecipients;

//> CONSTRUCTORS
	/**
	 * @param uiController
	 */
	private MessagePanelHandler(UiGeneratorController uiController, boolean shouldDisplay, boolean shouldCheckMaxMessageLength, int numberOfRecipients) {
		this.uiController 				 = uiController;
		this.shouldDisplayRecipientField = shouldDisplay;
		this.clearRecipientFieldOnSend = true;
		this.shouldCheckMaxMessageLength = shouldCheckMaxMessageLength;
		this.numberOfRecipients 		 = numberOfRecipients; 
	}
	
	private synchronized void init() {
		assert(this.messagePanel == null) : "This has already been initialised.";
		this.messagePanel = uiController.loadComponentFromFile(UI_FILE_MESSAGE_PANEL, this);
		
		Object pnRecipient = find(UiGeneratorControllerConstants.COMPONENT_PN_MESSAGE_RECIPIENT);
		uiController.setVisible(pnRecipient, shouldDisplayRecipientField);
		
		Object lbTooManyMessages = find(UiGeneratorControllerConstants.COMPONENT_LB_TOO_MANY_MESSAGES);
		if (lbTooManyMessages != null) {
			uiController.setVisible(lbTooManyMessages, false);
			uiController.setColor(lbTooManyMessages, "foreground", Color.RED);
		}
		
		updateMessageDetails(find(COMPONENT_TF_RECIPIENT), "");
	}

	private Object find(String component) {
		return Thinlet.find(this.messagePanel, component);
	}

//> ACCESSORS
	/** @return {@link #messagePanel} */
	public Object getPanel() {
		return this.messagePanel;
	}
	
	private double getCostPerSms() {
		return AppProperties.getInstance().getCostPerSmsSent();
	}
	
	/**
	 * Adds a constant substitution marker to the SMS text.
	 * @param currentText 
	 * @param textArea 
	 * @param type The constant that should be inserted
	 */
	public void addConstantToCommand(String currentText, Object tfMessage, String type) {
		BaseActionDialog.addConstantToCommand(uiController, currentText, tfMessage, type);
		
		updateMessageDetails(find(COMPONENT_TF_RECIPIENT), uiController.getText(tfMessage));
	}
	
//> THINLET UI METHODS
	/** Sets the method called by the send button at the bottom of the compose message panel */
	public void setSendButtonMethod(ThinletUiEventHandler eventHandler, Object rootComponent, String methodCall) {
		Object sendButton = find(COMPONENT_BT_SEND);
		uiController.setAction(sendButton, methodCall, rootComponent, eventHandler);
	}
	
	/**
	 * Extract message details from the controls in the panel, and send an SMS.
	 */
	public void send() {
		String recipient = uiController.getText(find(COMPONENT_TF_RECIPIENT));
		String message = uiController.getText(find(COMPONENT_TF_MESSAGE));
		
		if (recipient.length() == 0) {
			uiController.alert(InternationalisationUtils.getI18nString(FrontlineSMSConstants.MESSAGE_BLANK_PHONE_NUMBER));
			return;
		} 
		this.uiController.getFrontlineController().sendTextMessage(recipient, message);
		
		this.clearComponents();
	}
	
	private void clearComponents() {
		// We clear the components
		if(clearRecipientFieldOnSend){
			uiController.setText(find(COMPONENT_TF_RECIPIENT), "");
		}
		uiController.setText(find(COMPONENT_TF_MESSAGE), "");
		uiController.setText(find(COMPONENT_LB_REMAINING_CHARS), String.valueOf(FrontlineMessage.SMS_LENGTH_LIMIT));
		uiController.setText(find(COMPONENT_LB_MSG_NUMBER), "0");
		uiController.setIcon(find(COMPONENT_LB_FIRST), Icon.SMS_DISABLED);
		uiController.setIcon(find(COMPONENT_LB_SECOND), Icon.SMS_DISABLED);
		uiController.setIcon(find(COMPONENT_LB_THIRD), Icon.SMS_DISABLED);
		uiController.setText(find(COMPONENT_LB_COST), InternationalisationUtils.formatCurrency(0));
		if (shouldCheckMaxMessageLength) // Otherwise this component doesn't exist
			uiController.setVisible(find(COMPONENT_LB_TOO_MANY_MESSAGES), false);

		Object sendButton = find(COMPONENT_BT_SEND);
		if (sendButton != null) uiController.setEnabled(sendButton, false);
	}

	public void sendToGroup() {
		Object attachedObject = this.uiController.getAttachedObject(find(COMPONENT_TF_RECIPIENT));
		
		if (attachedObject != null && attachedObject instanceof Group) {
			List<Contact> recipientList = this.uiController.getFrontlineController().getGroupMembershipDao().getMembers((Group) attachedObject);
			for (Contact contact : recipientList) {
				this.uiController.getFrontlineController().sendTextMessage(contact.getPhoneNumber(), this.uiController.getText(find(COMPONENT_TF_MESSAGE)));
			}
		}
		
		this.clearComponents();
	}
	
	/**
	 * Event triggered when the message recipient has changed
	 * @param text the new text value for the message recipient
	 * 
	 */
	public void recipientChanged(Object recipientField, String message) {
		this.uiController.setAttachedObject(recipientField, null);
		this.uiController.setIcon(find(COMPONENT_LB_ICON), Icon.USER_STATUS_ACTIVE);
		this.numberToSend = 1;
		
		this.updateMessageDetails(recipientField, message);
	}
	
	/** Method which triggers showing of the contact selecter. */
	public void selectMessageRecipient() {
		ContactSelecter contactSelecter = new ContactSelecter(this.uiController);
		final boolean shouldHaveEmail = false;
		contactSelecter.show(InternationalisationUtils.getI18nString(FrontlineSMSConstants.SENTENCE_SELECT_MESSAGE_RECIPIENT_TITLE), "setRecipientTextfield(contactSelecter_contactList, contactSelecter)", null, this, shouldHaveEmail);
	}
	
	/** Method which triggers showing of the group selecter. */
	public void selectGroup() {
		GroupSelecterDialog groupSelect = new GroupSelecterDialog(this.uiController, this, uiController.getFrontlineController().getGroupDao());
		groupSelect.init(this.uiController.getRootGroup());
		
		groupSelect.show();
	}
	
	public void groupSelectionCompleted(Group group) {
		this.numberToSend = this.uiController.getFrontlineController().getGroupMembershipDao().getMemberCount(group);
		
		Object tfRecipient = find(UiGeneratorControllerConstants.COMPONENT_TF_RECIPIENT);
		this.uiController.setText(tfRecipient, group.getName() + " (" + this.numberToSend + ")");
		this.uiController.setAttachedObject(tfRecipient, group);
		this.uiController.setIcon(find(COMPONENT_LB_ICON), Icon.GROUP);
		setSendButtonMethod(this, this.messagePanel, "sendToGroup");
		
		this.updateMessageDetails(group, this.uiController.getText(find(UiGeneratorControllerConstants.COMPONENT_TF_MESSAGE)));
	}
	
	/**
	 * Sets the phone number of the selected contact.
	 * 
	 * This method is triggered by the contact selected, as detailed in {@link #selectMessageRecipient()}.
	 * 
	 * @param contactSelecter_contactList
	 * @param dialog
	 */
	public void setRecipientTextfield(Object contactSelecter_contactList, Object dialog) {
		Object 	tfRecipient = find(UiGeneratorControllerConstants.COMPONENT_TF_RECIPIENT),
				tfMessage	= find(UiGeneratorControllerConstants.COMPONENT_TF_MESSAGE);
		Object selectedItem = uiController.getSelectedItem(contactSelecter_contactList);
		if (selectedItem == null) {
			uiController.alert(InternationalisationUtils.getI18nString(FrontlineSMSConstants.MESSAGE_NO_CONTACT_SELECTED));
			return;
		}
		Contact selectedContact = uiController.getContact(selectedItem);
		uiController.setText(tfRecipient, selectedContact.getPhoneNumber());
		uiController.remove(dialog);
		
		setSendButtonMethod(this, null, "send");
		
		this.updateCost();
		
		// The recipient text has changed, we check whether the send button should be enabled
		this.recipientChanged(tfRecipient, uiController.getText(tfMessage));
	}
	
	/**
	 * @param recipients Either the recipients field's text or the group attached
	 * @param message the new text value for the message body
	 */
	public void updateMessageDetails(Object recipients, String message) {
		int messageLength = message.length();
		
		Object sendButton = find(COMPONENT_BT_SEND);
		boolean areAllCharactersValidGSM = GsmAlphabet.areAllCharactersValidGSM(message);
		int totalLengthAllowed = FrontlineMessage.getTotalLengthAllowed(message);
		
		boolean shouldEnableSendButton = (messageLength > 0 && (!shouldCheckMaxMessageLength || messageLength <= totalLengthAllowed));
		if (shouldDisplayRecipientField) {
			if (recipients instanceof Group) {
				shouldEnableSendButton &= this.numberToSend > 0;
			} else if (recipients != null) {
				shouldEnableSendButton &= !this.uiController.getText(recipients).isEmpty();
			}
		}
											
		
		if (sendButton != null)
			uiController.setEnabled(sendButton, shouldEnableSendButton);
		
		int singleMessageCharacterLimit;
		int multipartMessageCharacterLimit;
		if(areAllCharactersValidGSM) {
			singleMessageCharacterLimit = FrontlineMessage.SMS_LENGTH_LIMIT;
			multipartMessageCharacterLimit = FrontlineMessage.SMS_MULTIPART_LENGTH_LIMIT;
		} else {
			// It appears there are some unicode-only characters here.  We should therefore
			// treat this message as if it will be sent as unicode.
			singleMessageCharacterLimit = FrontlineMessage.SMS_LENGTH_LIMIT_UCS2;
			multipartMessageCharacterLimit = FrontlineMessage.SMS_MULTIPART_LENGTH_LIMIT_UCS2;
		}
		
		Object tfMessage = find(COMPONENT_TF_MESSAGE);
		Object lbTooManyMessages = find(COMPONENT_LB_TOO_MANY_MESSAGES);
		final int numberOfMsgs = FrontlineMessage.getExpectedNumberOfSmsParts(message);
		
		double costEstimate;
		int remaining;		
		if (shouldCheckMaxMessageLength && messageLength > totalLengthAllowed) {
			remaining = 0;
			costEstimate = 0;
			
			uiController.setVisible(lbTooManyMessages, true);
			uiController.setColor(tfMessage, "foreground", Color.RED);
		} else {
			if (shouldCheckMaxMessageLength) {
				uiController.setVisible(lbTooManyMessages, false);
				uiController.setColor(tfMessage, "foreground", Color.BLACK);
			}
			
			if (messageLength <= singleMessageCharacterLimit) {
				remaining = (messageLength % singleMessageCharacterLimit) == 0 ? 0
						: singleMessageCharacterLimit - (messageLength % singleMessageCharacterLimit);	
			} else {
				int charCount = messageLength - singleMessageCharacterLimit;
				remaining = (charCount % multipartMessageCharacterLimit) == 0 ? 0
						: multipartMessageCharacterLimit - ((charCount % multipartMessageCharacterLimit));
			}
			
			costEstimate = numberOfMsgs * this.getCostPerSms() * this.numberToSend;
		}
		
		// The message will actually cost {numberOfRecipients} times the calculated cost
		costEstimate *= numberOfRecipients;
		
		uiController.setText(find(COMPONENT_LB_REMAINING_CHARS), String.valueOf(remaining));
		uiController.setText(find(COMPONENT_LB_COST), InternationalisationUtils.formatCurrency(costEstimate));
		uiController.setVisible(find(COMPONENT_LB_HELP), false);
		
		uiController.setText(find(COMPONENT_LB_MSG_NUMBER), String.valueOf(numberOfMsgs));
		uiController.setIcon(find(COMPONENT_LB_FIRST), Icon.SMS_DISABLED);
		uiController.setIcon(find(COMPONENT_LB_SECOND), Icon.SMS_DISABLED);
		uiController.setIcon(find(COMPONENT_LB_THIRD), Icon.SMS_DISABLED);
		
		if (numberOfMsgs >= 1) uiController.setIcon(find(COMPONENT_LB_FIRST), Icon.SMS);
		if (numberOfMsgs >= 2) uiController.setIcon(find(COMPONENT_LB_SECOND), Icon.SMS);
		if (numberOfMsgs == 3) uiController.setIcon(find(COMPONENT_LB_THIRD), Icon.SMS);
		if (numberOfMsgs > 3) uiController.setIcon(find(COMPONENT_LB_THIRD), Icon.SMS_ADD);
		
		if (Pattern.matches(".*\\$[^ ]*\\}.*", message)) {
			uiController.setVisible(find(COMPONENT_LB_HELP), true);
		}
	}
	
	/**
	 * Sets the phone number of the selected contact.
	 * 
	 * @param contactSelecter_contactList
	 * @param dialog
	 */
	public void homeScreen_setRecipientTextfield(Object contactSelecter_contactList, Object dialog) {
		Object tfRecipient = find(COMPONENT_TF_RECIPIENT);
		Object selectedItem = uiController.getSelectedItem(contactSelecter_contactList);
		if (selectedItem == null) {
			uiController.alert(InternationalisationUtils.getI18nString(MESSAGE_NO_CONTACT_SELECTED));
			return;
		}
		Contact selectedContact = uiController.getContact(selectedItem);
		uiController.setText(tfRecipient, selectedContact.getPhoneNumber());
		uiController.remove(dialog);
		this.numberToSend = 1;
		this.updateCost();
	}
	
	public void hideSendButton() {
		this.uiController.setVisible(find(COMPONENT_BT_SEND), false);
	}
	
	public void updateCost() {
		LOG.trace("Updating message panel cost estimate");
		
		this.uiController.setText(find(COMPONENT_LB_MSG_NUMBER), String.valueOf(numberToSend));		
		this.uiController.setText(find(COMPONENT_LB_COST), InternationalisationUtils.formatCurrency(AppProperties.getInstance().getCostPerSmsSent() * numberToSend));
		
		LOG.trace("EXIT");
	}
	
	public void setClearRecipientFieldOnSend(boolean clearRecipientFieldOnSend) {
		this.clearRecipientFieldOnSend = clearRecipientFieldOnSend;
	}

//> INSTANCE HELPER METHODS

//> STATIC FACTORIES
	/**
	 * Create and initialise a new {@link MessagePanelHandler}.
	 * @return a new, initialised instance of {@link MessagePanelHandler}
	 */
	public static final MessagePanelHandler create(UiGeneratorController ui, boolean shouldDisplayRecipientField, boolean checkMaxMessageLength, int numberOfRecipients) {
		MessagePanelHandler handler = new MessagePanelHandler(ui, shouldDisplayRecipientField, checkMaxMessageLength, numberOfRecipients);
		handler.init();
		return handler;
	}

//> STATIC HELPER METHODS
}
