/**
 * 
 */
package net.frontlinesms.ui;

import java.awt.Font;
import java.awt.Image;
import java.awt.Rectangle;

import thinlet.Thinlet;
import thinlet.ThinletText;

/**
 * Extension of Thinlet which adds accessors for properties and factory methods for creating components.
 * @author Alex
 */
@SuppressWarnings("serial")
public class ExtendedThinlet extends Thinlet {
	
private static final String END = "end";
private static final String START = "start";

//> STATIC CONSTANTS

//> INSTANCE PROPERTIES

//> CONSTRUCTORS

//> ACCESSORS

//> INSTANCE HELPER METHODS
	/**
	 * Recursivelty sets a boolean attribute on a UI component and all its sub-components.
	 * @param parent
	 * @param value
	 */
	public final void setEnabledRecursively(Object parent, boolean value) {
		setEnabled(parent, value);
		for(Object component : getItems(parent)) {
			if(!getClass(parent).equals(TABLE)) setEnabledRecursively(component, value);
		}
	}
	
	/**
	 * Recursively sets a boolean attribute on a UI component and all its sub-components.
	 * @param parent
	 * @param value
	 */
	public final void setEditableRecursively(Object parent, boolean value) {
		setEditable(parent, value);
		for(Object component : getItems(parent)) {
			if(!getClass(parent).equals(TABLE)) setEditableRecursively(component, value);
		}
	}
	
	/**
	 * Repaint the given component's area later
	 * @param component
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public void repaint(Object component, int x, int y, int width, int height) {
		while ((component = getParent(component)) != null) {
			Rectangle bounds = this.getRectangle(component, BOUNDS);
			x += bounds.x; y += bounds.y;
			Rectangle view = this.getRectangle(component, ":view");
			if (view != null) {
				Rectangle port = this.getRectangle(component, ":port");
				x += -view.x + port.x; y += -view.y + port.y; //+ clip :port
			}
		}
		repaint(x, y, width, height);
	}
	
	protected Rectangle getRectangle(Object component, String key) {
		return (Rectangle) get(component, key);
	}
	
	public Rectangle getBounds(Object component){
		return this.getRectangle(component, BOUNDS);
	}
	
	/**
	 * @param component
	 * @return The current position on an editable component
	 */
	public final int getCaretPosition(Object component) {
		return getInteger(component, END);
	}
	
	public final void setCaretPosition(Object component, int caretPosition) {
		setInteger(component, START, caretPosition);
		setInteger(component, END, caretPosition);
	}
	
	/**
	 *Sets the Editable property of a component
	 * @param component
	 * @param value
	 */
	public void setEditable(Object component, boolean value) {
		super.setBoolean(component, "editable", value);
	}

//> COMPONENT ACCESSORS
	/**
	 * Sets the thinlet name of the supplied thinlet component.
	 * @param component
	 * @param name
	 */
	public final void setName(Object component, String name) {
		setString(component, NAME, name);
	}
	
	/**
	 * Attaches an object to a component.
	 * @param component
	 * @param attachment
	 */
	public final void setAttachedObject(Object component, Object attachment) {
		putProperty(component, PROPERTY_ATTACHED_OBJECT, attachment);
	}
	
	/**
	 * Retrieves the attached object from this component.
	 * @param component
	 * @return the object attached to the component, as set by {@link #setAttachedObject(Object, Object)}
	 */
	public final Object getAttachedObject(Object component) {
		return getProperty(component, PROPERTY_ATTACHED_OBJECT);
	}
	
	/**
	 * Retrieves an attached object from a component with the specified class.
	 * @param <T>
	 * @param component
	 * @param clazz
	 * @return An object of the requested class.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Object> T getAttachedObject(Object component, Class<T> clazz) {
		return (T) getAttachedObject(component);
	}

	/**
	 * Disables a UI component and all descended components.
	 * @param component
	 */
	public final void deactivate(Object component) {
		setEnabledRecursively(component, false);
	}
	
	/**
	 * Enables a UI component and all descended components.
	 * @param component
	 */
	public final void activate(Object component) {
		setEnabledRecursively(component, true);
	}
	
	/**
	 * Sets the visibility of a component.
	 * @param component
	 * @param visible
	 */
	public void setVisible(Object component, boolean visible) {
		setBoolean(component, VISIBLE, visible);
	}

	/**
	 * Checks if a component is selected.
	 * @param component
	 * @return <code>true</code> if the component is selected
	 */
	public boolean isSelected(Object component) {
		return getBoolean(component, SELECTED);
	}

	/**
	 * Checks if a component is enabled.
	 * @param component
	 * @return <code>true</code> if the component is enabled
	 */
	public boolean isEnabled(Object component) {
		return getBoolean(component, ENABLED);
	}
	
	/**
	 * Sets the number of columns of a thinlet component.
	 * @param component
	 * @param columns
	 */
	public void setColumns(Object component, int columns) {
		setInteger(component, ATTRIBUTE_COLUMNS, columns);
	}
	
	/**
	 * Sets the number of rows of a thinlet component.
	 * @param component
	 * @param columns
	 */
	public void setRows(Object component, int rows) {
		setInteger(component, ROWS, rows);
	}

	/**
	 * Sets the colspan attribute of a thinlet component.
	 * @param component
	 * @param colspan number of columns for component to span
	 */
	public void setColspan(Object component, int colspan) {
		setInteger(component, ATTRIBUTE_COLSPAN, colspan);
	}

	/**
	 * Sets the width of a thinlet component.
	 * @param component
	 * @param width
	 */
	public void setWidth(Object component, int width) {
		setInteger(component, Thinlet.ATTRIBUTE_WIDTH, width);
	}
	
	public int getHeight(Object component) {
		return getInteger(component, Thinlet.ATTRIBUTE_HEIGHT);
	}
	
	/**
	 * Sets the height of a thinlet component.
	 * @param component
	 * @param height
	 */
	public void setHeight(Object component, int height) {
		setInteger(component, Thinlet.ATTRIBUTE_HEIGHT, height);
	}

	/**
	 * Sets the gap attribute of a thinlet component.
	 * @param component
	 * @param gap gap to add around this component
	 */
	public void setGap(Object component, int gap) {
		setInteger(component, Thinlet.GAP, gap);
	}

	/**
	 * Sets the gap attribute of a thinlet component.
	 * @param component
	 * @param weightX horizontal weigth to set for this component
	 * @param weightY vertical weigth to set for this component
	 */
	public void setWeight(Object component, int weightX, int weightY) {
		setInteger(component, Thinlet.ATTRIBUTE_WEIGHT_X, weightX);
		setInteger(component, Thinlet.ATTRIBUTE_WEIGHT_Y, weightY);
	}
	
	/**
	 * Sets the display of a tree node, expanded or not.
	 * @param node
	 * @param expanded <code>true</code> if the node should be expanded, <code>false</code> otherwise.
	 */
	public void setExpanded(Object node, boolean expanded) {
		setBoolean(node, EXPANDED, expanded);
	}
	
	/**
	 * Sets the icon of a component
	 * @param component
	 * @param icon
	 */
	public void setIcon(Object component, Image icon) {
		setIcon(component, ICON, icon);
	}
	
	/**
	 * Sets the icon of a component from a specified path
	 * @param component
	 * @param iconPath
	 */
	public void setIcon(Object component, String iconPath) {
		setIcon(component, ICON, getIcon(iconPath));
	}
	
	public void setBold(Object component) {
		setFont(component, FONT, super.getFont().deriveFont(Font.BOLD));
	}
	
	/**
	 * Sets whether a component has a border or not
	 * @param component the component to add/remove border from
	 * @param hasBorder <code>true</code> to add border, <code>false</code> to remove
	 */
	public void setBorder(Object component, boolean hasBorder) {
		setBoolean(component, Thinlet.BORDER, hasBorder);
	}

	/**
	 * Set the ACTION of a component
	 * @param component
	 * @param methodCall
	 * @param root
	 * @param handler
	 */
	public void setAction(Object component, String methodCall, Object root, Object handler) {
		setMethod(component, Thinlet.ATTRIBUTE_ACTION, methodCall, root, handler);
	}

	/**
	 * Set the PERFORM method of a component
	 * @param component
	 * @param methodCall
	 * @param root
	 * @param handler
	 */
	public void setPerform(Object component, String methodCall, Object root, ThinletUiEventHandler handler) {
		setMethod(component, Thinlet.PERFORM, methodCall, root, handler);
	}
	
	/**
	 * Set the DELETE method of a component
	 * @param component
	 * @param methodCall
	 * @param root
	 * @param handler
	 */
	public void setDeleteAction(Object component, String methodCall, Object root, Object handler) {
		setMethod(component, "delete", methodCall, root, handler);
	}
	
	/**
	 * Set the INSERT method of a component
	 * @param component
	 * @param methodCall
	 * @param root
	 * @param handler
	 */
	public void setInsert(Object component, String methodCall, Object root, Object handler) {
		setMethod(component, Thinlet.INSERT, methodCall, root, handler);
	}
	
	/**
	 * Set the REMOVE method of a component
	 * @param component
	 * @param methodCall
	 * @param root
	 * @param handler
	 */
	public void setRemove(Object component, String methodCall, Object root, Object handler) {
		setMethod(component, Thinlet.REMOVE, methodCall, root, handler);
	}
	
	/**
	 * Sets the TOOLTIP of a component
	 * @param component
	 * @param tooltip
	 */
	public void setTooltip (Object component, String tooltip) {
		setString(component, Thinlet.TOOLTIP, tooltip);
	}
	
	/**
	 * Set the CLOSE action of a component
	 * @param component
	 * @param methodCall
	 * @param root
	 * @param handler
	 */
	public void setCloseAction(Object component, String methodCall, Object root, Object handler) {
		setMethod(component, Thinlet.CLOSE, methodCall, root, handler);
	}
	
	/**
	 * Invoke the ACTION method on a component.
	 * @param component The component whose ACTION should be invoked
	 */
	public final void invokeAction(Object component) {
		invoke(component, null, Thinlet.ATTRIBUTE_ACTION);
	}
	
	/**
	 * Sets the horizontal alignment of a component.
	 * @param component The component whose horizontal align will be set.
	 * @param align The alignment value, e.g. {@link ThinletText#LEFT}
	 */
	public void setHAlign(Object component, String align) {
		super.setChoice(component, Thinlet.ATTRIBUTE_HALIGN, align);
	}

	/**
	 * Sets the vertical alignment of a component.
	 * @param component The component whose horizontal align will be set.
	 * @param align The alignment value, e.g. {@link ThinletText#LEFT}
	 */
	public void setVAlign(Object component, String align) {
		super.setChoice(component, Thinlet.ATTRIBUTE_VALIGN, align);
	}
	
//> COMPONENT FACTORY METHODS
	/**
	 * Creates a Thinlet UI Component of type NODE, sets the component's TEXT
	 * attribute to the supplied text and attaches the supplied OBJECT. 
	 * @param text
	 * @param attachedObject
	 * @return a tree node with an object attached to it
	 */
	public final Object createNode(String text, Object attachedObject) {
		Object node = Thinlet.create(NODE);
		setString(node, TEXT, text);
		setAttachedObject(node, attachedObject);
		return node;
	}
	
	/**
	 * Create a Thinlet UI Component of type table row.
	 * @return a table row with an object attached
	 */
	public final Object createTableHeader() {
		Object header = Thinlet.create(HEADER);
		return header;
    }
	
	/**
	 * Create a Thinlet UI Component of type table row, and attaches the
	 * supplied object to it.
	 * @param attachedObject
	 * @return a table row with an object attached
	 */
	public final Object createTableHeader(Object attachedObject) {
		Object header = createTableHeader();
		setAttachedObject(header, attachedObject);
		return header;
    }

	/**
	 * Create a Thinlet UI Component of type table row.
	 * @param attachedObject
	 * @return a table row with an object attached
	 */
	public final Object createTableRow() {
    	Object row = Thinlet.create(ROW);
    	return row;
    }

	/**
	 * Create a Thinlet UI Component of type table row, and attaches the
	 * supplied object to it.
	 * @param attachedObject
	 * @return a table row with an object attached
	 */
	public final Object createTableRow(Object attachedObject) {
    	Object row = createTableRow();
    	setAttachedObject(row, attachedObject);
    	return row;
    }
	
	/**
	 * Create a Thinlet UI component of type table cell containing the supplied number.
	 * @param integerContent
	 * @return a table cell
	 */
	public final Object createTableCell(int integerContent) {
    	return createTableCell(Integer.toString(integerContent));
    }
        
	/**
	 * Creates a UI table cell component containing the specified text. 
	 * @param text
	 * @return a table cell
	 */
	public final Object createTableCell(String text) {
		Object cell = Thinlet.create(CELL);
		setString(cell, TEXT, text);
		return cell;
	}
	
	/**
	 * Creates a UI table cell component containing the specified text. 
	 * @param text
	 * @return a table cell
	 */
	public final Object createTableCell(String text, boolean bold) {
		Object cell = Thinlet.create(CELL);
		setString(cell, TEXT, text);
		if (bold) {
			Font boldFont = super.getFont().deriveFont(Font.BOLD);
			setFont(cell, boldFont);
		}
		return cell;
	}
        
	/**
	 * Creates a Thinlet UI component of type table cell in the table row provided.
	 * The cell text is also set. 
	 * @param row 
	 * @param text
	 * @return The cell I created.
	 */
	protected final Object createTableCell(Object row, String text) {
		Object cell = Thinlet.create(CELL);
		setString(cell, TEXT, text);
		add(row, cell);
		return cell;
	}
	
	/**
	 * Creates a Thinlet UI Component of type LIST ITEM, set's the component's
	 * TEXT attribute to the supplied text and attaches the supplied OBJECT.
	 * @param text
	 * @param attachedObject
	 * @return a list item with the supplied attachment 
	 */
	public final Object createListItem(String text, Object attachedObject) {
		Object item = Thinlet.create(ITEM);
		setString(item, TEXT, text);
		setAttachedObject(item, attachedObject);
		return item;
	}
	
	/**
	 * Creates a Thinlet UI Component of type LIST ITEM, set's the component's
	 * TEXT attribute to the supplied text and attaches the supplied OBJECT.
	 * @param text
	 * @param attachedObject
	 * @return a list item with the supplied attachment 
	 */
	public final Object createListItem(String text, Object attachedObject, boolean bold) {
		Object item = Thinlet.create(ITEM);
		setString(item, TEXT, text);
		setAttachedObject(item, attachedObject);
		
		if (bold) {
			Font boldFont = super.getFont().deriveFont(Font.BOLD);
			setFont(item, boldFont);
		}
		
		return item;
	}
	
	/**
	 * Creates a Thinlet UI Component of type COMBOBOX CHOICE, set's the component's
	 * TEXT attribute to the supplied text and attaches the supplied OBJECT.
	 * @param text
	 * @param attachedObject
	 * @return
	 */
	public final Object createCombobox(String name, String text) {
		Object item = Thinlet.create(COMBOBOX);
		setString(item, TEXT, text);
		setName(item, name);
		return item;
	}
	
	/**
	 * Creates a Thinlet UI Component of type COMBOBOX CHOICE, set's the component's
	 * TEXT attribute to the supplied text and attaches the supplied OBJECT.
	 * @param text
	 * @param attachedObject
	 * @return
	 */
	public final Object createComboboxChoice(String text, Object attachedObject) {
		Object item = Thinlet.create(CHOICE);
		setString(item, TEXT, text);
		setAttachedObject(item, attachedObject);
		return item;
	}
	
	/**
	 * Creates a Thinlet UI Component of type COMBOBOX CHOICE, set's the component's
	 * TEXT attribute to the supplied text and attaches the supplied OBJECT.
	 * @param text
	 * @param attachedObject
	 * @return
	 */
	public final Object createColumn(String text, Object attachedObject) {
		Object item = Thinlet.create(COLUMN);
		setString(item, TEXT, text);
		setAttachedObject(item, attachedObject);
		return item;
	}
	
	/**
	 * Create a modal, closeable and resizeable dialog!
	 * @param title
	 * @return
	 */
	public Object createDialog(String title) {
		Object dialog = Thinlet.create(DIALOG);
		setString(dialog, TEXT, title);
		setBoolean(dialog, MODAL, true);
		setBoolean(dialog, CLOSABLE, true);
		setBoolean(dialog, "resizable", true);
		return dialog;
	}
	
	/**
	 * Create's a Thinlet UI Component of type BUTTON and set's the button's
	 * action and text label.
	 * @param text
	 * @param action
	 * @param root
	 * @return
	 */
	public final Object createButton(String text, String action, Object root) {
		Object button = Thinlet.create(BUTTON);
		setString(button, TEXT, text);
		setMethod(button, ATTRIBUTE_ACTION, action, root, this);
		return button;
	}
	
	/**
	 * Create's a Thinlet UI Component of type BUTTON and set's the button's
	 * action and text label.
	 * @param text
	 * @param action
	 * @param root
	 * @return
	 */
	public final Object createButton(String text, String action, Object root, ThinletUiEventHandler handler) {
		Object button = Thinlet.create(BUTTON);
		setString(button, TEXT, text);
		setMethod(button, ATTRIBUTE_ACTION, action, root, handler);
		return button;
	}
	
	/**
	 * Create's a Thinlet UI Component of type BUTTON with type LINK.  The button's
	 * action and text label are also set.
	 * @param text
	 * @param action
	 * @param root
	 * @return
	 */
	public final Object createLink(String text, String action, Object root) {
		Object button = createButton(text, action, root);
		setChoice(button, "type", "link");
		return button;
	}
	
	/**
	 * Create's a Thinlet UI Component of type BUTTON with type LINK.  The button's
	 * action and text label are also set.
	 * @param text
	 * @param action
	 * @param root
	 * @return
	 */
	public final Object createLink(String text, String action, Object root, ThinletUiEventHandler handler) {
		Object button = createButton(text, action, root, handler);
		setChoice(button, "type", "link");
		return button;
	}
	
	/**
	 * Create's a Thinlet UI Component of type BUTTON and set's the button's
	 * action and text label.
	 * TODO how often is this used?
	 * @param text
	 * @return
	 */
	public final Object createButton(String text) {
		Object button = Thinlet.create(BUTTON);
		setString(button, TEXT, text);
		return button;
	}
	
	/**
	 * Create's a Thinlet UI Component of type LABEL with the supplied TEXT.
	 * @param text The text displayed for this label.
	 * @return
	 */
	public final Object createLabel(String text) {
		Object label = create(LABEL);
		setString(label, TEXT, text);
		return label;
	}
	
	/**
	 * Create's a Thinlet UI Component of type LABEL with the supplied TEXT.
	 * @param text The text displayed for this label.
	 * @return
	 */
	public final Object createLabel(String text, String icon) {
		Object label = create(LABEL);
		setString(label, TEXT, text);
		if (icon != null) {
			setIcon(label, icon);
		}
		return label;
	}
	
	/**
	 * Creates a thinlet Checkbox UI component.
	 * @param text
	 * @param checked
	 * @return
	 */
	public final Object createCheckbox(String name, String text, boolean checked) {
		Object item = create(WIDGET_CHECKBOX);
		setText(item, text);
		setName(item, name);
		setBoolean(item, SELECTED, checked);
		return item;
	}
	
	/**
	 * Creates a thinlet Radio Button UI component.
	 * @param text
	 * @param checked
	 * @return
	 */
	public final Object createRadioButton(String name, String text, String group, boolean selected) {
		Object item = create(WIDGET_CHECKBOX);
		setText(item, text);
		setString(item, GROUP, group);
		setName(item, name);
		setBoolean(item, SELECTED, selected);
		return item;
	}
	
	/**
	 * Creates a thinlet Panel UI component.
	 * @param text
	 * @param checked
	 * @return
	 */
	public final Object createPanel(String name) {
		Object item = Thinlet.create(PANEL);
		setName(item, name);
		return item;
	}
	
	/**
	 * Creates a textfield with the supplied object name and initial text.
	 * @param name
	 * @param initialText
	 * @return a Thinlet textfield component
	 */
	public final Object createTextfield(String name, String initialText) {
		Object item = Thinlet.create(TEXTFIELD);
		setText(item, initialText);
		setName(item, name);
		return item;
	}
	
	/**
	 * Creates a textfield with the supplied object name and initial text.
	 * @param name
	 * @param initialText
	 * @return a Thinlet textfield component
	 */
	public final Object createTextarea(String name, String initialText, int rows) {
		Object item = Thinlet.create(TEXTAREA);
		setText(item, initialText);
		setName(item, name);
		setRows(item, rows);
		return item;
	}
	
	/**
	 * Creates a passwordfield with the supplied object name and initial text.
	 * @param name
	 * @param initialText
	 * @return A Thinlet password entry component
	 */
	public final Object createPasswordfield(String name, String initialText) {
		Object item = Thinlet.create(PASSWORDFIELD);
		setText(item, initialText);
		setName(item, name);
		return item;
	}
	
	/**
	 * @param name
	 * @return A Thinlet Popup Menu
	 */
	public final Object createPopupMenu(String name) {
		Object popupMenu = Thinlet.create(POPUPMENU);
		setName(popupMenu, name);
		
		return popupMenu;
	}
	
	/**
	 * @param iconPath The path to the icon for this menu item
	 * @param text The text for the menuitem
	 * @return A Thinlet menuitem
	 */
	public final Object createMenuitem(String iconPath, String text) {
		Object item = Thinlet.create(MENUITEM);
		setIcon(item, iconPath);
		setText(item, text);
		return item;
	}
	
	/**
	 * @param iconPath The path to the icon for this menu item
	 * @param text The text for the menuitem
	 * @param checked <code>true</code> if the checkbox is ticked, <code>false</code> otherwise.
	 * @return A Thinlet menuitem
	 */
	public final Object createCheckboxMenuitem(String iconPath, String text, boolean checked) {
		Object item = Thinlet.create(CHECKBOXMENUITEM);
		setIcon(item, iconPath);
		setSelected(item, checked);
		setText(item, text);
		return item;
	}

//> STATIC FACTORIES

//> STATIC HELPER METHODS
}
