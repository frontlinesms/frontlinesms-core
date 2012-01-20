package net.frontlinesms.ui.handler.contacts;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.data.domain.Group;
import net.frontlinesms.data.repository.GroupDao;
import net.frontlinesms.ui.FrontlineUI;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

public class GroupSelecterDialog implements ThinletUiEventHandler, SingleGroupSelecterPanelOwner {
	private static final String XML_LAYOUT_GROUP_SELECTER_DIALOG = "/ui/core/contacts/dgGroupSelecter.xml";
	private FrontlineUI ui;
	private final GroupDao groupDao;
	private SingleGroupSelecterDialogOwner owner;
	private GroupSelecterPanel selecter;
	private Object dialogComponent;
	
	public GroupSelecterDialog(FrontlineUI ui, SingleGroupSelecterDialogOwner owner, GroupDao groupDao) {
		this.ui = ui;
		this.owner = owner;
		this.groupDao = groupDao;
	}
	
	/** Init with default title and no hidden groups */
	public void init(Group rootGroup) {
		init(InternationalisationUtils.getI18nString(FrontlineSMSConstants.COMMON_GROUP), rootGroup);
	}
	
	/** Init with default title */
	public void init(Group rootGroup, Collection<Group> hiddenGroups) {
		init(InternationalisationUtils.getI18nString(FrontlineSMSConstants.COMMON_GROUP), rootGroup, hiddenGroups);
	}
	
	/** Init with specific title and no hidden groups */
	public void init(String title, Group rootGroup) {
		Set<Group> noHiddenGroups = Collections.emptySet();
		this.init(title, rootGroup, noHiddenGroups);
	}
	
	public void init(String title, Group rootGroup, Collection<Group> hiddenGroups) {
		// TODO init
		dialogComponent = ui.loadComponentFromFile(XML_LAYOUT_GROUP_SELECTER_DIALOG, this);
		this.setTitle(title);
		
		this.selecter = new GroupSelecterPanel(ui, this, groupDao);
		selecter.init(rootGroup, hiddenGroups);
		selecter.refresh(false);
		
		Object selecterPanel = selecter.getPanelComponent();
		ui.setColspan(selecterPanel, 2);
		ui.setWeight(selecterPanel, 1, 1);
		ui.add(dialogComponent, selecterPanel, 0);
		
		selecter.setPerform(this, "done");
		
		// Disable the DONE button until the user has selected something
		setDoneButtonEnabled(false);
	}

	private void setTitle(String title) {
		ui.setText(this.dialogComponent, title);
	}
	
	public void show() {
		ui.add(this.dialogComponent);
	}

	public void groupSelectionChanged(Group selectedGroup) {
		// Once a group other than the root is selected, we want to allow the DONE button to be clicked
		// (if the selected group is not in the hidden groups list)
		boolean enableDoneButton = selectedGroup != null 
				&& !selectedGroup.isRoot()
				&& !this.selecter.isHidden(selectedGroup);
		setDoneButtonEnabled(enableDoneButton);
	}

//> UI EVENT METHODS
	public void done() {
		removeDialog();
		this.owner.groupSelectionCompleted(this.selecter.getSelectedGroup());
	}
	
	public void removeDialog() {
		ui.removeDialog(this.dialogComponent);
	}
	
//> UI HELPER METHODS
	/** Enable or disable the DONE button */
	private void setDoneButtonEnabled(boolean enabled) {
		ui.setEnabled(FrontlineUI.find(this.dialogComponent, "btGroupSelecterDone"), enabled);
	}
}
