/**
 * 
 */
package net.frontlinesms.ui.handler.message;

import static net.frontlinesms.FrontlineSMSConstants.COMMON_DATE;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_MESSAGE;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_RECIPIENT;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_SENDER;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_STATUS;
import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_MESSAGES_DELETED;
import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_MESSAGES_LOADED;
import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_REMOVING_MESSAGES;
import static net.frontlinesms.FrontlineSMSConstants.PROPERTY_FIELD;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_CB_CONTACTS;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_CB_GROUPS;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_COST;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_MSGS_NUMBER;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_PN_BOTTOM;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_RECEIVED_MESSAGES_TOGGLE;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_SENT_MESSAGES_TOGGLE;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_TF_END_DATE;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_TF_START_DATE;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.TAB_MESSAGE_HISTORY;

import java.awt.EventQueue;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import thinlet.Thinlet;
import thinlet.ThinletText;

import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.Order;
import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.domain.FrontlineMultimediaMessage;
import net.frontlinesms.data.domain.Group;
import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.data.domain.FrontlineMessage.Field;
import net.frontlinesms.data.domain.FrontlineMessage.Status;
import net.frontlinesms.data.domain.FrontlineMessage.Type;
import net.frontlinesms.data.events.EntitySavedNotification;
import net.frontlinesms.data.repository.ContactDao;
import net.frontlinesms.data.repository.GroupMembershipDao;
import net.frontlinesms.data.repository.KeywordDao;
import net.frontlinesms.data.repository.MessageDao;
import net.frontlinesms.events.EventObserver;
import net.frontlinesms.events.FrontlineEventNotification;
import net.frontlinesms.ui.Icon;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.UiProperties;
import net.frontlinesms.ui.events.FrontlineUiUpateJob;
import net.frontlinesms.ui.events.TabChangedNotification;
import net.frontlinesms.ui.handler.BaseTabHandler;
import net.frontlinesms.ui.handler.ComponentPagingHandler;
import net.frontlinesms.ui.handler.PagedComponentItemProvider;
import net.frontlinesms.ui.handler.PagedListDetails;
import net.frontlinesms.ui.handler.contacts.GroupSelecterPanel;
import net.frontlinesms.ui.handler.contacts.SingleGroupSelecterPanelOwner;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

/**
 * Handler for the MessageHistory tab.
 * 
 * @author Alex Anderson alex@frontlinesms.com
 * @author Carlos Eduardo Genz
 * <li> kadu(at)masabi(dot)com
 */
public class MessageHistoryTabHandler extends BaseTabHandler implements PagedComponentItemProvider, SingleGroupSelecterPanelOwner, EventObserver {
	
//> CONSTANTS
	/** Path to the Thinlet XML layout file for the message history tab */
	private static final String UI_FILE_MESSAGES_TAB = "/ui/core/messages/messagesTab.xml";

	/** UI Component name: the list of messages */
	public static final String COMPONENT_MESSAGE_LIST = "messageList";
	/** UI Component name: the list of groups.  This is a placeholder which is ultimately replaced. */
	private static final String COMPONENT_GROUP_LIST = "groupListPlaceholder";
	
	/** Number of milliseconds in a day */
	private static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000;

	private static final String COMPONENT_GROUP_PANEL = "pnGroups";
	private static final String COMPONENT_CONTACT_LIST = "lsContacts";
	private static final String COMPONENT_CONTACT_PANEL = "pnContactList";
	private static final String COMPONENT_KEYWORD_LIST = "lsKeywords";
	private static final String COMPONENT_KEYWORD_PANEL = "pnKeywordList";
	
//> INSTANCE PROPERTIES
	/** Logger */
	private final Logger LOG = FrontlineUtils.getLogger(this.getClass());
	
	/** DAO for {@link Contact}s */
	private final ContactDao contactDao;
	private final GroupMembershipDao groupMembershipDao;
	/** DAO for {@link Keyword}s */
	private final KeywordDao keywordDao;
	/** DAO for {@link FrontlineMessage}s */
	private final MessageDao messageDao;
	
//> UI COMPONENTS
	/** UI Component: table of messages */
	private Object messageListComponent;
	/** UI Component: checkbox: show sent messages? */
	private Object showSentMessagesComponent;
	/** UI Component: checkbox: show received messages? */
	private Object showReceivedMessagesComponent;
	/** UI Component: list of keywords */
	private Object keywordListComponent;
	/** UI Component: list of contacts */
	private Object contactListComponent;
	/** UI Component: tree of groups */
	private Object groupTreeComponent;

	/** Paging handler for the list of messages. */
	private ComponentPagingHandler messagePagingHandler;
	/** Paging handler for the list of contacts. */
	private ComponentPagingHandler contactListPagingHandler;
	/** Paging handler for the list of keywords. */
	private ComponentPagingHandler keywordListPagingHandler;

	/** Group selecter */
	private GroupSelecterPanel groupSelecter;
	
	/** Start date of the message history, or <code>null</code> if none has been set. */
	private Long messageHistoryStart;
	/** End date of the message history, or <code>null</code> if none has been set. */
	private Long messageHistoryEnd;
	/** The number of people the current SMS will be sent to */
	private int numberToSend = 1;
	/** The selected lines in the left panel  */
	private Group selectedGroup;
	private Contact selectedContact;
	private Keyword selectedKeyword;
	
//> CONSTRUCTORS
	/**
	 * @param ui value for {@link #ui}
	 */
	public MessageHistoryTabHandler(UiGeneratorController ui) {
		super(ui);
		this.contactDao = ui.getFrontlineController().getContactDao();
		this.keywordDao = ui.getFrontlineController().getKeywordDao();
		this.messageDao = ui.getFrontlineController().getMessageDao();
		this.groupMembershipDao = ui.getFrontlineController().getGroupMembershipDao();
	}

//> ACCESSORS
	/** Refresh the view. */
	public void refresh() {
		resetMessageHistoryFilter();
	}

	/**
	 * UI event called when the user changes tab
	 */
	public void notify(FrontlineEventNotification notification) {
		if(notification instanceof EntitySavedNotification<?>) {
			Object entity = ((EntitySavedNotification<?>) notification).getDatabaseEntity();
			if(entity instanceof FrontlineMessage) {
				if(entity instanceof FrontlineMultimediaMessage) {
					// TODO check if the list is visible before refreshing
					refresh();
				}
			}
		} else if(notification instanceof TabChangedNotification) {
			// This object is registered to the UIGeneratorController and get notified when the users changes tab
			String newTabName = ((TabChangedNotification) notification).getNewTabName();
			if (newTabName.equals(TAB_MESSAGE_HISTORY)) {
				this.refresh();
				this.ui.setStatus(InternationalisationUtils.getI18NString(MESSAGE_MESSAGES_LOADED));
			}
		}
	}
	
	/**
	 * Shows the message history for the selected contact or group.
	 * @param component group list or contact list
	 */
	public void doShowMessageHistory(Object component) {
		Object attachment = ui.getAttachedObject(ui.getSelectedItem(component));
		
		boolean isGroup = attachment instanceof Group;
		boolean isContact = attachment instanceof Contact;
		boolean isKeyword = attachment instanceof Keyword;
		
		// Select the correct radio option
		ui.setSelected(find("cbContacts"), isContact);
		ui.setSelected(find("cbGroups"), isGroup);
		ui.setSelected(find("cbKeywords"), isKeyword);
		resetMessageHistoryFilter();
		
		// Find which list item should be selected
		Object list = getMessageHistoryFilterList();
		
		// TODO can do this more simply for group selecter
		
		boolean recurse = Thinlet.TREE.equals(Thinlet.getClass(list));
		Object next = ui.getNextItem(list, Thinlet.get(list, ":comp"), recurse);
		while(next != null && !ui.getAttachedObject(next).equals(attachment)) {
			next = ui.getNextItem(list, next, recurse);
		}
		// Little fix for groups - it seems that getNextItem doesn't return the root of the
		// tree, so we never get a positive match.
		if(next == null) next = ui.getItem(list, 0);
		ui.setSelectedItem(list, next);
		updateMessageList();
	}

//> INSTANCE HELPER METHODS
	/** Initialise the tab */
	protected Object initialiseTab() {
		LOG.trace("ENTRY");

		Object tabComponent = ui.loadComponentFromFile(UI_FILE_MESSAGES_TAB, this);
		
		// We register the observer to the UIGeneratorController, which notifies when tabs have changed
		this.ui.getFrontlineController().getEventBus().registerObserver(this);
		
		messageListComponent = ui.find(tabComponent, COMPONENT_MESSAGE_LIST);
		messagePagingHandler = new ComponentPagingHandler(this.ui, this, this.messageListComponent);
		Object pnBottom = ui.find(tabComponent, COMPONENT_PN_BOTTOM);
		ui.add(pnBottom, messagePagingHandler.getPanel(), 0);

		// Cache the contacts list, and add paging
		contactListComponent = ui.find(tabComponent, COMPONENT_CONTACT_LIST);
		contactListPagingHandler = new ComponentPagingHandler(ui, this, this.contactListComponent);
		ui.add(ui.find(tabComponent, COMPONENT_CONTACT_PANEL), contactListPagingHandler.getPanel());

		// Cache the keywords list, and add paging
		keywordListComponent = ui.find(tabComponent, COMPONENT_KEYWORD_LIST);
		keywordListPagingHandler = new ComponentPagingHandler(ui, this, this.keywordListComponent);
		ui.add(ui.find(tabComponent, COMPONENT_KEYWORD_PANEL), keywordListPagingHandler.getPanel());
		
		// Initialise and cache the group tree
		Object oldGroupTreeComponent = ui.find(tabComponent, COMPONENT_GROUP_LIST);
		groupSelecter = new GroupSelecterPanel(ui, this);
		groupSelecter.init(ui.getRootGroup());
		
		Object groupTreeParent = ui.getParent(oldGroupTreeComponent);
		this.groupTreeComponent = groupSelecter.getGroupTreeComponent();
		ui.add(groupTreeParent, groupSelecter.getPanelComponent(), ui.getIndex(groupTreeParent, oldGroupTreeComponent));
		ui.remove(oldGroupTreeComponent);
		

		// Set the types for the message list columns...
		initMessageTableForSorting();
		
		showReceivedMessagesComponent = ui.find(tabComponent, COMPONENT_RECEIVED_MESSAGES_TOGGLE);
		showSentMessagesComponent = ui.find(tabComponent, COMPONENT_SENT_MESSAGES_TOGGLE);
		
		LOG.trace("EXIT");
		return tabComponent;
	}
	
//> GROUP SELECTER METHODS
	/** @see SingleGroupSelecterPanelOwner#groupSelectionChanged(Group) */
	public void groupSelectionChanged(Group selectedGroup) {
		this.selectedGroup = selectedGroup;
		
		updateMessageList();
	}
	
//> LIST PAGING METHODS
	/** @see PagedComponentItemProvider#getListDetails(Object, int, int) */
	public PagedListDetails getListDetails(Object list, int startIndex, int limit) {
		if(list.equals(this.messagePagingHandler.getList())) {
			return getMessageListPagingDetails(startIndex, limit);
		} else if(list.equals(this.contactListPagingHandler.getList())) {
			return getContactListPagingDetails(startIndex, limit);
		} else if(list.equals(this.keywordListPagingHandler.getList())) {
			return getKeywordListPagingDetails(startIndex, limit);
		} else throw new IllegalStateException();
	}
	
	/** @return {@link PagedListDetails} for {@link #keywordListComponent} */
	private PagedListDetails getKeywordListPagingDetails(int startIndex, int limit) {
		int totalItemCount = this.keywordDao.getTotalKeywordCount();
		
		List<Keyword> keywords = this.keywordDao.getAllKeywords(startIndex, limit);
		Object[] keywordRows = new Object[keywords.size() + 1];
		keywordRows[0] = getAllMessagesListItem();
		for (int i = 0; i < keywords.size(); i++) {
			Keyword k = keywords.get(i);
			keywordRows[i+1] = ui.createListItem(k);
		}
		
		return new PagedListDetails(totalItemCount, keywordRows);
	}

	/** @return {@link PagedListDetails} for {@link #contactListComponent} */
	private PagedListDetails getContactListPagingDetails(int startIndex, int limit) {
		int totalItemCount = this.contactDao.getContactCount();
		List<Contact> contacts = this.contactDao.getAllContactsSorted(startIndex, limit, Contact.Field.NAME, Order.ASCENDING);
		Object[] contactRows = new Object[contacts.size() + 1];
		contactRows[0] = getAllMessagesListItem();
		for (int i = 0; i < contacts.size(); i++) {
			Contact c = contacts.get(i);
			contactRows[i+1] = ui.createListItem(c);
		}
		
		return new PagedListDetails(totalItemCount, contactRows);
	}


	/** @return {@link PagedListDetails} for {@link #messageListComponent} */
	private PagedListDetails getMessageListPagingDetails(int startIndex, int limit) {
		int messageCount = getMessageCount();
		numberToSend = messageCount;
		
		List<FrontlineMessage> messages = getListMessages(startIndex, limit);
		Object[] messageRows = new Object[messages.size()];
		for (int i = 0; i < messages.size(); i++) {
			FrontlineMessage m = messages.get(i);
			messageRows[i] = ui.getRow(m);
		}
		
		return new PagedListDetails(messageCount, messageRows);
	}
	
	/** @return total number of messages to be displayed in the message list. */
	private int getMessageCount() {
		Object filterList = getMessageHistoryFilterList();
		Object selectedItem = ui.getSelectedItem(filterList);

		if (selectedItem == null) {
			return 0;
		} else {
			final FrontlineMessage.Type messageType = getSelectedMessageType();
			int selectedIndex = ui.getSelectedIndex(filterList);
			if (selectedIndex == 0) {
				return messageDao.getMessageCount(messageType, messageHistoryStart, messageHistoryEnd);
			} else {
				Class<?> filterClass = getMessageHistoryFilterType();
				if(filterClass == Contact.class) {
					Contact c = ui.getContact(selectedItem);
					return messageDao.getMessageCountForMsisdn(messageType, c.getPhoneNumber(), messageHistoryStart, messageHistoryEnd);
				} else if(filterClass == Group.class) {
					// A Group was selected
					Group selectedGroup = ui.getGroup(selectedItem);
					return messageDao.getMessageCount(messageType, getPhoneNumbers(selectedGroup), messageHistoryStart, messageHistoryEnd);
				} else /* (filterClass == Keyword.class) */ {
					// Keyword Selected
					Keyword k = ui.getKeyword(selectedItem);
					return messageDao.getMessageCount(messageType, k, messageHistoryStart, messageHistoryEnd);
				}
			}
		}
	}
	
	/**
	 * Gets the list of messages to display in the message table.
	 * @param startIndex The index of the first message to return
	 * @param limit The maximum number of messages to return
	 * @return a page of messages, sorted and filtered
	 */
	private List<FrontlineMessage> getListMessages(int startIndex, int limit) {
		Object filterList = getMessageHistoryFilterList();
		Object selectedItem = ui.getSelectedItem(filterList);
		
		if (selectedItem == null) {
			return Collections.emptyList();
		} else {
			FrontlineMessage.Type messageType = getSelectedMessageType();
			Order order = getMessageSortOrder();
			Field sortField = getMessageSortField();
			
			int selectedIndex = ui.getSelectedIndex(filterList);
			if (selectedIndex == 0) {
				List<FrontlineMessage> allMessages = messageDao.getAllMessages(messageType, sortField, order, messageHistoryStart, messageHistoryEnd, startIndex, limit);
				return allMessages;
			} else {
				Class<?> filterClass = getMessageHistoryFilterType();
				if(filterClass == Contact.class) {
					// Contact selected
					Contact c = ui.getContact(selectedItem);
					return messageDao.getMessagesForMsisdn(messageType, c.getPhoneNumber(), sortField, order, messageHistoryStart, messageHistoryEnd, startIndex, limit);
				} else if(filterClass == Group.class) {
					// A Group was selected
					Group selectedGroup = ui.getGroup(selectedItem);
					return messageDao.getMessages(messageType, getPhoneNumbers(selectedGroup), sortField, order, messageHistoryStart, messageHistoryEnd, startIndex, limit);
				} else if (filterClass == Keyword.class) {
					// Keyword Selected
					Keyword k = ui.getKeyword(selectedItem);
					return messageDao.getMessagesForKeyword(messageType, k, sortField, order, messageHistoryStart, messageHistoryEnd, startIndex, limit);
				} else throw new IllegalStateException("Unknown filter class: " + filterClass.getName());
			}
		}
	}
	
	/** @return the field to sort messages in the message list by */
	private Field getMessageSortField() {
		Object header = Thinlet.get(messageListComponent, ThinletText.HEADER);
		Object tableColumn = ui.getSelectedItem(header);
		FrontlineMessage.Field field = FrontlineMessage.Field.DATE;
		if (tableColumn != null) {
			field = (FrontlineMessage.Field) ui.getProperty(tableColumn, PROPERTY_FIELD);
		}
		
		return field;
	}
	
	/** @return the sorting order for the message list */
	private Order getMessageSortOrder() {
		Object header = Thinlet.get(messageListComponent, ThinletText.HEADER);
		Object tableColumn = ui.getSelectedItem(header);
		Order order = Order.DESCENDING;
		if (tableColumn != null) {
			order = Thinlet.get(tableColumn, ThinletText.SORT).equals(ThinletText.ASCENT) ? Order.ASCENDING : Order.DESCENDING;
		}

		return order;
	}
	
	/** @return he type(s) of messages to display in the message list */
	private FrontlineMessage.Type getSelectedMessageType() {
		boolean showSentMessages = ui.isSelected(showSentMessagesComponent);
		boolean showReceivedMessages = ui.isSelected(showReceivedMessagesComponent);
		FrontlineMessage.Type messageType;
		if (showSentMessages && showReceivedMessages) { 
			messageType = Type.ALL;
		} else if (showSentMessages) {
			messageType = Type.OUTBOUND;
		} else messageType = Type.RECEIVED;
		return messageType;
	}
	
//> PUBLIC UI METHODS
	/** Method called when the selected filter is changed. */
	public void messageHistory_filterChanged() {
		resetMessageHistoryFilter();
	}
	
	/**
	 * Shows the export wizard dialog for exporting contacts.
	 * @param list The list to get selected items from.
	 */
	public void showExportWizard(Object list) {
		this.ui.showExportWizard(list, "messages");
	}

	/**
	 * Event triggered when the date has been changed for the message history.
	 * The messages should be re-filtered with the new dates.
	 */
	public void messageHistoryDateChanged() {
		Long newStart = null;
		String tfStartDateValue = ui.getText(find(COMPONENT_TF_START_DATE));
		String tfEndDateValue = ui.getText(find(COMPONENT_TF_END_DATE));
		try {
			newStart = InternationalisationUtils.parseDate(tfStartDateValue).getTime();
		} catch (ParseException ex) {}
		Long newEnd = null;
		try {
			newEnd = InternationalisationUtils.parseDate(tfEndDateValue).getTime() + MILLIS_PER_DAY;
		} catch (ParseException ex) {}
		
		// We refresh the list once if one of the date fields changed to either a valid or an empty date 
		if ((newStart != null && !newStart.equals(messageHistoryStart)) || (tfStartDateValue.equals("") && messageHistoryStart != null)) {
			messageHistoryStart = newStart;
			updateMessageList();
		}
		if ((newEnd != null && !newEnd.equals(messageHistoryEnd)) || (tfEndDateValue.equals("") && messageHistoryEnd != null)) {
			messageHistoryEnd = newEnd;
			updateMessageList();
		}
	}
	
	/** @deprecated this should be private */
	public void updateMessageHistoryCost() {
		LOG.trace("ENTRY");
		
		ui.setText(find(COMPONENT_LB_MSGS_NUMBER), String.valueOf(numberToSend));		
		ui.setText(find(COMPONENT_LB_COST), InternationalisationUtils.formatCurrency(UiProperties.getInstance().getCostPerSms() * numberToSend));
		
		LOG.trace("EXIT");
	}

	/**
	 * Method called when there is a change in the selection of Sent and Received messages.
	 * @param checkbox
	 */
	public void toggleMessageListOptions(Object checkbox) {
		boolean showSentMessages = ui.isSelected(showSentMessagesComponent);
		boolean showReceivedMessages = ui.isSelected(showReceivedMessagesComponent);

		// One needs to be on, so if both have just been switched off, we need to turn the other back on.
		if (!showSentMessages && !showReceivedMessages) {
			if(checkbox == showSentMessagesComponent) {
				ui.setSelected(showReceivedMessagesComponent, true);
			} else {
				ui.setSelected(showSentMessagesComponent, true);
			}
		}
		updateMessageList();
	}
	
	/** Update the list of messages. */
	public void updateMessageList() {
		Object filterList = getMessageHistoryFilterList();
		
		if (filterList != null) {
			// We save the selected item in the contacts/keywords list
			Class<?> filterClass = getMessageHistoryFilterType();
			boolean showContacts = filterClass == Contact.class;
			boolean showKeywords = filterClass == Keyword.class;
			if (showContacts)
				this.selectedContact = ui.getAttachedObject(ui.getSelectedItem(filterList), Contact.class);
			else if (showKeywords)
				this.selectedKeyword = ui.getAttachedObject(ui.getSelectedItem(filterList), Keyword.class);
		}
		
		this.messagePagingHandler.setCurrentPage(0);
		this.messagePagingHandler.refresh();
		updateMessageHistoryCost();
	}
	
	/** Reset the message history filter. */
	private void resetMessageHistoryFilter() {
		Class<?> filterClass = getMessageHistoryFilterType();
		Object filterList = getMessageHistoryFilterList();
		
		boolean showGroups = filterClass == Group.class;
		boolean showContacts = filterClass == Contact.class;
		boolean showKeywords = filterClass == Keyword.class;
		

		// We clear and reload the displayed list/tree
		if(showGroups) {
			groupSelecter.refresh(true);
		} else if(showContacts) {
			this.contactListPagingHandler.refresh();
		} else if(showKeywords) {
			this.keywordListPagingHandler.refresh();
		} else {
			throw new IllegalStateException();
		}
		
		ui.setVisible(find(COMPONENT_GROUP_PANEL), showGroups);
		ui.setVisible(find(COMPONENT_CONTACT_PANEL), showContacts);
		ui.setVisible(find(COMPONENT_KEYWORD_PANEL), showKeywords);
		
		// We select the right item in the displayed list, i.e. the one previously selected, if there was one
		if (filterList != null) {
			if (showGroups) {
				groupSelecter.selectGroup(selectedGroup);
			} else if (showContacts) {
				this.ui.setSelectedItem(filterList, getListItemForObject(filterList, selectedContact));
			} else if (showKeywords) {
				this.ui.setSelectedItem(filterList, getListItemForObject(filterList, selectedKeyword));
			}
			updateMessageList();
		}
	}
	
	/**
	 * Gets the list item we are currently displaying for an object (Contact or Keyword so far).
	 * @param filterList The list in which we're looking for the selected item
	 * @param selected The selected object which should match an item in the list
	 * @return 
	 */
	private Object getListItemForObject(Object filterList, Object selected) {
		if (selected == null) return null;
		
		Object ret = null;
		for (Object o : this.ui.getItems(filterList)) {
			Object k = ui.getAttachedObject(o);
			if (k != null && k.equals(selected)) {
				ret = o;
				break;
			}
		}
		return ret;
	}

	public void lsContacts_enableSend(Object popUp) {
		boolean visible = ui.getSelectedIndex(contactListComponent) > 0;
		ui.setVisible(popUp, visible);
	}

	/**
	 * Event triggered when an outgoing message is created or updated.
	 * @param message The message involved in the event
	 */
	public synchronized void outgoingMessageEvent(final FrontlineMessage message) {
		LOG.debug("Refreshing message list");
		
		
		
		// If the message is already in the list, we just need to update its row
		for (int i = 0; i < ui.getItems(messageListComponent).length; i++) {
			FrontlineMessage e = ui.getMessage(ui.getItem(messageListComponent, i));
			if (e.equals(message)) {
				final int index = i;
				FrontlineUiUpateJob updateJob = new FrontlineUiUpateJob() {
					
					public void run() {
						ui.remove(ui.getItem(messageListComponent, index));
						ui.add(messageListComponent, ui.getRow(message), index);
					}
				};
				
				EventQueue.invokeLater(updateJob);
				return;
			}
		}
		
		// If the message is not already in the list, add it if relevant
		addMessageToList(message);
	}
	
	/**
	 * Event triggered when an incoming message arrives.
	 * @param message The message involved in the event
	 */
	public synchronized void incomingMessageEvent(FrontlineMessage message) {
		addMessageToList(message);
	}
	
	public void messagesTab_removeMessages() {
		LOG.trace("ENTER");
		
		ui.removeConfirmationDialog();
		ui.setStatus(InternationalisationUtils.getI18NString(MESSAGE_REMOVING_MESSAGES));

		final Object[] selected = ui.getSelectedItems(messageListComponent);
		int numberRemoved = 0;
		for(Object o : selected) {
			FrontlineMessage toBeRemoved = ui.getMessage(o);
			LOG.debug("Message [" + toBeRemoved + "]");
			FrontlineMessage.Status status = toBeRemoved.getStatus();
			if (status != Status.PENDING) {
				LOG.debug("Removing Message [" + toBeRemoved + "] from database.");
				if (status == Status.OUTBOX) {
					// FIXME should not be getting the phone manager like this - should be a local propery i rather think
					ui.getPhoneManager().removeFromOutbox(toBeRemoved);
				}
				numberToSend -= toBeRemoved.getNumberOfSMS();
				messageDao.deleteMessage(toBeRemoved);
				numberRemoved++;
			} else {
				LOG.debug("Message status is [" + toBeRemoved.getStatus() + "], so we do not remove!");
			}
		}
		if (numberRemoved > 0) {
			ui.setStatus(InternationalisationUtils.getI18NString(MESSAGE_MESSAGES_DELETED));
			updateMessageList();
		}
		
		LOG.trace("EXIT");
	}

	/** Show the details of the message selected in {@link #messageListComponent}. */
	public void showMessageDetails() {
		Object selected = ui.getSelectedItem(this.messageListComponent);
		if (selected != null) {
			FrontlineMessage message = ui.getMessage(selected);
			new MessageDetailsDisplay(ui).show(message);
		}
	}
	
	/**
	 * Re-Sends the selected messages and updates the list with the supplied page number afterwards.
	 * 
	 * @param pageNumber
	 * @param resultsPerPage
	 * @param object
	 */
	public void resendSelectedFromMessageList(Object object) {
		Object[] selected = ui.getSelectedItems(object);
		for (Object o : selected) {
			FrontlineMessage toBeReSent = ui.getMessage(o);
			FrontlineMessage.Status status = toBeReSent.getStatus();
			if (status == Status.FAILED) {
				toBeReSent.setSenderMsisdn("");
				toBeReSent.setRetriesRemaining(FrontlineMessage.MAX_RETRIES);
				ui.getPhoneManager().sendSMS(toBeReSent);
			} else if (status == Status.DELIVERED || status == Status.SENT) {
				if(toBeReSent.isBinaryMessage()) {
					FrontlineMessage newMessage = FrontlineMessage.createBinaryOutgoingMessage(System.currentTimeMillis(), "",
							toBeReSent.getRecipientMsisdn(), toBeReSent.getRecipientSmsPort(), toBeReSent.getBinaryContent());
					ui.getFrontlineController().sendMessage(newMessage);
				} else {
					ui.getFrontlineController().sendTextMessage(toBeReSent.getRecipientMsisdn(), toBeReSent.getTextContent());
				}
			}
		}
	}
	
	/**
	 * Enables or disables menu options in a List Component's popup list
	 * and toolbar.  These enablements are based on whether any items in
	 * the list are selected, and if they are, on the nature of these
	 * items.
	 * @param list the list
	 * @param popupMenu the popup menu the list refers to
	 */
	public void enableOptions(Object list, Object popupMenu) {
		Object[] selectedItems = ui.getSelectedItems(list);
		boolean hasSelection = selectedItems.length > 0;
		
		// If nothing is selected, hide the popup menu
		ui.setVisible(popupMenu, hasSelection);
		
		if (hasSelection) {
			// If we are looking at a list of messages, there are certain popup menu items that
			// should or shouldn't be enabled, depending on the type of messages we have selected.
			boolean receivedMessagesSelected = false;
			boolean sentMessagesSelected = false;
			for(Object selectedComponent : selectedItems) {
				FrontlineMessage attachedMessage = ui.getAttachedObject(selectedComponent, FrontlineMessage.class);
				if(attachedMessage.getType() == Type.RECEIVED) {
					receivedMessagesSelected = true;
				}
				if(attachedMessage.getType() == Type.OUTBOUND) {
					sentMessagesSelected = true;
				}
			}
			
			for (Object popupMenuItem : ui.getItems(popupMenu)) {
				String popupMenuItemName = ui.getName(popupMenuItem);
				boolean visible = hasSelection;
				if(popupMenuItemName.equals("miReply")) {
					visible = receivedMessagesSelected;
				}
				if(popupMenuItemName.equals("miResend")) {
					visible = sentMessagesSelected;
				}
				ui.setVisible(popupMenuItem, visible);
			}
		}
	}

//> UI HELPER METHODS
	/** Initialise the message table's HEADER component for sorting the table. */
	private void initMessageTableForSorting() {
		Object header = Thinlet.get(messageListComponent, ThinletText.HEADER);
		for (Object o : ui.getItems(header)) {
			String text = ui.getString(o, Thinlet.TEXT);
			// Here, the FIELD property is set on each column of the message table.  These field objects are
			// then used for easy sorting of the message table.
			if(text != null) {
				if (text.equalsIgnoreCase(InternationalisationUtils.getI18NString(COMMON_STATUS))) ui.putProperty(o, PROPERTY_FIELD, FrontlineMessage.Field.STATUS);
				else if(text.equalsIgnoreCase(InternationalisationUtils.getI18NString(COMMON_DATE))) ui.putProperty(o, PROPERTY_FIELD, FrontlineMessage.Field.DATE);
				else if(text.equalsIgnoreCase(InternationalisationUtils.getI18NString(COMMON_SENDER))) ui.putProperty(o, PROPERTY_FIELD, FrontlineMessage.Field.SENDER_MSISDN);
				else if(text.equalsIgnoreCase(InternationalisationUtils.getI18NString(COMMON_RECIPIENT))) ui.putProperty(o, PROPERTY_FIELD, FrontlineMessage.Field.RECIPIENT_MSISDN);
				else if(text.equalsIgnoreCase(InternationalisationUtils.getI18NString(COMMON_MESSAGE))) ui.putProperty(o, PROPERTY_FIELD, FrontlineMessage.Field.MESSAGE_CONTENT);
			}
		}
	}

	/**
	 * Adds a message to the list we are currently viewing, if it is relevant.
	 * We just add the message to the top of the list, as things will get rather complicated otherwise
	 * @param message the message to add
	 */
	private void addMessageToList(final FrontlineMessage message) {
		LOG.trace("ENTER");
		LOG.debug("Message [" + message + "]");
		Object sel = ui.getSelectedItem(contactListComponent); // TODO doesn't seem to do anything for keyword list
		boolean sent = ui.isSelected(showSentMessagesComponent);
		boolean received = ui.isSelected(showReceivedMessagesComponent);
		if (sel != null && ((sent && message.getType() == Type.OUTBOUND) || (received && message.getType() == Type.RECEIVED))) {
			boolean toAdd = false;
			if (ui.getSelectedIndex(contactListComponent) == 0) {
				toAdd = true;
			} else {
				if (ui.isSelected(find(COMPONENT_CB_CONTACTS))) {
					Contact c = ui.getContact(sel);
					LOG.debug("Contact selected [" + c.getName() + "]");
					if (message.getSenderMsisdn().endsWith(c.getPhoneNumber()) 
							|| message.getRecipientMsisdn().endsWith(c.getPhoneNumber())) {
						toAdd = true;
					}
				} else if (ui.isSelected(find(COMPONENT_CB_GROUPS))) {
					Group g = ui.getGroup(sel);
					LOG.debug("Group selected [" + g.getName() + "]");
					if (g.equals(ui.getRootGroup())) {
						toAdd = true;
					} else {
						List<Group> groups = new ArrayList<Group>();
						ui.getGroupsRecursivelyUp(groups, g);
						Contact sender = contactDao.getFromMsisdn(message.getSenderMsisdn());
						Contact receiver = contactDao.getFromMsisdn(message.getRecipientMsisdn());
						for (Group gg : groups) {
							if((sender != null && groupMembershipDao.isMember(gg, sender))
									|| (receiver != null && groupMembershipDao.isMember(gg, receiver))) {
								toAdd = true;
								break;
							}
						}
					}
				} else {
					Keyword selected = ui.getKeyword(sel);
					LOG.debug("Keyword selected [" + selected.getKeyword() + "]");
					Keyword keyword = keywordDao.getFromMessageText(message.getTextContent());
					toAdd = selected.equals(keyword);
				}
			}
			if (toAdd) {
				LOG.debug("Time to try to add this message to list...");
				FrontlineUiUpateJob updateJob = new FrontlineUiUpateJob() {
					
					public void run() {
						addMessage(message);
					}
				};
				
				EventQueue.invokeLater(updateJob);
				
				
			}
		}
		LOG.trace("EXIT");
	}
	
	private void addMessage(FrontlineMessage message) {
		// if this message is already in the list, perhaps with a different status, we should just
		// update the current row rather than adding a new one
		Object[] messageItems = ui.getItems(messageListComponent);
		boolean replaced = false;
		for (int rowIndex = 0; rowIndex < messageItems.length; rowIndex++) {
			Object currentRowComponent = messageItems[rowIndex];
			if(message.equals(ui.getAttachedObject(currentRowComponent))) {
				// replace or update the row in the message table
				ui.add(messageListComponent, ui.getRow(message), rowIndex);
				ui.remove(currentRowComponent);
				replaced = true;
				break;
			}
		}
		
		if(!replaced) {
			if (ui.getItems(messageListComponent).length < this.messagePagingHandler.getMaxItemsPerPage()) {
				LOG.debug("There's space! Adding...");
				ui.add(messageListComponent, ui.getRow(message));
				ui.setEnabled(messageListComponent, true);
				if (message.getType() == Type.OUTBOUND) {
					numberToSend += message.getNumberOfSMS();
					updateMessageHistoryCost();
				}
			}
		}
	}

	/**
	 * Gets the selected filter type for the message history, i.e. Contact, Group or Keyword.
	 * @return {@link Contact}, {@link Group} or {@link Keyword}, depending which is set for the message filter.
	 */
	private Class<?> getMessageHistoryFilterType() {
		if(ui.isSelected(find(COMPONENT_CB_CONTACTS))) return Contact.class;
		else if(ui.isSelected(find(COMPONENT_CB_GROUPS))) return Group.class;
		else return Keyword.class;
	}
	
	/**
	 * Gets the list component relating to the class returned by {@link #getMessageHistoryFilterType()}
	 * @return
	 */
	private Object getMessageHistoryFilterList() {
		Class<?> filterClass = getMessageHistoryFilterType();
		return filterClass == Group.class ? groupTreeComponent
				  : filterClass == Contact.class ? contactListComponent
						  						: keywordListComponent;
	}
	
	/** @return list item component representing ALL MESSAGES in the system */
	private Object getAllMessagesListItem() {
		Object allMessages = ui.createListItem(InternationalisationUtils.getI18NString(FrontlineSMSConstants.COMMON_ALL_MESSAGES), null);
		ui.setIcon(allMessages, Icon.SMS_HISTORY);
		return allMessages;
	}
	
//> UI PASS-THROUGH METHODS
	/** @see UiGeneratorController#show_composeMessageForm(Object) */
	public void show_composeMessageForm(Object list) {
		this.ui.show_composeMessageForm(list);
	}
	/** @see UiGeneratorController#showDateSelecter(Object) */
	public void showDateSelecter(Object textField) {
		this.ui.showDateSelecter(textField);
	}
	
//> HELPER METHODS
	public List<String> getPhoneNumbers(Group group) {
		return getPhoneNumbers(this.groupMembershipDao.getMembers(group));
	}
	
	public List<String> getPhoneNumbers(List<Contact> contacts) {
		ArrayList<String> phoneNumbers = new ArrayList<String>(contacts.size());
		for(Contact c : contacts) {
			phoneNumbers.add(c.getPhoneNumber());
		}
		return phoneNumbers;
	}
}
