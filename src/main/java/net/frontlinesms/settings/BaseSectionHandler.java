package net.frontlinesms.settings;

import java.util.HashMap;
import java.util.Map;

import net.frontlinesms.events.EventBus;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.settings.SettingsChangedEventNotification;
import net.frontlinesms.ui.settings.UiSettingsSectionHandler;

public abstract class BaseSectionHandler {
	protected EventBus eventBus;
	protected UiGeneratorController ui;
	protected Object panel;
	protected Map<String, Object> originalValues;

	protected BaseSectionHandler (UiGeneratorController ui) {
		this.ui = ui;
		if (this.ui instanceof UiGeneratorController) {
			this.eventBus = ((UiGeneratorController) ui).getFrontlineController().getEventBus();
		}
		
		this.originalValues = new HashMap<String, Object>();
	}
	
	protected void settingChanged(String key, Object newValue) {
		Object oldValue = this.originalValues.get(key);
		if (this.eventBus != null) {
			SettingsChangedEventNotification notification;
			if (newValue == null && oldValue == null || newValue.equals(oldValue)) {
				notification = new SettingsChangedEventNotification(key, true);
			} else {
				notification = new SettingsChangedEventNotification(key, false);
			}
			
			this.eventBus.notifyObservers(notification);
		}
	}
	
	public Object getPanel() {
		if (this.panel == null) {
			init();
		}
		return this.panel;
	}
	
	/**
	 * Override to load the panel
	 */
	abstract protected void init();
	
	public Object getSectionNode() {
		return null;
	}
	
	/**
	 * Helps create a Thinlet node for a section
	 * @param isRootNode
	 * @param title
	 * @param attachedObject
	 * @param iconPath
	 * @return
	 */
	protected Object createSectionNode(String title, UiSettingsSectionHandler attachedObject, String iconPath) {
		Object sectionRootNode = ui.createNode(title, attachedObject);
		
		// Try to get an icon from the classpath
		this.ui.setIcon(sectionRootNode, iconPath);
		
		return sectionRootNode;
	}
	
	protected Object find (String component) {
		return this.ui.find(this.panel, component);
	}
}
