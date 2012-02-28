package net.frontlinesms.ui.handler.settings;

import java.util.Collection;

import net.frontlinesms.EmailSender;
import net.frontlinesms.EmailServerHandler;
import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.domain.EmailAccount;
import net.frontlinesms.data.events.DatabaseEntityNotification;
import net.frontlinesms.data.repository.EmailAccountDao;
import net.frontlinesms.events.EventBus;
import net.frontlinesms.events.EventObserver;
import net.frontlinesms.events.FrontlineEventNotification;
import net.frontlinesms.settings.BaseSectionHandler;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiDestroyEvent;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.events.FrontlineUiUpdateJob;
import net.frontlinesms.ui.handler.email.EmailAccountSettingsDialogHandler;
import net.frontlinesms.ui.settings.UiSettingsSectionHandler;

import org.apache.log4j.Logger;

import thinlet.Thinlet;

/**
 * UI Handler for the sections incorporating a list of email accounts
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public abstract class SettingsAbstractEmailsSectionHandler extends BaseSectionHandler implements UiSettingsSectionHandler, ThinletUiEventHandler, EventObserver {
	//> UI LAYOUT FILES
	protected static final String UI_FILE_LIST_EMAIL_ACCOUNTS_PANEL = "/ui/core/settings/generic/pnAccountsList.xml";
	
	//> THINLET COMPONENT NAMES
	protected static final String UI_COMPONENT_ACCOUNTS_LIST = "accountsList";
	protected static final String UI_COMPONENT_BT_EDIT = "btEditAccount";
	protected static final String UI_COMPONENT_BT_DELETE = "btDeleteAccount";
	
//> INSTANCE PROPERTIES
	/** Logger */
	protected Logger LOG = FrontlineUtils.getLogger(this.getClass());
	
	protected EmailAccountDao emailAccountDao;
	/** Manager of {@link EmailAccount}s and {@link EmailSender}s */
	protected EmailServerHandler emailManager;
	
	protected boolean isForReceiving;

	private Object accountsListPanel;
	
	public SettingsAbstractEmailsSectionHandler (UiGeneratorController ui, boolean isForReceiving) {
		super(ui);
		this.emailAccountDao = this.ui.getFrontlineController().getEmailAccountFactory();
		this.emailManager = this.ui.getFrontlineController().getEmailServerHandler();
		this.isForReceiving = isForReceiving;
		this.accountsListPanel = this.ui.loadComponentFromFile(UI_FILE_LIST_EMAIL_ACCOUNTS_PANEL, this);
		
		// Register with the EventBus to receive notification of new email accounts
		this.eventBus.registerObserver(this);
	}

	public Object getAccountsListPanel() {
		this.refresh();
		
		return this.accountsListPanel;
	}

	public void refresh() {
		Object table = Thinlet.find(this.accountsListPanel, UI_COMPONENT_ACCOUNTS_LIST);
		if (table != null) {
			this.ui.removeAll(table);
			Collection<EmailAccount> emailAccounts;
			
			if (this.isForReceiving) {
				emailAccounts = emailAccountDao.getReceivingEmailAccounts();
			} else {
				emailAccounts = emailAccountDao.getSendingEmailAccounts();
			}
			
			for (EmailAccount acc : emailAccounts) {
				this.ui.add(table, this.ui.getRow(acc));
			}
			
			enableBottomButtons(null);
		}
	}

//> UI EVENT METHODS
		
	public void newEmailAccountSettings () {
		showEmailAccountSettingsDialog(null);
	}
	
	public void editEmailAccountSettings(Object list) {
		Object selected = this.ui.getSelectedItem(list);
		if (selected != null) {
			EmailAccount emailAccount = (EmailAccount) this.ui.getAttachedObject(selected);
			showEmailAccountSettingsDialog(emailAccount);
		}
	}
	
	private void showEmailAccountSettingsDialog(EmailAccount emailAccount) {
		EmailAccountSettingsDialogHandler emailAccountSettingsDialogHandler = new EmailAccountSettingsDialogHandler(this.ui, this.isForReceiving);
		emailAccountSettingsDialogHandler.initDialog(emailAccount);
		this.ui.add(emailAccountSettingsDialogHandler.getDialog());
	}
	
	public void enableBottomButtons(Object table) {
		if (table == null) {
			table = Thinlet.find(accountsListPanel, UI_COMPONENT_ACCOUNTS_LIST);
		}
		
		
		boolean enableEditAndDelete = (this.ui.getSelectedIndex(table) >= 0);
		
		this.ui.setEnabled(Thinlet.find(accountsListPanel, UI_COMPONENT_BT_EDIT), enableEditAndDelete);
		this.ui.setEnabled(Thinlet.find(accountsListPanel, UI_COMPONENT_BT_DELETE), enableEditAndDelete);
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
		Object[] selectedItems = this.ui.getSelectedItems(list);
		boolean hasSelection = selectedItems.length > 0;

		if(popup!= null && !hasSelection && "emailServerListPopup".equals(this.ui.getName(popup))) {
			this.ui.setVisible(popup, false);
			return;
		}
		
		if (hasSelection && popup != null) {
			// If nothing is selected, hide the popup menu
			this.ui.setVisible(popup, hasSelection);
		}
		
		if (toolbar != null && !toolbar.equals(popup)) {
			for (Object o : this.ui.getItems(toolbar)) {
				this.ui.setEnabled(o, hasSelection);
			}
		}
	}
	
	/**
	 * Removes the selected accounts.
	 */
	public void removeSelectedFromAccountList() {
		LOG.trace("ENTER");
		this.ui.removeConfirmationDialog();
		Object list = Thinlet.find(this.accountsListPanel, UI_COMPONENT_ACCOUNTS_LIST);
		Object[] selected = this.ui.getSelectedItems(list);
		for (Object o : selected) {
			EmailAccount acc = this.ui.getAttachedObject(o, EmailAccount.class);
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
				new FrontlineUiUpdateJob() {
					public void run() {
						refresh();
					}
				}.execute();
			}
		} else if (event instanceof UiDestroyEvent) {
			if(((UiDestroyEvent) event).isFor(this.ui)) {
				this.eventBus.unregisterObserver(this);
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
}