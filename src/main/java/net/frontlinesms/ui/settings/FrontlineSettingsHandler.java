package net.frontlinesms.ui.settings;

import java.util.ArrayList;
import java.util.List;

import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.domain.PersistableSettings;
import net.frontlinesms.events.EventBus;
import net.frontlinesms.events.EventObserver;
import net.frontlinesms.events.FrontlineEventNotification;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;
import net.frontlinesms.plugins.PluginController;
import net.frontlinesms.plugins.PluginProperties;
import net.frontlinesms.plugins.PluginSettingsController;
import net.frontlinesms.settings.FrontlineValidationMessage;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiDestroyEvent;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.handler.settings.SettingsAppearanceSectionHandler;
import net.frontlinesms.ui.handler.settings.SettingsGeneralSectionHandler;
import net.frontlinesms.ui.handler.settings.SettingsServicesSectionHandler;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

import org.apache.log4j.Logger;

/**
 * Ui Handler for {@link FrontlineSettingsHandler} settings.
 * The whole settings dialog system is handled by this class.
 * 
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public class FrontlineSettingsHandler implements ThinletUiEventHandler, EventObserver {
//> CONSTANTS
	/** Path to XML for UI layout for settings screen, {@link #settingsDialog} */
	private static final String UI_SETTINGS = "/ui/core/settings/dgFrontlineSettings.xml";
	
	/** Logging object */
	private static final Logger LOG = FrontlineUtils.getLogger(FrontlineSettingsHandler.class);

	private static final String UI_COMPONENT_CORE_TREE = "generalTree";
	private static final String UI_COMPONENT_PLUGIN_TREE = "pluginTree";
	private static final String UI_COMPONENT_PN_DISPLAY_SETTINGS = "pnDisplaySettings";
	private static final String UI_COMPONENT_BT_SAVE = "btSave";
	private static final String UI_COMPONENT_BT_CLOSE = "btClose";

	private static final String I18N_MESSAGE_CONFIRM_CLOSE_SETTINGS = "message.confirm.close.settings";
	private static final String I18N_SETTINGS_SAVED = "settings.saved";
	private static final String I18N_TOOLTIP_SETTINGS_BTSAVE_DISABLED = "tooltip.settings.btsave.disabled";
	private static final String I18N_TOOLTIP_SETTINGS_SAVES_ALL = "tooltip.settings.saves.all";


//> INSTANCE PROPERTIES
	/** Thinlet instance that owns this handler */
	private final UiGeneratorController uiController;
	/** dialog for editing {@link SmsInternetService} settings, {@link PersistableSettings} instances */
	private Object settingsDialog;

	private EventBus eventBus;
	
	private List<UiSettingsSectionHandler> handlersList;
	
	private List<String> changesList;

//> CONSTRUCTORS
	/**
	 * Creates a new instance of this UI.
	 * @param controller thinlet controller that owns this {@link FrontlineSettingsHandler}.
	 */
	public FrontlineSettingsHandler(UiGeneratorController controller) {
		this.uiController = controller;
		this.eventBus = controller.getFrontlineController().getEventBus();
		this.handlersList = new ArrayList<UiSettingsSectionHandler>();
		this.changesList = new ArrayList<String>();

		this.init();
	}

	/**
	 * Shows the general confirmation dialog (for removal). 
	 * @param methodToBeCalled the method to be called if the confirmation is affirmative
	 */
	public void showConfirmationDialog(String methodToBeCalled){
		uiController.showConfirmationDialog(methodToBeCalled, this);
	}

	private void init() {
		LOG.trace("Initializing Frontline Settings");
		this.eventBus.registerObserver(this);
		settingsDialog = uiController.loadComponentFromFile(UI_SETTINGS, this);
		
		this.loadCoreSettings();
		this.loadPluginSettings();
	}

	private void loadCoreSettings() {
		Object coreTree = find(UI_COMPONENT_CORE_TREE);

		/** APPEARANCE **/
		this.loadCoreSection(coreTree, new SettingsAppearanceSectionHandler(this.uiController));
		
		/** GENERAL **/
		this.loadCoreSection(coreTree, new SettingsGeneralSectionHandler(this.uiController));
		
		/** SERVICES **/
		this.loadCoreSection(coreTree, new SettingsServicesSectionHandler(this.uiController));
	}
	
	/**
	 * Loads a section for the core settings.
	 * @param coreTree
	 * @param handler
	 */
	private void loadCoreSection(Object coreTree, UiSettingsSectionHandler handler) {
		Object rootNode = handler.getSectionNode();
		this.uiController.add(coreTree, rootNode);
		
		if (handler instanceof SettingsAppearanceSectionHandler) {
			this.uiController.setSelectedItem(coreTree, rootNode);
			this.selectionChanged(coreTree);
		}
	}

	/**
	 * Loads the different plugins into the plugins tree
	 */
	private void loadPluginSettings() {
		for(Class<? extends PluginController> pluginClass : PluginProperties.getInstance().getPluginClasses()) {
			PluginSettingsController pluginSettingsController = null;
			
			try {
				PluginController pluginController = this.uiController.getFrontlineController().getPluginManager().loadPluginController(pluginClass.getName());
				this.uiController.addPluginTextResources(pluginController);
				pluginSettingsController = pluginController.getSettingsController(this.uiController);
			
				if (pluginSettingsController != null) { // Then the Plugin has settings
					Object pluginRootNode = pluginSettingsController.getRootNode();
					
					// Collapse all root nodes by default
					this.uiController.setExpanded(pluginRootNode, false);
					
					this.uiController.add(find(UI_COMPONENT_PLUGIN_TREE), pluginRootNode);
				}
			} catch (Throwable t) {
				t.printStackTrace();
				LOG.warn("Error when trying to load settings for Plugin " + pluginClass.getSimpleName(), t);
			}
		}
	}
	
	/**
	 * Called when the selection changed in one of the two trees
	 * @param tree
	 */
	public void selectionChanged(Object tree) {
		uiController.setSelectedItem(getOtherTree(tree), null);
		Object selected = this.uiController.getSelectedItem(tree);
		
		Object attachedObject = this.uiController.getAttachedObject(selected);
		this.displayPanel((UiSettingsSectionHandler) attachedObject);
	}

	private Object getOtherTree(Object tree) {
		String otherName = uiController.getName(tree).equals(UI_COMPONENT_CORE_TREE)? UI_COMPONENT_PLUGIN_TREE: UI_COMPONENT_CORE_TREE;
		return uiController.find(settingsDialog, otherName);
	}

	/**
	 * Handles the display in the dialog
	 * @param handler The {@link UiSettingsSectionHandler} responsible of the panel which should be displayed.
	 */
	private void displayPanel(UiSettingsSectionHandler handler) {
		Object pnDisplaySettings = find(UI_COMPONENT_PN_DISPLAY_SETTINGS);
		
		this.uiController.removeAll(pnDisplaySettings);
		this.uiController.add(pnDisplaySettings, handler.getPanel());
		
		if (!this.handlersList.contains(handler)) {
			this.handlersList.add(handler);
		}
	}
	
	public void closeDialog() {
		if (this.changesList.isEmpty()) {
			removeDialog();
		} else {
			this.uiController.showConfirmationDialog("removeDialog", this, I18N_MESSAGE_CONFIRM_CLOSE_SETTINGS);
		}
	}

	/** Shows this dialog to the user. */
	public Object getDialog() {
		return settingsDialog;
	}

	/**
	 * Removes the provided component from the view.
	 */
	public void removeDialog() {
		this.uiController.remove(this.settingsDialog);
		this.uiController.removeConfirmationDialog();
	}
	
	private Object find (String componentName) {
		return this.uiController.find(settingsDialog, componentName);
	}
	
	public void save () {
		List<Object> validationMessages = new ArrayList<Object>();
		
		for (UiSettingsSectionHandler settingsSectionHandler : this.handlersList) {
			List<FrontlineValidationMessage> validation = settingsSectionHandler.validateFields();
			if (validation != null && !validation.isEmpty()) {
				for (FrontlineValidationMessage validationMessage : validation) {
					//validationMessages.add("[" + settingsSectionHandler.getTitle() + "] " + validationMessage.getLocalisedMessage());
					validationMessages.add(createValidationPanel(settingsSectionHandler, validationMessage));
				}
			}
		}
		
		if (validationMessages.isEmpty()) {
			this.doSave();
		} else {
			this.uiController.alert(validationMessages.toArray(new Object[0]));
		}
	}

	private Object createValidationPanel(UiSettingsSectionHandler settingsSectionHandler, FrontlineValidationMessage validationMessage) {
		Object panel = this.uiController.createPanel("Validation message");
		this.uiController.setGap(panel, 5);
		
		Object sectionNameLabel = this.uiController.createLabel(settingsSectionHandler.getTitle(), validationMessage.getIcon());
		this.uiController.setBold(sectionNameLabel);
		
		this.uiController.add(panel, sectionNameLabel);
		this.uiController.add(panel, this.uiController.createLabel(validationMessage.getLocalisedMessage()));
		
		return panel;
	}

	private void doSave() {
		for (UiSettingsSectionHandler settingsSectionHandler : this.handlersList) {
			settingsSectionHandler.save();
		}
		
		this.uiController.removeDialog(settingsDialog);
		this.uiController.infoMessage(InternationalisationUtils.getI18nString(I18N_SETTINGS_SAVED));
	}

//> INSTANCE HELPER METHODS

//> STATIC FACTORIES

//> STATIC HELPER METHODS

	public void notify(FrontlineEventNotification notification) {
		if (notification instanceof SettingsChangedEventNotification) {
			SettingsChangedEventNotification settingsNotification = (SettingsChangedEventNotification) notification;
			String sectionItem = settingsNotification.getSectionItem();
			
			if (settingsNotification.isUnchange()) {
				// A previous change has been cancelled, let's remove it from our list
				this.changesList.remove(sectionItem);
			} else {
				// This is an actual change, let's add it to our list if it's a new change
				if (!this.changesList.contains(sectionItem)) {
					this.changesList.add(sectionItem);
				}
			}
			
			boolean changesToSave = !this.changesList.isEmpty();
			this.handleCloseButton(changesToSave);
			this.handleSaveButton(changesToSave);
		} else if (notification instanceof UiDestroyEvent) {
			if(((UiDestroyEvent) notification).isFor(this.uiController)) {
				this.uiController.getFrontlineController().getEventBus().unregisterObserver(this);
			}
		}
	}
	
	private void handleCloseButton(boolean changesToSave) {
		Object btClose = find(UI_COMPONENT_BT_CLOSE);
		uiController.setText(btClose, InternationalisationUtils.getI18nString(
				changesToSave ? FrontlineSMSConstants.ACTION_CANCEL : FrontlineSMSConstants.ACTION_CLOSE));
	}

	private void handleSaveButton(boolean shouldEnableSaveButton) {
		// If our list of changes is empty, this means we went back to the original configuration
		Object btSave = find(UI_COMPONENT_BT_SAVE);
		this.uiController.setEnabled(btSave, shouldEnableSaveButton);
		
		String tooltip;
		if (shouldEnableSaveButton) {
			tooltip = InternationalisationUtils.getI18nString(I18N_TOOLTIP_SETTINGS_SAVES_ALL);
		} else {
			tooltip = InternationalisationUtils.getI18nString(I18N_TOOLTIP_SETTINGS_BTSAVE_DISABLED);
		}
		
		this.uiController.setTooltip(btSave, tooltip);
	}
	
//> CONSTANT HANDLERS
}