/**
 * 
 */
package net.frontlinesms.ui.handler.email;

import static net.frontlinesms.FrontlineSMSConstants.COMMON_CONTENT;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_DATE;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_RECIPIENT;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_SENDER;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_STATUS;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_SUBJECT;
import static net.frontlinesms.FrontlineSMSConstants.DEFAULT_END_DATE;
import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_EMAILS_LOADED;
import static net.frontlinesms.FrontlineSMSConstants.PROPERTY_FIELD;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.TAB_EMAIL_LOG;

import java.util.List;

import thinlet.Thinlet;
import thinlet.ThinletText;
import net.frontlinesms.EmailSender;
import net.frontlinesms.EmailServerHandler;
import net.frontlinesms.FrontlineSMS;
import net.frontlinesms.data.Order;
import net.frontlinesms.data.domain.Email;
import net.frontlinesms.data.domain.EmailAccount;
import net.frontlinesms.data.domain.Email.Field;
import net.frontlinesms.data.repository.EmailDao;
import net.frontlinesms.events.FrontlineEventNotification;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.events.TabChangedNotification;
import net.frontlinesms.ui.handler.BaseTabHandler;
import net.frontlinesms.ui.handler.ComponentPagingHandler;
import net.frontlinesms.ui.handler.PagedComponentItemProvider;
import net.frontlinesms.ui.handler.PagedListDetails;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.i18n.TextResourceKeyOwner;

/**
 * @author Alex Anderson 
 * @author Carlos Eduardo Genz
 */
@TextResourceKeyOwner(prefix="MESSAGE_")
public class EmailTabHandler extends BaseTabHandler implements PagedComponentItemProvider {
//> UI LAYOUT FILES
	/** Thinlet XML Layout file for the email tab */
	public static final String UI_FILE_EMAILS_TAB = "/ui/core/email/emailsTab.xml";
	
//> UI COMPONENT NAMES
	public static final String COMPONENT_EMAIL_LIST = "emailList";
	public static final String COMPONENT_PN_EMAIL = "pnEmail";
	public static final String COMPONENT_EMAILS_TOOLBAR = "emails_toolbar";

	public static final String MESSAGE_EMAILS_DELETED = "message.emails.deleted";
	public static final String MESSAGE_REMOVING_EMAILS = "message.removing.emails";
	
//> INSTANCE PROPERTIES
	/** Manager of {@link EmailAccount}s and {@link EmailSender}s */
	private EmailServerHandler emailManager;
	/** DAO for {@link Email}s */
	private EmailDao emailDao;
	/** DAO for {@link EmailAccount}s */
	
	/** Thinlet UI Component: list of emails */
	private Object emailListComponent;
	/** Paging handler for {@link #emailListComponent} */
	private ComponentPagingHandler emailListPager;
	
//> CONSTRUCTORS
	/** 
	 * Create a new {@link EmailTabHandler} tied to the specified UI and frontline controller. 
	 * @param ui 
	 * @param frontlineController
	 */
	public EmailTabHandler(UiGeneratorController ui) {
		super(ui, true);
		FrontlineSMS frontlineController = ui.getFrontlineController();
		this.emailManager = frontlineController .getEmailServerHandler();
		this.emailDao = frontlineController.getEmailDao();
	}
	
//> ACCESSORS
	/** Refresh the view */
	public void refresh() {
		this.emailListPager.refresh();
	}
	
	/** @see BaseTabHandler#init() */
	protected Object initialiseTab() {
		Object tabComponent = ui.loadComponentFromFile(UI_FILE_EMAILS_TAB, this);
		
		emailListComponent = ui.find(tabComponent, COMPONENT_EMAIL_LIST);
		this.emailListPager = new ComponentPagingHandler(this.ui, this, this.emailListComponent);
		Object pnEmail = ui.find(tabComponent, COMPONENT_PN_EMAIL);
		ui.add(pnEmail, this.emailListPager.getPanel(), 2);
		
		// Set the types for the email list columns...
		initEmailTableForSorting();
	
		return tabComponent;
	}
	
//> PAGING METHODS
	/** @see PagedComponentItemProvider#getListDetails(Object, int, int) */
	public PagedListDetails getListDetails(Object list, int startIndex, int limit) {
		if(list == this.emailListComponent) {
			return getEmailListDetails(startIndex, limit);
		} else throw new IllegalStateException();
	}
	
	/** @return {@link PagedListDetails} for the {@link #emailListComponent} */
	private PagedListDetails getEmailListDetails(int startIndex, int limit) {
		int totalItemCount = this.emailDao.getEmailCount();

		List<Email> emails = this.emailDao.getEmailsWithLimit(getSortField(), getSortOrder(), startIndex, limit);
		Object[] components = new Object[emails.size()];
		for (int i=0; i<components.length; ++i) {
			Email email = emails.get(i);
			components[i] = getRow(email);
		}
		
		return new PagedListDetails(totalItemCount, components);
	}

	/** @return the order to sort emails in {@link #emailListComponent} */
	private Order getSortOrder() {
		Object header = Thinlet.get(emailListComponent, ThinletText.HEADER);
		Object tableColumn = ui.getSelectedItem(header);
		Order order = Order.DESCENDING;
		if (tableColumn != null) {
			order = Thinlet.get(tableColumn, ThinletText.SORT).equals(ThinletText.ASCENT) ? Order.ASCENDING : Order.DESCENDING;
		}
		return order;
	}
	/** @return the column to sort emails in {@link #emailListComponent} by */
	private Field getSortField() {
		Object header = Thinlet.get(emailListComponent, ThinletText.HEADER);
		Object tableColumn = ui.getSelectedItem(header);
		Email.Field field = null;
		if (tableColumn != null) {
			field = (Email.Field) ui.getProperty(tableColumn, PROPERTY_FIELD);
		}
		return field;
	}
	
//>

	/** Set up the email table's columns to allow easy sorting */
	private void initEmailTableForSorting() {
		Object header = Thinlet.get(emailListComponent, ThinletText.HEADER);
		for (Object o : ui.getItems(header)) {
			String text = ui.getString(o, Thinlet.TEXT);
			// Here, the FIELD property is set on each column of the message table.  These field objects are
			// then used for easy sorting of the message table.
			if (text != null) {
				if (text.equalsIgnoreCase(InternationalisationUtils.getI18nString(COMMON_STATUS))) ui.putProperty(o, PROPERTY_FIELD, Email.Field.STATUS);
				else if(text.equalsIgnoreCase(InternationalisationUtils.getI18nString(COMMON_DATE))) ui.putProperty(o, PROPERTY_FIELD, Email.Field.DATE);
				else if(text.equalsIgnoreCase(InternationalisationUtils.getI18nString(COMMON_SENDER))) ui.putProperty(o, PROPERTY_FIELD, Email.Field.FROM);
				else if(text.equalsIgnoreCase(InternationalisationUtils.getI18nString(COMMON_RECIPIENT))) ui.putProperty(o, PROPERTY_FIELD, Email.Field.TO);
				else if(text.equalsIgnoreCase(InternationalisationUtils.getI18nString(COMMON_CONTENT))) ui.putProperty(o, PROPERTY_FIELD, Email.Field.EMAIL_CONTENT);
				else if(text.equalsIgnoreCase(InternationalisationUtils.getI18nString(COMMON_SUBJECT))) ui.putProperty(o, PROPERTY_FIELD, Email.Field.SUBJECT);
			}
		}
	}
	
//> UI EVENT HANDLING METHODS
	/**
	 * Removes the selected emails
	 */
	public void removeSelectedFromEmailList() {
		log.trace("ENTER");
		ui.removeConfirmationDialog();
		ui.setStatus(InternationalisationUtils.getI18nString(MESSAGE_REMOVING_EMAILS));
		final Object[] selected = ui.getSelectedItems(emailListComponent);
		int numberRemoved = 0;
		for (Object o : selected) {
			Email toBeRemoved = ui.getEmail(o);
			log.debug("E-mail [" + toBeRemoved + "]");
			Email.Status status = toBeRemoved.getStatus();
			if (status != Email.Status.PENDING 
					&& status != Email.Status.RETRYING) {
				log.debug("Removing from database..");
				if (status == Email.Status.OUTBOX) {
					emailManager.removeFromOutbox(toBeRemoved);
				}
				emailDao.deleteEmail(toBeRemoved);
				numberRemoved++;
			} else {
				log.debug("E-mail status is [" + toBeRemoved.getStatus() + "]. Do not remove...");
			}
		}
		if (numberRemoved > 0) {
			updateEmailList();
		}
		ui.setStatus(InternationalisationUtils.getI18nString(MESSAGE_EMAILS_DELETED));
		log.trace("EXIT");
	}
	
	/** Update the email list inside the email log tab. */
	public void updateEmailList() {
		this.emailListPager.refresh();
		enableOptions(emailListComponent, null, find(COMPONENT_EMAILS_TOOLBAR));
	}
	
	/** Re-Sends the selected emails */
	public void resendSelectedFromEmailList() {
		Object[] selected = ui.getSelectedItems(emailListComponent);
		for (Object o : selected) {
			Email toBeReSent = ui.getEmail(o);
			Email.Status status = toBeReSent.getStatus();
			if (status == Email.Status.FAILED) {
				emailManager.sendEmail(toBeReSent);
			} else if (status == Email.Status.SENT ) {
				Email newEmail = new Email(toBeReSent.getEmailFrom(), toBeReSent.getEmailRecipients(), toBeReSent.getEmailSubject(), toBeReSent.getEmailContent());
				emailDao.saveEmail(newEmail);
				emailManager.sendEmail(newEmail);
			}
		}
	}
	
	/**
	 * Enables or disables menu options in a List Component's popup list
	 * and toolbar.  These enablements are based on whether any items in
	 * the list are selected, and if they are, on the nature of these
	 * items.
	 * @param list 
	 * @param popup 
	 * @param toolbar
	 */
	public void enableOptions(Object list, Object popup, Object toolbar) {
		Object[] selectedItems = ui.getSelectedItems(list);
		boolean hasSelection = selectedItems.length > 0;

		if(popup!= null && !hasSelection) {
			ui.setVisible(popup, false);
			return;
		}
		
		if (hasSelection && popup != null) {
			// If nothing is selected, hide the popup menu
			ui.setVisible(popup, true);
		}
		
		if (toolbar != null && !toolbar.equals(popup)) {
			for (Object o : ui.getItems(toolbar)) {
				ui.setEnabled(o, hasSelection);
			}
		}
	}
	
//> UI HELPER METHODS
	/**
	 * Creates a Thinlet UI table row containing details of an Email.
	 * @param email
	 * @return
	 */
	private Object getRow(Email email) {
		Object row = ui.createTableRow(email);

		ui.add(row, ui.createTableCell(InternationalisationUtils.getI18nString(email.getStatus())));
		if (email.getDate() == DEFAULT_END_DATE) {
			ui.add(row, ui.createTableCell(""));
		} else {
			ui.add(row, ui.createTableCell(InternationalisationUtils.getDatetimeFormat().format(email.getDate())));
		}
		ui.add(row, ui.createTableCell(email.getEmailFrom().getAccountName()));
		ui.add(row, ui.createTableCell(email.getEmailRecipients()));
		ui.add(row, ui.createTableCell(email.getEmailSubject()));
		ui.add(row, ui.createTableCell(email.getEmailContent()));

		return row;
	}
	
//> LISTENER EVENT METHODS
	public synchronized void outgoingEmailEvent(EmailSender sender, Email email) {
		log.debug("Refreshing e-mail list");
		int index = -1;
		for (int i = 0; i < ui.getItems(emailListComponent).length; i++) {
			Email e = ui.getEmail(ui.getItem(emailListComponent, i));
			if (e.equals(email)) {
				index = i;
				break;
			}
		}
		if (index != -1) {
			//Updating
			ui.remove(ui.getItem(emailListComponent, index));
			ui.add(emailListComponent, getRow(email), index);
		} else {
			int limit = this.emailListPager.getMaxItemsPerPage();
			//Adding
			if (ui.getItems(emailListComponent).length < limit && email.getStatus() == Email.Status.OUTBOX) {
				ui.add(emailListComponent, getRow(email));
			}
		}
	}
	
	/**
	 * UI event called when the user changes tab
	 */
	public void notify(FrontlineEventNotification notification) {
		super.notify(notification);
		// This object is registered to the UIGeneratorController and get notified when the users changes tab
		if(notification instanceof TabChangedNotification) {
			String newTabName = ((TabChangedNotification) notification).getNewTabName();
			if (newTabName.equals(TAB_EMAIL_LOG)) {
				threadSafeRefresh();
				this.ui.setStatus(InternationalisationUtils.getI18nString(MESSAGE_EMAILS_LOADED));
			}
		}
	}
}
