/**
 * 
 */
package net.frontlinesms.ui.handler.contacts;

// TODO remove static imports
import static net.frontlinesms.FrontlineSMSConstants.ACTION_ADD_TO_GROUP;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_CONTACTS_IN_GROUP;
import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_CONTACTS_DELETED;
import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_CONTACT_MANAGER_LOADED;
import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_GROUPS_DELETED;
import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_GROUPS_AND_CONTACTS_DELETED;
import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_GROUP_ALREADY_EXISTS;
import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_REMOVING_CONTACTS;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_CONTACT_MANAGER_CONTACT_LIST;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_DELETE_NEW_CONTACT;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_GROUPS_MENU;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_MENU_ITEM_MSG_HISTORY;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_MENU_ITEM_VIEW_CONTACT;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_MI_DELETE;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_MI_SEND_SMS;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_NEW_GROUP;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_PN_CONTACTS;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_SEND_SMS_BUTTON;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_VIEW_CONTACT_BUTTON;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.TAB_CONTACT_MANAGER;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.Order;
import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.domain.Group;
import net.frontlinesms.data.repository.ContactDao;
import net.frontlinesms.data.repository.GroupDao;
import net.frontlinesms.data.repository.GroupMembershipDao;
import net.frontlinesms.events.FrontlineEventNotification;
import net.frontlinesms.ui.Icon;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.events.TabChangedNotification;
import net.frontlinesms.ui.handler.BaseTabHandler;
import net.frontlinesms.ui.handler.ChoiceDialogHandler;
import net.frontlinesms.ui.handler.ComponentPagingHandler;
import net.frontlinesms.ui.handler.PagedComponentItemProvider;
import net.frontlinesms.ui.handler.PagedListDetails;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import thinlet.Thinlet;

/**
 * Event handler for the Contacts tab and associated dialogs.
 * @author Alex Anderson <alex@frontlinesms.com>
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public class ContactsTabHandler extends BaseTabHandler implements PagedComponentItemProvider, SingleGroupSelecterPanelOwner, ContactEditorOwner {
//> STATIC CONSTANTS
	/** UI XML File Path: the Home Tab itself */
	private static final String UI_FILE_CONTACTS_TAB = "/ui/core/contacts/contactsTab.xml";
	private static final String UI_FILE_NEW_GROUP_FORM = "/ui/dialog/newGroupForm.xml"; // TODO move this to the correct path
	
	private static final String COMPONENT_GROUP_SELECTER_CONTAINER = "pnGroupsContainer";
	private static final String COMPONENT_BUTTON_OK	 = "btOk";
	private static final String COMPONENT_CONTACTS_PANEL = "pnContacts";
	private static final String COMPONENT_DELETE_BUTTON = "deleteButton";
	private static final String COMPONENT_SEND_SMS_BUTTON_GROUP_SIDE = "sendSMSButtonGroupSide";
	private static final String I18N_SENTENCE_DELETE_CONTACTS_FROM_GROUPS = "sentence.choice.remove.contacts.of.groups";
	
//> INSTANCE PROPERTIES
	
//> DATA ACCESS OBJECTS
	/** Data access object for {@link Group}s */
	private final GroupDao groupDao;
	/** Data access object for {@link Contact}s */
	private final ContactDao contactDao;
	private final GroupMembershipDao groupMembershipDao;
	
	
//> CACHED THINLET UI COMPONENTS
	/** UI Component component: contact list.  This is cached here to save searching for it later. */
	private Object contactListComponent;
	
	/** Handler for paging of {@link #contactListComponent} */
	private ComponentPagingHandler contactListPager;
	/** String to filter the contacts by */
	private String contactFilter;

	private final GroupSelecterPanel groupSelecter;
	
	/** The selected group in the left panel  */
	private Group selectedGroup;
	private Object newGroupForm;

//> CONSTRUCTORS
	/**
	 * Create a new instance of this class.
	 * @param ui value for {@link #ui}
	 * @param contactDao {@link #contactDao}
	 * @param groupDao {@link #groupDao}
	 */
	public ContactsTabHandler(UiGeneratorController ui) {
		super(ui, true);
		this.contactDao = ui.getFrontlineController().getContactDao();
		this.groupDao = ui.getFrontlineController().getGroupDao();
		this.groupMembershipDao = ui.getFrontlineController().getGroupMembershipDao();
		this.groupSelecter = new GroupSelecterPanel(ui, this, groupDao);
	}
	
	@Override
	public void init() {
		super.init();
		this.groupSelecter.init(ui.getRootGroup());
		
		this.ui.setDeleteAction(this.groupSelecter.getGroupTreeComponent(), "showDeleteOptionDialog", null, this);
		
		ui.add(find(COMPONENT_GROUP_SELECTER_CONTAINER), this.groupSelecter.getPanelComponent(), 0);
	}

//> ACCESSORS
	/** Refreshes the data displayed in the tab. */
	public void refresh() {
		updateGroupList();
	}
	
//> GROUP SELECTION METHODS
	/**
	 * Method invoked when the group/contacts tree selection changes. 
	 * This method updated the contact list according to the new selection.
	 */
	public void groupSelectionChanged(Group selectedGroup) {
		if(log.isTraceEnabled()) log.trace("Group selected: " + selectedGroup);
		this.selectedGroup = selectedGroup;
		
		String contactsPanelTitle;
		boolean enableDeleteButton;
		if(selectedGroup == null) {
			contactsPanelTitle = "";
			enableDeleteButton = false;
		} else {
			contactsPanelTitle = InternationalisationUtils.getI18nString(COMMON_CONTACTS_IN_GROUP, selectedGroup.getName());
			enableDeleteButton = !this.ui.isDefaultGroup(selectedGroup);
		}
		this.ui.setText(find(COMPONENT_CONTACTS_PANEL), contactsPanelTitle);
		
		Object buttonPanelContainer = find(COMPONENT_GROUP_SELECTER_CONTAINER);
		Object deleteButton = this.ui.find(buttonPanelContainer, COMPONENT_DELETE_BUTTON);
		this.ui.setEnabled(deleteButton, enableDeleteButton);
		
		Object btSendSmsToGroup = this.ui.find(buttonPanelContainer, COMPONENT_SEND_SMS_BUTTON_GROUP_SIDE);
		this.ui.setEnabled(btSendSmsToGroup, selectedGroup != null);
		
		updateContactList();
	}
	
	public void groupNameChanged (String groupName) {
		boolean shouldEnableOKButton = (groupName != null && !groupName.equals(""));
		if (this.newGroupForm != null) {
			Object okButton = this.ui.find(this.newGroupForm, COMPONENT_BUTTON_OK);
			this.ui.setEnabled(okButton, shouldEnableOKButton);
		}
	}
	
//> CONTACT EDITING METHODS
	/** @see ContactEditorOwner#contactCreationComplete(Contact) */
	public void contactCreationComplete(Contact contact) {
		// Refresh the Contacts tab, and make sure that the group and contact who were previously selected are still selected
		updateContactList();
	};
	/** @see ContactEditorOwner#contactEditingComplete(Contact) */
	public void contactEditingComplete(Contact contact) {
		contactCreationComplete(contact);
	}
	
//> PAGING METHODS
	public PagedListDetails getListDetails(Object list, int startIndex, int limit) {
		if(list == this.contactListComponent) {
			return getContactListDetails(startIndex, limit);
		} else throw new IllegalStateException();
	}
	
	private PagedListDetails getContactListDetails(int startIndex, int limit) {
		Group selectedGroup = this.groupSelecter.getSelectedGroup();
		
		if(selectedGroup == null) {
			return PagedListDetails.EMPTY;
		} else {
			int totalItemCount = groupMembershipDao.getFilteredMemberCount(selectedGroup, this.contactFilter);
			List<Contact> contacts = groupMembershipDao.getFilteredMembersSorted(selectedGroup, contactFilter, Contact.Field.NAME, Order.ASCENDING, startIndex, limit);
			Object[] listItems = toThinletComponents(contacts);
			
			return new PagedListDetails(totalItemCount, listItems);
		}
		
	}
	
	private Object[] toThinletComponents(List<Contact> contacts) {
		Object[] components = new Object[contacts.size()];
		for (int i = 0; i < components.length; i++) {
			Contact c = contacts.get(i);
			components[i] = ui.getRow(c);
		}
		return components;
	}

//> UI METHODS
	/** Show editor for new contact. */
	public void showNewContactDialog() {
		ContactEditor editor = new ContactEditor(ui, this);
		Group selectedGroup = this.groupSelecter.getSelectedGroup();
		editor.show(selectedGroup);
	}
	
	/**
	 * Shows the delete option dialog
	 * If the group contains contacts, it asks the user if he/she wants to remove the selected contacts from database.
	 * Otherwise, it only shows a confirmation dialog
	 */
	public void showDeleteOptionDialog() {
		Group g = this.groupSelecter.getSelectedGroup();
		if (!this.ui.isDefaultGroup(g)) {
			if (groupMembershipDao.getMemberCount(g) > 0) {
				// If the group is not empty, we ask if the user also wants to delete the contacts
				ChoiceDialogHandler choiceDialogHandler = new ChoiceDialogHandler(this.ui, this);
				choiceDialogHandler.showChoiceDialog(true,
						"removeSelectedFromGroupList('true', choiceDialog)",
						"removeSelectedFromGroupList('false', choiceDialog)",
						I18N_SENTENCE_DELETE_CONTACTS_FROM_GROUPS);
			} else {
				// Otherwise, the
				showConfirmationDialog("deleteSelectedGroup");
			}
		}
	}
	
	/**
	 * Launches the deletion of the selected group
	 * if the user confirmed it in the confirm dialog
	 */
	public void deleteSelectedGroup () {
		this.ui.removeConfirmationDialog();
		removeSelectedFromGroupList(false, null);
	}

	/**
	 * Shows contact dialog to allow edition of the selected contact.
	 * <br>This method affects the advanced mode.
	 * @param list
	 */
	public void showContactDetails(Object list) {
		Object selected = this.ui.getSelectedItem(list);
		if(selected != null) {
			ContactEditor editor = new ContactEditor(ui, this);
			editor.show(this.ui.getAttachedObject(selected, Contact.class));
		}
	}
	
	/**
	 * Populates the pop up menu with all groups create by users.
	 * 
	 * @param popUp
	 * @param list
	 */
	public void populateGroups(Object popUp, Object list) {
		Object[] selectedItems = this.ui.getSelectedItems(list);
		this.ui.setVisible(popUp, this.ui.getSelectedItems(list).length > 0);
		if (selectedItems.length == 0) {
			//Nothing selected
			boolean none = true;
			for (Object o : this.ui.getItems(popUp)) {
				if (this.ui.getName(o).equals(COMPONENT_NEW_GROUP)
						|| this.ui.getName(o).equals("miNewContact")) {
					this.ui.setVisible(o, true);
					none = false;
				} else {
					this.ui.setVisible(o, false);
				}
			}
			this.ui.setVisible(popUp, !none);
		} else if (this.ui.getAttachedObject(selectedItems[0]) instanceof Contact) {
			for (Object o : this.ui.getItems(popUp)) {
				String name = this.ui.getName(o);
				if (name.equals(COMPONENT_MENU_ITEM_MSG_HISTORY) 
						|| name.equals(COMPONENT_MENU_ITEM_VIEW_CONTACT)) {
					this.ui.setVisible(o, this.ui.getSelectedItems(list).length == 1);
				} else if (!name.equals(COMPONENT_GROUPS_MENU)) {
					this.ui.setVisible(o, true);
				}
			}
			Object menu = this.ui.find(popUp, COMPONENT_GROUPS_MENU);
			this.ui.removeAll(menu);
			List<Group> allGroups = this.groupDao.getAllGroups();
			for (Group g : allGroups) {
				Object menuItem = Thinlet.create(Thinlet.MENUITEM);
				this.ui.setText(menuItem, g.getPath());
				this.ui.setIcon(menuItem, Icon.GROUP);
				this.ui.setAttachedObject(menuItem, g);
				this.ui.setAction(menuItem, "addToGroup(this)", menu, this);
				this.ui.add(menu, menuItem);
			}
			this.ui.setVisible(menu, allGroups.size() != 0);
			String menuName = InternationalisationUtils.getI18nString(ACTION_ADD_TO_GROUP);
			this.ui.setText(menu, menuName);
			
			Object menuRemove = this.ui.find(popUp, "groupsMenuRemove");
			if (menuRemove != null) {
				Contact c = this.ui.getContact(this.ui.getSelectedItem(list));
				this.ui.removeAll(menuRemove);
				List<Group> groups = this.groupMembershipDao.getGroups(c);
				for (Group g : groups) {
					Object menuItem = Thinlet.create(Thinlet.MENUITEM);
					this.ui.setText(menuItem, g.getPath());
					this.ui.setIcon(menuItem, Icon.GROUP);
					this.ui.setAttachedObject(menuItem, g);
					this.ui.setAction(menuItem, "removeFromGroup(this)", menuRemove, this);
					this.ui.add(menuRemove, menuItem);
				}
				this.ui.setEnabled(menuRemove, groups.size() != 0);
			}
		} else {
			Group g = this.ui.getGroup(this.ui.getSelectedItem(list));
			//GROUPS OR BOTH
			for (Object o : this.ui.getItems(popUp)) {
				String name = this.ui.getName(o);
				if (COMPONENT_NEW_GROUP.equals(name) 
						|| COMPONENT_MI_SEND_SMS.equals(name)
						|| COMPONENT_MI_DELETE.equals(name)
						|| COMPONENT_MENU_ITEM_MSG_HISTORY.equals(name)
						|| "miNewContact".equals(name)) {
					this.ui.setVisible(o, true);
				} else {
					this.ui.setVisible(o, false);
				}
				if (COMPONENT_MI_DELETE.equals(name)) {
					this.ui.setVisible(o, !this.ui.isDefaultGroup(g));
				}
				
				// FIXME this is superfluous - always sets vis to true
				if (COMPONENT_NEW_GROUP.equals(name)) {
					this.ui.setVisible(o, true);
				}
			}
		}
	}
	
	/**
	 * Shows the new group dialog.
	 * @param groupList
	 */
	public void showNewGroupDialog() {
		newGroupForm = this.ui.loadComponentFromFile(UI_FILE_NEW_GROUP_FORM, this);
		ui.setAttachedObject(newGroupForm, this.groupSelecter.getSelectedGroup());
		this.ui.add(newGroupForm);
	}

	/** Updates the list of contacts with the new filter. */
	public void filterContacts() {
		updateContactList();
	}

	/**
	 * Applies a text filter to the contact list.  The list is not updated until {@link #filterContacts()}
	 * is called.
	 * @param contactFilter The new filter.
	 */	
	public void setContactFilter(String contactFilter) {
		this.contactFilter = contactFilter;
	}

	/**
	 * Enables or disables the buttons on the Contacts tab (advanced mode).
	 * @param contactList
	 */
	public void enabledButtonsAfterSelection(Object contactList) {
		boolean enabled = this.ui.getSelectedItems(contactList).length > 0;
		this.ui.setEnabled(find(COMPONENT_DELETE_NEW_CONTACT), enabled);
		this.ui.setEnabled(find(COMPONENT_VIEW_CONTACT_BUTTON), enabled);
		this.ui.setEnabled(find(COMPONENT_SEND_SMS_BUTTON), enabled);
	}

	/**
	 * Adds selected contacts to group.
	 * 
	 * @param item The item holding the destination group.
	 */
	public void addToGroup(Object item) {
		log.trace("ENTER");
		Object[] selected = null;
		selected = this.ui.getSelectedItems(contactListComponent);
		// Add to the selected groups...
		Group destination = this.ui.getGroup(item);
		// Let's check all the selected items.  Any that are groups should be added to!
		for (Object component : selected) {
			if (this.ui.isAttachment(component, Contact.class)) {
				Contact contact = this.ui.getContact(component);
				log.debug("Adding Contact [" + contact.getName() + "] to [" + destination + "]");
				if(this.groupMembershipDao.addMember(destination, contact)) {
					groupDao.updateGroup(destination);
				}
			}
		}
		updateGroupList();
		log.trace("EXIT");
	}
	
	public void removeSelectedFromGroupList(String deleteContacts, Object dialog) {
		removeSelectedFromGroupList(Boolean.valueOf(deleteContacts), dialog);
	}
	/**
	 * Remove selected groups and optionally contacts.
	 * @param confirmationDialog the confirmation dialog, or <code>null</code> if none was displayed
	 */
	private void removeSelectedFromGroupList(boolean deleteContacts, Object confirmationDialog) {
		log.trace("ENTER");
		if (confirmationDialog != null) {
			this.ui.removeDialog(confirmationDialog);
		}

		Group selectedGroup = this.groupSelecter.getSelectedGroup();
		if(!ui.isDefaultGroup(selectedGroup)) {
			log.debug("Selected Group [" + selectedGroup.getName() + "]");
			log.debug("Remove Contacts from database [" + deleteContacts + "]");
			if (!ui.isDefaultGroup(selectedGroup)) {
				log.debug("Removing group [" + selectedGroup.getName() + "] from database");
				groupDao.deleteGroup(selectedGroup, deleteContacts);
				this.groupSelecter.selectGroup(groupSelecter.getRootGroup());
			} else {
				// Inside a default group
				throw new IllegalStateException();
			}
		}
		
		Object sms = ui.find(find(COMPONENT_GROUP_SELECTER_CONTAINER), COMPONENT_SEND_SMS_BUTTON_GROUP_SIDE);
		ui.setEnabled(sms, selectedGroup != null);
		ui.infoMessage(InternationalisationUtils.getI18nString(
				deleteContacts ? MESSAGE_GROUPS_AND_CONTACTS_DELETED : MESSAGE_GROUPS_DELETED));
		refresh();
		log.trace("EXIT");
	}

	/**
	 * Removes the contacts selected in the contacts list from the group which is selected in the groups tree.
	 * @param selectedGroup A set of thinlet components with group members attached to them.
	 */
	public void removeFromGroup(Object selectedGroup) {
		Object[] selectedContacts = this.ui.getSelectedItems(this.ui.find(COMPONENT_CONTACT_MANAGER_CONTACT_LIST));
		Group group = this.ui.getGroup(selectedGroup);
		
		for (Object selected : selectedContacts) {
			Contact c = this.ui.getContact(selected);
			this.groupMembershipDao.removeMember(group, c);
		}
		this.refresh();		
	}

	/** Removes the selected contacts of the supplied contact list component. */
	public void deleteSelectedContacts() {
		log.trace("ENTER");
		Group selectedGroup = this.groupSelecter.getSelectedGroup();
		this.ui.removeConfirmationDialog();
		this.ui.setStatus(InternationalisationUtils.getI18nString(MESSAGE_REMOVING_CONTACTS));
		final Object[] selected = this.ui.getSelectedItems(contactListComponent);
		for (Object o : selected) {
			Contact contact = ui.getContact(o);
			log.debug("Deleting contact [" + contact.getName() + "]");
			contactDao.deleteContact(contact);
		}
		ui.alert(InternationalisationUtils.getI18nString(MESSAGE_CONTACTS_DELETED));
		refresh();
		this.groupSelecter.selectGroup(selectedGroup);
		
		log.trace("EXIT");
	}

	/**
	 * Creates a new group with the supplied name.
	 * 
	 * @param newGroupName The desired group name.
	 * @param dialog the dialog holding the information to where we should create this new group.
	 */
	public void createNewGroup(String newGroupName, Object dialog) {
		// The selected parent group should be attached to this dialog.  Get it,
		// create the new group, update the group list and then remove the dialog.
		Group selectedParentGroup = this.ui.getGroup(dialog);
		doGroupCreation(newGroupName, dialog, selectedParentGroup);
	}
	
//> PRIVATE UI HELPER METHODS
	/**
	 * Creates a group with the supplied name and inside the supplied parent .
	 * @param newGroupName The desired group name.
	 * @param dialog The dialog to be removed after the operation.
	 * @param selectedParentGroup
	 */
	private void doGroupCreation(String newGroupName, Object dialog, Group selectedParentGroup) {
		log.trace("ENTER");
		if(log.isDebugEnabled()) {
			String parentGroupName = selectedParentGroup == null ? "null" : selectedParentGroup.getName();
			log.debug("Parent group [" + parentGroupName + "]");
		}
		if(selectedParentGroup == null) {
			selectedParentGroup = ui.getRootGroup();
		}

		log.debug("Group Name [" + newGroupName + "]");
		try {
			if(log.isDebugEnabled()) log.debug("Creating group with name: " + newGroupName + " and parent: " + selectedParentGroup);
			
			Group g = new Group(selectedParentGroup, newGroupName);
			this.groupDao.saveGroup(g);
			
			this.groupSelecter.addGroup(g);
			
			if (dialog != null) this.ui.remove(dialog);
			this.selectedGroup = g;
			this.updateGroupList();
			
			log.debug("Group created successfully!");
		} catch (DuplicateKeyException e) {
			log.debug("A group with this name already exists.", e);
			this.ui.alert(InternationalisationUtils.getI18nString(MESSAGE_GROUP_ALREADY_EXISTS));
		}
		log.trace("EXIT");
	}
	
	/** Repopulates the contact list according to the current filter. */
	public void updateContactList() {
		this.contactListPager.setCurrentPage(0);
		this.contactListPager.refresh();
		enabledButtonsAfterSelection(contactListComponent);
	}

	/** Updates the group tree. */
	private void updateGroupList() {
		this.groupSelecter.refresh(true);
		this.groupSelecter.selectGroup(selectedGroup);
			
		Object btSendSmsToGroup = ui.find(find(COMPONENT_GROUP_SELECTER_CONTAINER), COMPONENT_SEND_SMS_BUTTON_GROUP_SIDE);
		this.ui.setEnabled(btSendSmsToGroup, this.groupSelecter.getSelectedGroup() != null);
		
		updateContactList();
	}
	
//> EVENT HANDLER METHODS
	public void addToContactList(Contact contact, Group group) {
		List<Group> selectedGroupsFromTree = new ArrayList<Group>();
		
		Group g = this.groupSelecter.getSelectedGroup();
		selectedGroupsFromTree.add(g);
		
		if (selectedGroupsFromTree.contains(group)) {
			int limit = this.contactListPager.getMaxItemsPerPage();
			//Adding
			if (this.ui.getItems(contactListComponent).length < limit) {
				this.ui.add(contactListComponent, this.ui.getRow(contact));
			}
		}
		
		this.groupSelecter.refresh(true);
//		updateTree(group);
	}
	
//> UI PASS-THROUGH METHODS TO UiGC
	/** @see UiGeneratorController#groupList_expansionChanged(Object) */
	public void groupList_expansionChanged(Object groupList) {
		this.ui.groupList_expansionChanged(groupList);
	}
	/** Shows the compose message dialog for all members of the selected group. */
	public void sendSmsToGroup() {
		this.ui.show_composeMessageForm(this.groupSelecter.getSelectedGroup());
	}
	/** Shows the compose message dialog for all selected contacts. */
	public void sendSmsToContacts() {
		Object[] selectedItems = ui.getSelectedItems(contactListComponent);
		if(selectedItems.length > 0) {
			HashSet<Object> contacts = new HashSet<Object>(); // Must be Objects because of stupid method sig of show_comp...
			for(Object selectedItem : selectedItems) {
				contacts.add(ui.getAttachedObject(selectedItem, Contact.class));
			}

			this.ui.show_composeMessageForm(contacts);	
		}
		
	}
	/**
	 * Shows the export wizard dialog for exporting contacts.
	 * @param list The list to get selected items from.
	 * @param type the name of the type to export
	 */
	public void showExportWizard(Object list) {
		this.ui.showExportWizard(list, "contacts");
	}

//> INSTANCE HELPER METHODS
	/** Initialise dynamic contents of the tab component. */
	protected Object initialiseTab() {
		Object tabComponent = ui.loadComponentFromFile(UI_FILE_CONTACTS_TAB, this);
		
		
		// Cache Thinlet UI components
		contactListComponent = this.ui.find(tabComponent, COMPONENT_CONTACT_MANAGER_CONTACT_LIST);
		
		this.contactListPager = new ComponentPagingHandler(this.ui, this, contactListComponent);
		Object pnContacts = this.ui.find(tabComponent, COMPONENT_PN_CONTACTS);
		this.ui.add(pnContacts, this.contactListPager.getPanel());
		
		//initContactTableForSorting();
		
		return tabComponent;
	}
	
	/**
	 * UI event called when the user changes tab
	 */
	public void notify(FrontlineEventNotification notification) {
		super.notify(notification);
		// This object is registered to the UIGeneratorController and get notified when the users changes tab
		if(notification instanceof TabChangedNotification) {
			String newTabName = ((TabChangedNotification) notification).getNewTabName();
			if (newTabName.equals(TAB_CONTACT_MANAGER)) {
				threadSafeRefresh();
				this.ui.setStatus(InternationalisationUtils.getI18nString(MESSAGE_CONTACT_MANAGER_LOADED));
			}
		}
	}

//> STATIC FACTORIES

//> STATIC HELPER METHODS
}
