/**
 * 
 */
package net.frontlinesms.ui.handler.email;

import java.util.Collection;

import org.apache.log4j.Logger;

import thinlet.Thinlet;

import net.frontlinesms.EmailSender;
import net.frontlinesms.EmailServerHandler;
import net.frontlinesms.FrontlineSMS;
import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.domain.EmailAccount;
import net.frontlinesms.data.events.DatabaseEntityNotification;
import net.frontlinesms.data.repository.EmailAccountDao;
import net.frontlinesms.events.EventBus;
import net.frontlinesms.events.EventObserver;
import net.frontlinesms.events.FrontlineEventNotification;
import net.frontlinesms.ui.Icon;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiDestroyEvent;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.events.FrontlineUiUpdateJob;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.i18n.TextResourceKeyOwner;

/**
 * @author aga
 *
 */
@TextResourceKeyOwner
public class EmailAccountDialogHandler implements ThinletUiEventHandler, EventObserver {
//> UI LAYOUT FILES
	private static final String UI_FILE_EMAIL_ACCOUNTS_SETTINGS_FORM = "/ui/core/email/dgServerConfig.xml";
	private static final String UI_FILE_EMAIL_ACCOUNTS_LIST_FORM = "/ui/core/email/pnAccountsList.xml";
	
	//> THINLET COMPONENT NAMES
	private static final String UI_COMPONENT_ACCOUNTS_LIST = "accountsList";
	private static final String UI_COMPONENT_BT_EDIT = "btEditAccount";
	private static final String UI_COMPONENT_BT_DELETE = "btDeleteAccount";
	
	//> I18N TEXT KEYS
	private static final String I18N_COMMON_EMAIL_ACCOUNT_SETTINGS = "common.email.account.settings";
	private static final String I18N_MMS_EMAIL_ACCOUNT_SETTINGS = "mms.email.account.settings";


//> INSTANCE PROPERTIES
	/** Logger */
	private Logger LOG = FrontlineUtils.getLogger(this.getClass());
	
	private UiGeneratorController ui;
	private EmailAccountDao emailAccountDao;
	/** Manager of {@link EmailAccount}s and {@link EmailSender}s */
	private EmailServerHandler emailManager;
	
	private Object dialogComponent;
	
	private boolean isForReceiving;
	
	public EmailAccountDialogHandler(UiGeneratorController ui, boolean isForReceiving) {
		this.ui = ui;
		FrontlineSMS frontlineController = ui.getFrontlineController();
		this.emailAccountDao = frontlineController.getEmailAccountFactory();
		this.emailManager = frontlineController.getEmailServerHandler();
		this.isForReceiving = isForReceiving;
		
		// Register with the EventBus to receive notification of new email accounts
		ui.getFrontlineController().getEventBus().registerObserver(this);
	}
	
	public Object getDialog() {
		initDialog();
		return this.getDialogComponent();
	}
	
	private void initDialog() {
		this.setDialogComponent(ui.loadComponentFromFile(UI_FILE_EMAIL_ACCOUNTS_SETTINGS_FORM, this));
		Object pnAccountsList = ui.loadComponentFromFile(UI_FILE_EMAIL_ACCOUNTS_LIST_FORM, this);
		this.ui.add(this.getDialogComponent(), pnAccountsList, 0);
		
		if (this.isForReceiving) {
			this.ui.setText(getDialogComponent(), InternationalisationUtils.getI18nString(I18N_MMS_EMAIL_ACCOUNT_SETTINGS));
		} else {
			this.ui.setText(getDialogComponent(), InternationalisationUtils.getI18nString(I18N_COMMON_EMAIL_ACCOUNT_SETTINGS));
		}
		this.refresh();
	}
	
	private void threadSafeRefresh() {
		new FrontlineUiUpdateJob() {
			public void run() {
				refresh();
			}
		}.execute();
	}

	public void refresh() {
		Object table = find(UI_COMPONENT_ACCOUNTS_LIST);
		this.ui.removeAll(table);
		Collection<EmailAccount> emailAccounts;
		if (this.isForReceiving) {
			emailAccounts = emailAccountDao.getReceivingEmailAccounts();
		} else {
			emailAccounts = emailAccountDao.getSendingEmailAccounts();
		}
		for (EmailAccount acc : emailAccounts) {
			this.ui.add(table, ui.getRow(acc));
		}
		
		this.enableBottomButtons(table);
	}

//> UI EVENT METHODS
		
	public void finishEmailManagement(Object dialog) {
		Object att = ui.getAttachedObject(dialog);
		if (att != null) {
			Object list = Thinlet.find(att, UI_COMPONENT_ACCOUNTS_LIST);
			ui.removeAll(list);
			for (EmailAccount acc : emailAccountDao.getAllEmailAccounts()) {
				Object item = ui.createListItem(acc.getAccountName(), acc);
				ui.setIcon(item, Icon.SERVER);
				ui.add(list, item);
			}
		}
		ui.removeDialog(dialog);
	}
	
	public void newEmailAccountSettings () {
		showEmailAccountSettingsDialog(null);
	}
	
	public void editEmailAccountSettings(Object list) {
		Object selected = ui.getSelectedItem(list);
		if (selected != null) {
			EmailAccount emailAccount = (EmailAccount) ui.getAttachedObject(selected);
			showEmailAccountSettingsDialog(emailAccount);
		}
	}
	
	private void showEmailAccountSettingsDialog(EmailAccount emailAccount) {
		EmailAccountSettingsDialogHandler emailAccountSettingsDialogHandler = new EmailAccountSettingsDialogHandler(ui, this.isForReceiving);
		emailAccountSettingsDialogHandler.initDialog(emailAccount);
		this.ui.add(emailAccountSettingsDialogHandler.getDialog());
	}
	
	public void enableBottomButtons(Object table) {
		boolean enableEditAndDelete = (ui.getSelectedIndex(table) >= 0);
		
		this.ui.setEnabled(find(UI_COMPONENT_BT_EDIT), enableEditAndDelete);
		this.ui.setEnabled(find(UI_COMPONENT_BT_DELETE), enableEditAndDelete);
	}
	
	/**
	 * Enables or disables menu options in a List Component's popup list
	 * and toolbar.  These enablements are based on whether any items in
	 * the list are selected, and if they are, on the nature of these
	 * items.
	 * @param list 
	 * @param popup 
	 * @param toolbar
	 * 
	 * TODO check where this is used, and make sure there is no dead code
	 */
	public void enableOptions(Object list, Object popup, Object toolbar) {
		Object[] selectedItems = ui.getSelectedItems(list);
		boolean hasSelection = selectedItems.length > 0;

		if(popup!= null && !hasSelection && "emailServerListPopup".equals(ui.getName(popup))) {
			ui.setVisible(popup, false);
			return;
		}
		
		if (hasSelection && popup != null) {
			// If nothing is selected, hide the popup menu
			ui.setVisible(popup, hasSelection);
		}
		
		if (toolbar != null && !toolbar.equals(popup)) {
			for (Object o : ui.getItems(toolbar)) {
				ui.setEnabled(o, hasSelection);
			}
		}
	}
	
	/**
	 * Removes the selected accounts.
	 */
	public void removeSelectedFromAccountList() {
		LOG.trace("ENTER");
		ui.removeConfirmationDialog();
		Object list = find(UI_COMPONENT_ACCOUNTS_LIST);
		Object[] selected = ui.getSelectedItems(list);
		for (Object o : selected) {
			EmailAccount acc = ui.getAttachedObject(o, EmailAccount.class);
			LOG.debug("Removing Account [" + acc.getAccountName() + "]");
			emailManager.serverRemoved(acc);
			emailAccountDao.deleteEmailAccount(acc);
		}
		
		this.refresh();
		LOG.trace("EXIT");
	}
	
	/** Handle notifications from the {@link EventBus} */
	public void notify(FrontlineEventNotification event) {
		if(event instanceof DatabaseEntityNotification<?>) {
			if(((DatabaseEntityNotification<?>)event).getDatabaseEntity() instanceof EmailAccount) {
				this.threadSafeRefresh();
			}
		} else if (event instanceof UiDestroyEvent) {
			if(((UiDestroyEvent) event).isFor(this.ui)) {
				this.ui.getFrontlineController().getEventBus().unregisterObserver(this);
			}
		}
	}


//> UI PASSTHROUGH METHODS
	/** @see UiGeneratorController#showConfirmationDialog(String, Object) */
	public void showConfirmationDialog(String methodToBeCalled) {
		this.ui.showConfirmationDialog(methodToBeCalled, this);
	}
	/**
	 * @param page page to show
	 * @see UiGeneratorController#showHelpPage(String)
	 */
	public void showHelpPage(String page) {
		this.ui.showHelpPage(page);
	}
	/** @see UiGeneratorController#removeDialog(Object) */
	public void removeDialog(Object dialog) {
		this.ui.removeDialog(dialog);
	}
	
//> UI HELPER METHODS
	/**
	 * Find a UI component within the {@link #dialogComponent}.
	 * @param componentName the name of the UI component
	 * @return the ui component, or <code>null</code> if it could not be found
	 */
	private Object find(String componentName) {
		return Thinlet.find(this.getDialogComponent(), componentName);
	}

	public void setDialogComponent(Object dialogComponent) {
		this.dialogComponent = dialogComponent;
	}

	public Object getDialogComponent() {
		return dialogComponent;
	}
}
