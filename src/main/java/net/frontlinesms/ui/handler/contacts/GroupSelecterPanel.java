/**
 * 
 */
package net.frontlinesms.ui.handler.contacts;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.apache.log4j.Logger;

import thinlet.Thinlet;

import net.frontlinesms.FrontlineSMS;
import net.frontlinesms.data.domain.Group;
import net.frontlinesms.data.repository.GroupDao;
import net.frontlinesms.data.repository.unmodifiable.UnmodifiableGroupDao;
import net.frontlinesms.ui.FrontlineUI;
import net.frontlinesms.ui.Icon;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.handler.BasePanelHandler;

/**
 * @author aga
 */
public class GroupSelecterPanel extends BasePanelHandler {
	private Logger LOG = Logger.getLogger(this.getClass());

	private static final String XML_LAYOUT_GROUP_PANEL = "/ui/core/contacts/pnGroupSelecter.xml";
	private static final String COMPONENT_GROUP_TREE = "trGroups";
	
	/** Thinlet UI component: the group tree */
	private Object groupTreeComponent;
	
	private GroupSelecterPanelOwner owner;

	private final GroupDao normalGroupDao;
	private UnmodifiableGroupDao groupDao;
	
	private boolean allowMultipleSelections;

	private Group rootGroup;
	/** A list of groups which should be disabled/hidden */
	private final HashSet<Group> hiddenGroups = new HashSet<Group>();

//> CONSTRUCTORS
	public GroupSelecterPanel(FrontlineUI ui, GroupSelecterPanelOwner owner, GroupDao groupDao) {
		super(ui);
		this.owner = owner;
		this.normalGroupDao = groupDao;
		
		allowMultipleSelections = owner instanceof MultiGroupSelecterPanelOwner;
	}

	/** Initialise the selecter. */
	public void init(Group rootGroup) {
		Collection<Group> noHiddenGroups = Collections.emptySet();
		init(rootGroup, noHiddenGroups );
	}

	/** Initialise the selecter. */
	public void init(Group rootGroup, Collection<Group> hiddenGroups) {
		super.loadPanel(XML_LAYOUT_GROUP_PANEL);
		
		// TODO update selection of group tree appropriate to allowMultipleSelections

		// Cache the group tree
		groupTreeComponent = super.find(COMPONENT_GROUP_TREE);
		
		// add nodes for group tree
		this.setRootGroup(rootGroup);
		
		this.setHiddenGroups(hiddenGroups);
	}

	/**
	 * Refresh the list of groups
	 * This method will reload the view of groups.
	 */
	public void refresh(boolean showRoot) {
		Object groupTree = getGroupTreeComponent();
		
		this.groupDao = new UnmodifiableGroupDao(normalGroupDao);
		
		ui.removeAll(groupTree);
		Object node = createNode(getRootGroup(), showRoot);
		// If we want to show the root group, we add it to the tree component
		// Otherwise, the main nodes are added to the tree component in the createNode() function 
		if (showRoot)
			ui.add(groupTree, node);
	}
	
	/**
	 * Sets a list of groups which should be disabled/hidden
	 * @param hiddenGroups
	 */
	public void setHiddenGroups(Collection<Group> hiddenGroups) {
		this.hiddenGroups.clear();
		this.hiddenGroups.addAll(hiddenGroups);
	}
	
	public boolean isHidden(Group group) {
		return this.hiddenGroups.contains(group);
	}
	
//> ACCESSORS
	/** @return a single group selected in the tree, or <code>null</code> if none is selected */
	public Group getSelectedGroup() {
		assert(!this.allowMultipleSelections) : "Cannot get a single selection if multiple groups are selectable.";
		return ui.getAttachedObject(ui.getSelectedItem(this.getGroupTreeComponent()), Group.class);
	}
	
	/** @return groups selected in the tree */
	public Collection<Group> getSelectedGroups() {
		assert(this.allowMultipleSelections) : "Cannot get multiple groups if multiple selection is not enabled.";
		Object[] selectedItems = ui.getSelectedItems(this.getGroupTreeComponent());
		HashSet<Group> groups = new HashSet<Group>();
		for(Object selected : selectedItems) {
			groups.add(ui.getAttachedObject(selected, Group.class));
		}
		return groups;
	}


	/** @return Recursively search for the group in the tree */
	public void selectGroup(Group g) {
		if (g == null) return;
		
		this.ui.setSelectedItem(getGroupTreeComponent(), getNodeForGroup(getGroupTreeComponent(), g));
		this.selectionChanged();
	}
	
	
	/** FIXME this is only here for debug purposes. */
	@Override
	public Object getPanelComponent() {
		return super.getPanelComponent();
	}
	
	/** Adds a new group to the tree */
	public void addGroup(Group group) {
		Object groupTree = this.getGroupTreeComponent();
		Group parent = group.getParent();
		Object parentGroupNode;
		if(parent != null && !parent.isRoot()) {
			parentGroupNode = getNodeForGroup(groupTree, parent);
		} else {
			// There is no parent for the group, so we need to get the node for the root group
			parentGroupNode = ui.getItem(groupTree, 0);
		}
		ui.add(parentGroupNode, createNode(group, true));
	}
	
//> UI EVENT METHODS
	public void selectionChanged() {
		if(owner instanceof SingleGroupSelecterPanelOwner) {
			((SingleGroupSelecterPanelOwner) owner).groupSelectionChanged(getSelectedGroup());
		} else {
			((MultiGroupSelecterPanelOwner) owner).groupSelectionChanged(getSelectedGroups());
		}
	}

//> UI HELPER METHODS
	/**
	 * Gets the node we are currently displaying for a group.
	 * @param component
	 * @param group
	 * @return
	 */
	private Object getNodeForGroup(Object component, Group group) {
		assert(group!=null) : "Cannot get a node for a null group.";
		Object ret = null;
		for (Object o : this.ui.getItems(component)) {
			Group g = ui.getAttachedObject(o, Group.class);
			if (g.equals(group)) {
				ret = o;
				break;
			} else {
				ret = getNodeForGroup(o, group);
				if (ret != null) break;
			}
		}
		return ret;
	}
	
	/** @return the group tree TODO cache this - it's not going to change, and we use it a lot */
	public Object getGroupTreeComponent() {
		return this.groupTreeComponent;
	}
	
	/**
	 * Creates a node for the supplied group, creating nodes for its sub-groups and contacts as well.
	 * @param group The group to be put into a node.
	 * @return a Thinlet tree node for displaying a group
	 */
	private Object createNode(Group group, boolean showRootGroup) {
		LOG.trace("ENTER");
		
		LOG.debug("Group [" + group.getName() + "]");
		
		String toSet = group.getName();
		
		Object node;
		
		if (group.equals(getRootGroup()) && !showRootGroup) {
			node = groupTreeComponent;
		} else {
			node = ui.createNode(toSet, group);
			
			if(ui.getBoolean(node, Thinlet.EXPANDED)) {
				ui.setIcon(node, Icon.FOLDER_OPEN);
			} else {
				ui.setIcon(node, Icon.FOLDER_CLOSED);
			}
		}
		
		// Add subgroup components to this node
		for (Group subGroup : groupDao.getChildGroups(group)) {
			Object groupNode = createNode(subGroup, showRootGroup);
			if (groupNode != null) {
				ui.add(node, groupNode);
				if (hiddenGroups != null && hiddenGroups.contains(subGroup)) {
					ui.setEnabled(groupNode, false);
				}
			}
		}
		LOG.trace("EXIT");
		return node;
	}

	public void setRootGroup(Group rootGroup) {
		this.rootGroup = rootGroup;
	}

	public void setPerform(ThinletUiEventHandler groupSelecterDialog, String methodCall) {
		ui.setPerform(this.groupTreeComponent, methodCall, this.groupTreeComponent, groupSelecterDialog);
	}

	public Group getRootGroup() {
		return rootGroup;
	}
}
