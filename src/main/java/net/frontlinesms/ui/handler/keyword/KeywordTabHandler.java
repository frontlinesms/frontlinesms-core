/**
 * 
 */
package net.frontlinesms.ui.handler.keyword;

import static net.frontlinesms.FrontlineSMSConstants.COMMON_BLANK;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_EDITING_KEYWORD;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_KEYWORD_ACTIONS_OF;
import static net.frontlinesms.FrontlineSMSConstants.DEFAULT_END_DATE;
import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_KEYWORDS_LOADED;
import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_KEYWORD_EXISTS;
import static net.frontlinesms.FrontlineSMSConstants.MESSAGE_KEYWORD_SAVED;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_ACTION_LIST;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_CB_AUTO_REPLY;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_KEYWORDS_DIVIDER;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_KEYWORD_LIST;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_LB_REMAINING_CHARS;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_MENU_ITEM_CREATE;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_MENU_ITEM_EDIT;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_NEW_KEYWORD_BUTTON_DONE;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_NEW_KEYWORD_FORM_DESCRIPTION;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_NEW_KEYWORD_FORM_KEYWORD;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_NEW_KEYWORD_FORM_TITLE;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.TAB_KEYWORD_MANAGER;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;

import thinlet.Thinlet;

import net.frontlinesms.FrontlineSMS;
import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.domain.Group;
import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.data.domain.KeywordAction;
import net.frontlinesms.data.repository.KeywordActionDao;
import net.frontlinesms.data.repository.KeywordDao;
import net.frontlinesms.events.FrontlineEventNotification;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.events.FrontlineUiUpdateJob;
import net.frontlinesms.ui.events.TabChangedNotification;
import net.frontlinesms.ui.handler.BaseTabHandler;
import net.frontlinesms.ui.handler.ComponentPagingHandler;
import net.frontlinesms.ui.handler.PagedComponentItemProvider;
import net.frontlinesms.ui.handler.PagedListDetails;
import net.frontlinesms.ui.handler.contacts.GroupSelecterDialog;
import net.frontlinesms.ui.handler.contacts.SingleGroupSelecterDialogOwner;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

/**
 * UI Event handler for Keywords tab
 * @author Alex Anderson alex@frontlinesms.com
 * @author Carlos Eduardo Genz kadu@masabi.com
 */
public class KeywordTabHandler extends BaseTabHandler implements PagedComponentItemProvider, SingleGroupSelecterDialogOwner {
//> UI LAYOUT FILES
	public static final String UI_FILE_KEYWORDS_TAB = "/ui/core/keyword/keywordsTab.xml";
	public static final String UI_FILE_KEYWORDS_SIMPLE_VIEW = "/ui/core/keyword/pnSimpleView.xml";
	public static final String UI_FILE_KEYWORDS_ADVANCED_VIEW = "/ui/core/keyword/pnAdvancedView.xml";
	public static final String UI_FILE_NEW_KEYWORD_FORM = "/ui/core/keyword/newKeywordForm.xml";

	public static final String COMPONENT_KEY_PANEL = "keyPanel";
	private static final String COMPONENT_JOIN_GROUP_SELECT_LABEL = "lbJoinGroup";
	private static final String COMPONENT_LEAVE_GROUP_SELECT_LABEL = "lbLeaveGroup";

	public static final String COMPONENT_CB_ACTION_TYPE = "cbActionType";
	public static final String COMPONENT_KEY_ACT_PANEL = "keyActPanel";
	public static final String COMPONENT_LB_KEYWORD_DESCRIPTION = "lbKeywordDescription";
	public static final String COMPONENT_TA_KEYWORD_DESCRIPTION = "newKeywordForm_description";
	public static final String COMPONENT_TF_AUTO_REPLY = "tfAutoReply";
	public static final String COMPONENT_TF_KEYWORD = "tfKeyword";
	
	private static final String I18N_CREATE_KEYWORD = "action.new.keyword";
	private static final String I18N_KEYWORD_ACTION_NO_GROUP = "common.keyword.actions.no.group";
	private static final String I18N_COMMON_NONE = "common.none";

	private KeywordDao keywordDao;
	private KeywordActionDao keywordActionDao;

	private Object keywordListComponent;
	private ComponentPagingHandler keywordListPagingHandler;
	
	/** Flag used to indicate if we are selecting the join group or the leave group. */
	private boolean selectingJoinGroup;
	/** Add/Edit keyword form */
	private Object keywordForm;
	
	public KeywordTabHandler(UiGeneratorController ui) {
		super(ui, true);
		
		FrontlineSMS frontlineController = ui.getFrontlineController();
		this.keywordDao = frontlineController.getKeywordDao();
		this.keywordActionDao = frontlineController.getKeywordActionDao(); 
	}

	protected Object initialiseTab() {
		Object tabComponent = ui.loadComponentFromFile(UI_FILE_KEYWORDS_TAB, this);
		this.keywordListComponent = ui.find(tabComponent, COMPONENT_KEYWORD_LIST);
		this.keywordListPagingHandler = new ComponentPagingHandler(ui, this, keywordListComponent);
		
		// Add the paging controls just below the list of keyword
		Object pageControls = keywordListPagingHandler.getPanel();
		ui.setHAlign(pageControls, Thinlet.RIGHT);
		Object parentPanel = ui.getParent(keywordListComponent);
		ui.add(parentPanel, pageControls, ui.getIndex(parentPanel, keywordListComponent)+1);
		return tabComponent;
	}

	public void refresh() {
		updateKeywordList();
	}
	
//> UI EVENT METHODS
	/**
	 * Shows the export wizard dialog for exporting contacts.
	 * @param list The list to get selected items from.
	 */
	public void showExportWizard(Object list) {
		this.ui.showExportWizard(list, "keywords");
	}
	
	public void autoReplyChanged(String reply, Object cbAutoReply) {
		ui.setSelected(cbAutoReply, reply.length() > 0);
	}
	
	/**
	 * @param index
     *  0 - Auto Reply
     *  1 - Auto Forward
     *  2 - Join Group
     *  3 - Leave Group
     *  4 - E-mail
     *  5 - External Command 
     */
	public void keywordTab_createAction(int index) {
		BaseActionDialog dialog;
		switch (index) {
			case 0:
				dialog = new ReplyActionDialog(ui, this);
				break;
			case 1:
				dialog = new ForwardActionDialog(ui, this); 
				break;
			case 2:
				dialog = new JoinGroupActionDialog(ui, this);
				break;
			case 3:
				dialog = new LeaveGroupActionDialog(ui, this); 
				break;
			case 4:
				dialog = new EmailActionDialog(ui, this);
				break;
			case 5:
				dialog = new ExternalCommandActionDialog(ui, this);
				break;
			default: throw new IllegalStateException("Unhandled action type: " + index);
		}
		
		Keyword keyword = ui.getKeyword(ui.getSelectedItem(keywordListComponent));
		dialog.init(keyword);
		dialog.show();
	}
	
	public void keywordShowAdvancedView() {
		Object divider = find(COMPONENT_KEYWORDS_DIVIDER);
		if (ui.getItems(divider).length >= 2) {
			ui.remove(ui.getItems(divider)[ui.getItems(divider).length - 1]);
		}
		Object panel = ui.loadComponentFromFile(UI_FILE_KEYWORDS_ADVANCED_VIEW, this);
		Object table = ui.find(panel, COMPONENT_ACTION_LIST);
		Keyword keyword = ui.getKeyword(ui.getSelectedItem(keywordListComponent));
		Object lbKeywordDescription = ui.find(panel, COMPONENT_LB_KEYWORD_DESCRIPTION);
		
		String keywordDescription = getDisplayableDescription(keyword);
		if (keywordDescription != null && keywordDescription.length() > 0) {
			ui.setText(lbKeywordDescription, keywordDescription);
		} else {
			ui.remove(lbKeywordDescription);
		}
		
		ui.setText(panel, InternationalisationUtils.getI18nString(COMMON_KEYWORD_ACTIONS_OF, getDisplayableKeyword(keyword)));
		for (KeywordAction action : this.keywordActionDao.getActions(keyword)) {
			ui.add(table, ui.getRow(action));
		}
		enableKeywordActionFields(table, ui.find(panel, COMPONENT_KEY_ACT_PANEL));
		ui.add(divider, panel);
	}
	
	/**
	 * UI Method.
	 * Deletes the keyword that is selected in {@link #keywordListComponent}.
	 */
	public void removeSelectedFromKeywordList() {
		// Get the selected keyword
		Object selected = ui.getSelectedItem(keywordListComponent);
		Keyword keyword = ui.getAttachedObject(selected, Keyword.class);
		
		// Delete attached actions, and then delete they keyword
		for(KeywordAction action : this.keywordActionDao.getActions(keyword)) {
			this.keywordActionDao.deleteKeywordAction(action);
		}
		this.keywordDao.deleteKeyword(keyword);

		// Now update the UI - remove the selected item and set a new selected item
		ui.remove(selected);
		ui.setSelectedIndex(keywordListComponent, 0);
		showSelectedKeyword();

		// Finally, remove the "confirm delete" dialog
		ui.removeConfirmationDialog();
	}
	
	/**
	 * Event fired when the popup menu (in the keyword manager tab) is shown.
	 * If there is no keyword listed in the tree, the only option allowed is
	 * to create one. Otherwise, all components are allowed.
	 */
	public void enableKeywordFields(Object component) {
		log.trace("ENTER");
		int selected = ui.getSelectedIndex(keywordListComponent);
		if (selected <= 0) {
			log.debug("Nothing selected, so we only allow keyword creation.");
			for (Object o : ui.getItems(component)) {
				String name = ui.getString(o, Thinlet.NAME);
				if (name == null) {
					continue;
				} else {
					// "New" button is always enabled
					// "Edit" button is only enabled if a keyword is selected, even the blank keyword
					boolean isEnabled = (name.equals(COMPONENT_MENU_ITEM_CREATE) || (name.equals(COMPONENT_MENU_ITEM_EDIT) && selected == 0));
					ui.setEnabled(o, isEnabled);
				}
			}
		} else {
			//Keyword selected
			for (Object o : ui.getItems(component)) {
				ui.setEnabled(o, true);
			}
		}
		log.trace("EXIT");
	}
	
	public void keywordTab_newAction(Object combo) {
		keywordTab_createAction(ui.getSelectedIndex(combo));
	}

	/**
	 * Shows the new keyword dialog.
	 * @param parentKeyword
	 */
	public void showNewKeywordForm() {
		String title = InternationalisationUtils.getI18nString(I18N_CREATE_KEYWORD);
		this.keywordForm = ui.loadComponentFromFile(UI_FILE_NEW_KEYWORD_FORM, this);
		ui.setText(ui.find(keywordForm, COMPONENT_NEW_KEYWORD_FORM_TITLE), title);
		
		// Update the display for the number of characters left in the description field
		this.keywordDescriptionChanged("");
		
		ui.add(keywordForm);
	}

	/**
	 * Create a new keyword with the supplied information (newKeyword and description).
	 * 
	 * @param formPanel The panel to be removed from the application.
	 * @param newKeyword The desired keyword.
	 * @param description The description for this new keyword.
	 */
	public void do_createKeyword(String newKeyword, String description) {
		log.trace("ENTER");
		log.debug("Creating keyword [" + newKeyword + "] with description [" + description + "]");
		try {
			// Trim the keyword to remove trailing and leading whitespace
			newKeyword = newKeyword.trim();
			// Remove any double-spaces within the keyword
			newKeyword = newKeyword.replaceAll("\\s+", " ");
			
			Keyword keyword = new Keyword(newKeyword, description);
			this.keywordDao.saveKeyword(keyword);
		} catch (DuplicateKeyException e) {
			ui.alert(InternationalisationUtils.getI18nString(MESSAGE_KEYWORD_EXISTS));
			log.trace("EXIT");
			return;
		}
		updateKeywordList();
		removeDialog(this.keywordForm);
		log.trace("EXIT");
	}
	
	public void keywordTab_doSave(Object panel) {
		log.trace("ENTER");
		long startDate;
		try {
			startDate = InternationalisationUtils.parseDate(InternationalisationUtils.getDefaultStartDate()).getTime();
		} catch (ParseException e) {
			log.debug("We never should get this", e);
			log.trace("EXIT");
			return;
		}
		
		// Get the KeywordAction details
		String replyText = keywordSimple_getAutoReply(panel);
		Group joinGroup = keywordSimple_getJoin();
		Group leaveGroup = keywordSimple_getLeave();
		
		// Get the keyword attached to the selected item.  If the "Add Keyword" option is selected,
		// there will be no keyword attached to it.
		Keyword keyword = null;
		Object selectedKeywordItem = ui.getSelectedItem(keywordListComponent);
		if(selectedKeywordItem != null) keyword = ui.getKeyword(selectedKeywordItem);
		
		if (keyword == null) {
			//Adding keyword as well as actions
			String newkeyword = ui.getText(ui.find(panel, COMPONENT_TF_KEYWORD));
			try {
				keyword = new Keyword(newkeyword, "");
				this.keywordDao.saveKeyword(keyword);
			} catch (DuplicateKeyException e) {
				ui.alert(InternationalisationUtils.getI18nString(MESSAGE_KEYWORD_EXISTS));
				log.trace("EXIT");
				return;
			}
			keywordTab_doClear(panel);
		} else {
			// Editing an existent keyword.  This keyword may already have actions applied to it, so
			// we need to check for actions and update them as appropriate.
			KeywordAction replyAction = this.keywordActionDao.getAction(keyword, KeywordAction.Type.REPLY);
			if (replyAction != null) {
				if (replyText == null) {
					// The reply action has been removed
					keywordActionDao.deleteKeywordAction(replyAction);
				} else {
					replyAction.setReplyText(replyText);
					this.keywordActionDao.updateKeywordAction(replyAction);
					//We set null to don't add it in the end
					replyText = null;
				}
			}
			
			KeywordAction joinAction = this.keywordActionDao.getAction(keyword, KeywordAction.Type.JOIN);
			if (joinAction != null) {
				if (joinGroup == null) {
					// Previous join action has been removed, so delete it.
					keywordActionDao.deleteKeywordAction(joinAction);
				} else {
					// Group to join has been updated
					joinAction.setGroup(joinGroup);
					this.keywordActionDao.updateKeywordAction(joinAction);
					// Join Group has been handled, so unset it.
					joinGroup = null;
				}
			}
			
			KeywordAction leaveAction = this.keywordActionDao.getAction(keyword, KeywordAction.Type.LEAVE);
			if (leaveAction != null) {
				if (leaveGroup == null) {
					keywordActionDao.deleteKeywordAction(leaveAction);
				} else {
					leaveAction.setGroup(leaveGroup);
					this.keywordActionDao.updateKeywordAction(leaveAction);
					//We set null to don't add it in the end
					leaveGroup = null;
				}
			}
		}
		
		// Handle creation of new KeywordActions if required
		if (replyText != null) {
			KeywordAction action = KeywordAction.createReplyAction(keyword, replyText, startDate, DEFAULT_END_DATE);
			keywordActionDao.saveKeywordAction(action);
		}
		if (joinGroup != null) {
			KeywordAction action = KeywordAction.createGroupJoinAction(keyword, joinGroup, startDate, DEFAULT_END_DATE);
			keywordActionDao.saveKeywordAction(action);
		}
		if (leaveGroup != null) {
			KeywordAction action = KeywordAction.createGroupLeaveAction(keyword, leaveGroup, startDate, DEFAULT_END_DATE);
			keywordActionDao.saveKeywordAction(action);
		}
		
		// Refresh the UI
		updateKeywordList();
		ui.infoMessage(InternationalisationUtils.getI18nString(MESSAGE_KEYWORD_SAVED));
		log.trace("EXIT");
	}

	/**
	 * Removes selected keyword action.
	 */
	public void removeSelectedFromKeywordActionsList() {
		ui.removeConfirmationDialog();
		Object list = find(COMPONENT_ACTION_LIST);
		Object selected = ui.getSelectedItem(list);
		if (selected != null) {
			KeywordAction keyAction = ui.getAttachedObject(selected, KeywordAction.class);
			this.keywordActionDao.deleteKeywordAction(keyAction);
			ui.remove(selected);
			enableKeywordActionFields(list, find(COMPONENT_KEY_ACT_PANEL));
		}
	}

	/**
	 * Event fired when the popup menu (in the keyword manager tab) is shown.
	 * If there is no keyword action listed in the table, the only option allowed is
	 * to create one. Otherwise, all components are allowed.
	 */
	public void enableKeywordActionFields(Object table, Object component) {
		log.trace("ENTER");
		int selected = ui.getSelectedIndex(table);
		if (selected < 0) {
			log.debug("Nothing selected, so we only allow keyword action creation.");
			for (Object o : ui.getItems(component)) {
				String name = ui.getString(o, Thinlet.NAME);
				if (name == null)
					continue;
				boolean isEnabled = name.equals(COMPONENT_MENU_ITEM_CREATE)
						|| name.equals(COMPONENT_CB_ACTION_TYPE);
				ui.setEnabled(o, isEnabled);
			}
		} else {
			//Keyword action selected
			for (Object o : ui.getItems(component)) {
				ui.setEnabled(o, true);
			}
		}
		log.trace("EXIT");
	}
	
	public void keywordTab_doClear(Object panel) {
		ui.setText(ui.find(panel, COMPONENT_TF_KEYWORD), "");
		ui.setSelected(ui.find(panel, COMPONENT_CB_AUTO_REPLY), false);
		ui.setText(ui.find(panel, COMPONENT_TF_AUTO_REPLY), "");
		// TODO do we need to clear the group displays here?
	}
	
	/**
	 * Method called when the user has selected the edit option inside the Keywords tab.
	 * 
	 * @param tree
	 */
	public void keywordManager_edit(Object tree) {
		log.trace("ENTER");
		Object selectedObj = ui.getSelectedItem(tree);
		if (ui.isAttachment(selectedObj, KeywordAction.class)) {
			//KEYWORD ACTION EDITION
			KeywordAction action = ui.getKeywordAction(selectedObj);
			log.debug("Editing keyword action [" + action + "]");
			showActionEditDialog(action);
		} else {
			Keyword keyword = ui.getKeyword(selectedObj);
			//KEYWORD EDITION
			if (keyword != null) {
				log.debug("Editing keyword [" + keyword.getKeyword() + "]");
				showKeywordDialogForEdition(keyword);
			}
		} 
		log.trace("EXIT");
	}
	
	/**
	 * Method called when the user has finished to edit a keyword.
	 * 
	 * @param dialog The dialog, which is holding the current reference to the keyword being edited.
	 * @param desc The new description for the keyword.
	 * @throws DuplicateKeyException 
	 */
	public void finishKeywordEdition(Object dialog, String desc) throws DuplicateKeyException {
		log.trace("ENTER");
		Keyword key = ui.getKeyword(dialog);
		log.debug("New description [" + desc + "] for keyword [" + key.getKeyword() + "]");
		key.setDescription(desc);
		this.keywordDao.updateKeyword(key);
		this.removeDialog(dialog);
		this.showSelectedKeyword();
		log.trace("EXIT");
	}

	public void showSelectedKeyword() {
		Object selected = ui.getSelectedItem(keywordListComponent);
		
		Object divider = find(COMPONENT_KEYWORDS_DIVIDER);
		if (ui.getItems(divider).length >= 2) {
			ui.remove(ui.getItems(divider)[ui.getItems(divider).length - 1]);
		}
		
		// If selected is null, then we are here because a keyword has been unselected
		if (selected == null) {
			enableKeywordFields(ui.find(COMPONENT_KEY_PANEL));
			return;
		}
		
		//An existent keyword is selected, let's check if it is simple or advanced.
		Keyword keyword = ui.getAttachedObject(selected, Keyword.class);
		Collection<KeywordAction> actions = this.keywordActionDao.getActions(keyword);
		boolean simple = actions.size() <= 3;
		if (simple) {
			KeywordAction.Type previousType = null;
			for (KeywordAction action : actions) {
				KeywordAction.Type type = action.getType();
				if (type != KeywordAction.Type.REPLY
						&& type != KeywordAction.Type.JOIN
						&& type != KeywordAction.Type.LEAVE) {
					simple = false;
					break;
				}
				
				if (action.getEndDate() != DEFAULT_END_DATE) {
					simple = false;
					break;
				}
				
				if (type == previousType) {
					simple = false;
					break;
				}
				
				previousType = type;
			}
		}
		
		String keywordDescription = getDisplayableDescription(keyword);
		if (simple) {
			Object panel = ui.loadComponentFromFile(UI_FILE_KEYWORDS_SIMPLE_VIEW, this);
			ui.add(divider, panel);
			
			//Fill every field
			Object tfKeyword = ui.find(panel, COMPONENT_TF_KEYWORD);
			Object lbKeywordDescription = ui.find(panel, COMPONENT_LB_KEYWORD_DESCRIPTION);
			ui.setEnabled(tfKeyword, false);
			ui.setText(tfKeyword, getDisplayableKeyword(keyword));
			
			// We display the keyword description in the panel
			if (keywordDescription != null && keywordDescription.length() > 0) {
				ui.setText(lbKeywordDescription,keywordDescription);
			} else {
				ui.remove(lbKeywordDescription);
			}
			
			// We have to set the text in case there is no join/leave keyword actions
			setJoinGroupDisplay(null);
			setLeaveGroupDisplay(null);
			
			for (KeywordAction action : actions) {
				KeywordAction.Type type = action.getType();
				if (type == KeywordAction.Type.REPLY) {
					Object cbReply = ui.find(panel, COMPONENT_CB_AUTO_REPLY);
					Object tfReply = ui.find(panel, COMPONENT_TF_AUTO_REPLY);
					ui.setSelected(cbReply, true);
					ui.setText(tfReply, action.getUnformattedReplyText());
				} else if (type == KeywordAction.Type.JOIN) {
					setJoinGroupDisplay(action.getGroup());
				} else if (type == KeywordAction.Type.LEAVE) {
					setLeaveGroupDisplay(action.getGroup());
				}
			}
		} else {
			Object panel = ui.loadComponentFromFile(UI_FILE_KEYWORDS_ADVANCED_VIEW, this);
			Object table = ui.find(panel, COMPONENT_ACTION_LIST);
			Object lbKeywordDescription = ui.find(panel, COMPONENT_LB_KEYWORD_DESCRIPTION);
			
			// We display the keyword description in the panel
			if (keywordDescription != null && keywordDescription.length() > 0) {
				ui.setText(lbKeywordDescription,keywordDescription);
			} else {
				ui.remove(lbKeywordDescription);
			}
			ui.setText(panel, InternationalisationUtils.getI18nString(COMMON_KEYWORD_ACTIONS_OF, getDisplayableKeyword(keyword)));
			//Fill every field
			for (KeywordAction action : actions) {
				ui.add(table, ui.getRow(action));
			}
			ui.add(divider, panel);
			enableKeywordActionFields(table, ui.find(panel, COMPONENT_KEY_ACT_PANEL));
		}
		enableKeywordFields(ui.find(COMPONENT_KEY_PANEL));
	}
	/** Show group selecter for choosing the group to join. */
	public void selectJoinGroup() {
		selectingJoinGroup = true;
		GroupSelecterDialog groupSelect = new GroupSelecterDialog(ui, this);
		groupSelect.init(ui.getRootGroup());

		// TODO should select the current leave group in the dialog
		
		groupSelect.show();
	}
	public void removeJoinGroup() {
		setJoinGroupDisplay(null);
	}
	public void removeLeaveGroup() {
		setLeaveGroupDisplay(null);
	}
	/** Show group selecter for choosing the group to leave. */
	public void selectLeaveGroup() {
		selectingJoinGroup = false;
		GroupSelecterDialog groupSelect = new GroupSelecterDialog(ui, this);
		groupSelect.init(ui.getRootGroup());

		// TODO should select the current leave group in the dialog
		
		groupSelect.show();
	}
	
	public void removeDialog(Object dialog) {
		super.removeDialog(dialog);
		if(dialog == this.keywordForm) {
			keywordForm = null;
		}
	}
	
	public void keywordDescriptionChanged (String descriptionString) {
		if (descriptionString != null && descriptionString.length() > 0) {
			if (descriptionString.length() > FrontlineSMSConstants.KEYWORD_MAX_DESCRIPTION_LENGTH) {
				ui.setText(ui.find(keywordForm, COMPONENT_TA_KEYWORD_DESCRIPTION), descriptionString.substring(0, FrontlineSMSConstants.KEYWORD_MAX_DESCRIPTION_LENGTH));
			}
		}
		descriptionString = ui.getText(ui.find(keywordForm, COMPONENT_TA_KEYWORD_DESCRIPTION));
		ui.setText(ui.find(keywordForm, COMPONENT_LB_REMAINING_CHARS), String.valueOf(FrontlineSMSConstants.KEYWORD_MAX_DESCRIPTION_LENGTH - descriptionString.length()));
		ui.setVisible(ui.find(keywordForm, COMPONENT_LB_REMAINING_CHARS), true);
		ui.setVisible(ui.find(keywordForm, "lbTextRemainingChars"), true);			
	}

//> UI HELPER METHODS
	/** 
	 * In advanced mode, updates the list of keywords in the Keyword Manager.  
	 * <br>Has no effect in classic mode.
	 */
	private void updateKeywordList() {
		new FrontlineUiUpdateJob() {
			public void run() {
				keywordListPagingHandler.refresh();
				showSelectedKeyword();
				enableKeywordFields(ui.find(COMPONENT_KEY_PANEL));		
			}
		}.execute();
	}
	
	/**
	 * This method invokes the correct edit dialog according to the supplied action type.
	 * @param action The action to edit
	 */
	private void showActionEditDialog(KeywordAction action) {
		BaseActionDialog dialog;
		switch (action.getType()) {
			case FORWARD:
				dialog = new ForwardActionDialog(ui, this);
				break;
			case JOIN: 
				dialog = new JoinGroupActionDialog(ui, this);
				break;
			case LEAVE: 
				dialog = new JoinGroupActionDialog(ui, this);
				break;
			case REPLY:
				dialog = new ReplyActionDialog(ui, this);
				break;
			case EXTERNAL_CMD:
				dialog = new ExternalCommandActionDialog(ui, this);
				break;
			case EMAIL:
				dialog = new EmailActionDialog(ui, this);
				break;
			default: throw new IllegalStateException();
		}
		dialog.init(action);
		dialog.show();
	}
	
	/**
	 * Shows the keyword dialog for edit purpose.
	 * 
	 * @param keyword The object to be edited.
	 */
	private void showKeywordDialogForEdition(Keyword keyword) {
		String key = getDisplayableKeyword(keyword);
		String title = InternationalisationUtils.getI18nString(COMMON_EDITING_KEYWORD, key);
		keywordForm = ui.loadComponentFromFile(UI_FILE_NEW_KEYWORD_FORM, this);
		ui.setAttachedObject(keywordForm, keyword);
		ui.setText(ui.find(keywordForm, COMPONENT_NEW_KEYWORD_FORM_TITLE), title);
		// Pre-populate the textfields with currently-selected keyword attributes strings
		Object textField = ui.find(keywordForm, COMPONENT_NEW_KEYWORD_FORM_KEYWORD);
		Object textFieldDescription = ui.find(keywordForm, COMPONENT_NEW_KEYWORD_FORM_DESCRIPTION);
		ui.setText(textField, key);
		ui.setEnabled(textField, false);
		
		String displayedDescription = keyword.getDescription();
		if(displayedDescription == null) displayedDescription = "";
		ui.setText(textFieldDescription, displayedDescription);
		this.keywordDescriptionChanged(displayedDescription);
		
		String method = "finishKeywordEdition(newKeywordForm, newKeywordForm_description.text)";
		ui.setAction(ui.find(keywordForm, COMPONENT_NEW_KEYWORD_BUTTON_DONE), method, keywordForm, this);
		ui.add(keywordForm);
	}
	
	void updateKeywordActionList_(KeywordAction action, boolean isNew) {
		updateKeywordActionList(action, isNew);
	}
	
	private void updateKeywordActionList(KeywordAction action, boolean isNew) {
		Object selected = ui.getSelectedItem(keywordListComponent);
		Keyword keyword = ui.getAttachedObject(selected, Keyword.class);
		
		Object table = find(COMPONENT_ACTION_LIST);
		
		this.ui.removeAll(table);
		for (KeywordAction keywordAction : this.keywordActionDao.getActions(keyword)) {
			ui.add(table, ui.getRow(keywordAction));
		}
	}

	private void setJoinGroupDisplay(Group group) {
		Object joinGroupButton = find(COMPONENT_JOIN_GROUP_SELECT_LABEL);
		ui.setAttachedObject(joinGroupButton, group);

		ui.setVisible(find("btJoinGroupSelect"), group==null);
		ui.setVisible(find("btJoinGroupRemove"), group!=null);
		
		if(group != null) {
			ui.setText(joinGroupButton, group.getPath());
		} else {
			ui.setText(joinGroupButton, InternationalisationUtils.getI18nString(I18N_KEYWORD_ACTION_NO_GROUP, InternationalisationUtils.getI18nString(I18N_COMMON_NONE)));
		}
	}
	private void setLeaveGroupDisplay(Group group) {
		Object leaveGroupButton = find(COMPONENT_LEAVE_GROUP_SELECT_LABEL);
		ui.setAttachedObject(leaveGroupButton, group);

		ui.setVisible(find("btLeaveGroupSelect"), group==null);
		ui.setVisible(find("btLeaveGroupRemove"), group!=null);
		
		if(group != null) {
			ui.setText(leaveGroupButton, group.getPath());
		} else {
			ui.setText(leaveGroupButton, InternationalisationUtils.getI18nString(I18N_KEYWORD_ACTION_NO_GROUP, InternationalisationUtils.getI18nString(I18N_COMMON_NONE)));
		}
	}

	private String keywordSimple_getAutoReply(Object panel) {
		String ret = null;
		if (ui.isSelected(ui.find(panel, COMPONENT_CB_AUTO_REPLY))) {
			ret = ui.getText(ui.find(panel, COMPONENT_TF_AUTO_REPLY));
		}
		return ret;
	}
	
	private Group keywordSimple_getJoin() {
		Object joinGroupButton = find(COMPONENT_JOIN_GROUP_SELECT_LABEL);
		if(joinGroupButton != null) {
			return ui.getAttachedObject(joinGroupButton, Group.class);
		} else {
			return null;
		}
	}
	
	private Group keywordSimple_getLeave() {
		Object leaveGroupButton = find(COMPONENT_LEAVE_GROUP_SELECT_LABEL);
		if (leaveGroupButton != null) {
			return ui.getAttachedObject(leaveGroupButton, Group.class);
		} else {
			return null;
		}
	}
	
//> PAGING METHODS
	/** @see PagedComponentItemProvider#getListDetails(Object, int, int) */
	public PagedListDetails getListDetails(Object list, int startIndex, int limit) {
		int totalItemCount = this.keywordDao.getTotalKeywordCount();

		Object selectedItem = ui.getSelectedItem(list);
		Keyword selectedKeyword = ui.getAttachedObject(selectedItem, Keyword.class);
		
		List<Keyword> keywords = keywordDao.getAllKeywords(startIndex, limit);
		Object[] listItems = new Object[keywords.size()];
		
		for(int i=0; i<listItems.length; ++i) {
			Keyword keyword = keywords.get(i);
			listItems[i] = ui.createListItem(keyword);
			if(selectedKeyword != null && selectedKeyword.equals(keyword)) {
				selectedItem = listItems[i];
			}
		}

		return new PagedListDetails(totalItemCount, listItems, selectedItem);
	}
	
//> GROUP SELECTER METHODS
	public void groupSelectionCompleted(Group group) {
		// Make sure we don't try to use the root group here
		if(group.isRoot()) group = null;
		
		// We are either selecting the join group or the leave group
		if(selectingJoinGroup) {
			setJoinGroupDisplay(group);
		} else {
			setLeaveGroupDisplay(group);
		}
	}
	
	/**
	 * Gets a displayable string for a keyword, replacing the blank keyword with an internationalised
	 * string describing the blank keyword.
	 * @param keyword
	 * @return
	 */
	public static String getDisplayableKeyword(Keyword keyword) {
		String displayable = keyword.getKeyword();
		if (displayable.length() == 0) return "<" + InternationalisationUtils.getI18nString(COMMON_BLANK) + ">";
		else return displayable;		
	}
	
	/**
	 * Gets a displayable string for a keyword description, replacing the blank keyword with an internationalised
	 * string describing the blank keyword.
	 * @param keyword
	 * @return
	 */
	public static String getDisplayableDescription(Keyword keyword) {
		boolean hasDescription = (keyword.getDescription() != null && keyword.getDescription().length() > 0); 
		if (keyword.getKeyword().length() == 0 && !hasDescription) return InternationalisationUtils.getI18nString(FrontlineSMSConstants.MESSAGE_BLANK_KEYWORD_DESCRIPTION);
		else if (keyword.getKeyword().equals(FrontlineSMSConstants.MMS_KEYWORD)){
			return InternationalisationUtils.getI18nString(FrontlineSMSConstants.MESSAGE_MMS_KEYWORD_DESCRIPTION);
		} else {
			return keyword.getDescription();
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
			if (newTabName.equals(TAB_KEYWORD_MANAGER)) {
				threadSafeRefresh();
				ui.setStatus(InternationalisationUtils.getI18nString(MESSAGE_KEYWORDS_LOADED));
			}
		}
	}
}
