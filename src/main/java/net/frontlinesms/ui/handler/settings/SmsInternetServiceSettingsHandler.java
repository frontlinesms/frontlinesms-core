package net.frontlinesms.ui.handler.settings;

import java.lang.reflect.*;
import java.util.*;

import net.frontlinesms.*;
import net.frontlinesms.data.*;
import net.frontlinesms.data.domain.*;
import net.frontlinesms.events.EventBus;
import net.frontlinesms.messaging.Provider;
import net.frontlinesms.messaging.sms.events.InternetServiceEventNotification;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;
import net.frontlinesms.messaging.sms.internet.SmsInternetServiceLoader;
import net.frontlinesms.messaging.sms.properties.OptionalRadioSection;
import net.frontlinesms.messaging.sms.properties.OptionalSection;
import net.frontlinesms.messaging.sms.properties.PasswordString;
import net.frontlinesms.messaging.sms.properties.PhoneSection;
import net.frontlinesms.resources.UserHomeFilePropertySet;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.handler.contacts.ContactSelecter;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

import org.apache.log4j.Logger;

import thinlet.Thinlet;

/**
 * Ui Handler for {@link SmsInternetServiceSettingsHandler} settings.
 * 
 * @author Alex Anderson, Carlos Eduardo Genz
 */
public class SmsInternetServiceSettingsHandler implements ThinletUiEventHandler {
//> CONSTANTS
	/** Path to XML for UI layout for settings screen, {@link #settingsDialog} */
	private static final String UI_SETTINGS = "/ui/core/settings/services/internet/settings.xml";
	/** Path to XML for UI layout for provider choosing screen, {@link #newServiceWizard} */
	private static final String UI_CHOOSE_PROVIDER = "/ui/core/settings/services/internet/chooseProvider.xml";
	/** Path to XML for UI layout for configuration screen, {@link #configurator} */
	private static final String UI_CONFIGURE = "/ui/core/settings/services/internet/configure.xml";
	
	private static final String UI_COMPONENT_LS_ACCOUNTS = "lsSmsInternetServices";
	private static final String UI_COMPONENT_PN_BUTTONS = "pnButtons";
	
	/** Logging object */
	private static final Logger LOG = FrontlineUtils.getLogger(SmsInternetServiceSettingsHandler.class);

//> INSTANCE PROPERTIES
	/** Thinlet instance that owns this handler */
	private final UiGeneratorController controller;
	/** dialog for editing {@link SmsInternetService} settings, {@link SmsInternetServiceSettings} instances */
	private Object settingsDialog;
	/** dialog for choosing the class of a new {@link SmsInternetService} */
	private Object newServiceWizard;
	/** dialog for configuring a new {@link SmsInternetService} */
	private Object configurator;

	/** Properties file containing mappings from proeprty names to the icons that should be displayed next to input fields for these properties. */
	private IconMap iconProperties;
	/** All possible {@link SmsInternetService} classes available. */
	private final Collection<Class<? extends SmsInternetService>> internetServiceProviders;
	private EventBus eventBus;

//> CONSTRUCTORS
	/**
	 * Creates a new instance of this UI.
	 * @param controller thinlet controller that owns this {@link SmsInternetServiceSettingsHandler}.
	 */
	public SmsInternetServiceSettingsHandler(UiGeneratorController controller) {
		this.controller = controller;
		this.eventBus = controller.getFrontlineController().getEventBus();
		
		iconProperties = new IconMap(FrontlineSMSConstants.PROPERTIES_SMS_INTERNET_ICONS);

		this.internetServiceProviders = new SmsInternetServiceLoader().getAll();
	}

	/** Clears the desktop of all dialogs that this controls. */
	private void clearDesktop() {
		if(newServiceWizard != null) removeDialog(newServiceWizard);
	}

	/**
	 * Shows the general confirmation dialog (for removal). 
	 * @param methodToBeCalled the method to be called if the confirmation is affirmative
	 */
	public void showConfirmationDialog(String methodToBeCalled){
		controller.showConfirmationDialog(methodToBeCalled, this);
	}

	/** Show this dialog to the user. */
	public void showSettingsDialog() {
		clearDesktop();

		settingsDialog = controller.loadComponentFromFile(UI_SETTINGS, this);

		// Update the list of accounts from the list provided
		Object accountList = controller.find(settingsDialog, UI_COMPONENT_LS_ACCOUNTS);
		this.refreshAccounts(accountList);
		
		selectionChanged(accountList, controller.find(settingsDialog, UI_COMPONENT_PN_BUTTONS));
		controller.add(settingsDialog);
	}

	private void refreshAccounts(Object accountList) {
		if (accountList != null) {
			this.controller.removeAll(accountList);
			Collection<SmsInternetService> smsInternetServices = controller.getSmsInternetServices();
			for (SmsInternetService service : smsInternetServices) {
				controller.add(accountList, controller.createListItem(getProviderName(service.getClass()) + " - " + service.getIdentifier(), service));
			}
		}
	}

	/** Show the wizard for creating a new service. */
	public void showNewServiceWizard() {
		clearDesktop();

		newServiceWizard = controller.loadComponentFromFile(UI_CHOOSE_PROVIDER, this);
		Object providerList = controller.find(newServiceWizard, "lsProviders");
		if (providerList != null) {
			for (Class<? extends SmsInternetService> provider : internetServiceProviders) {
				Object item = controller.createListItem(getProviderName(provider), provider.getCanonicalName());
				String icon = getProviderIcon(provider);
				if (icon != null) {
					controller.setIcon(item, controller.getIcon(icon));
				}
				controller.add(providerList, item);
			}
		}

		selectionChanged(providerList, controller.find(newServiceWizard, "pnButtons"));
		controller.add(newServiceWizard);
	}

	/**
	 * Configure a provider given its UI component.
	 * @param lsProviders
	 */
	public void configureService(Object lsProviders) {
		Object serviceComponent = this.controller.getSelectedItem(lsProviders);
		showConfigureService((SmsInternetService)controller.getAttachedObject(serviceComponent), settingsDialog);
	}

	/**
	 * Removes the provided component from the view.
	 * @param component
	 */
	public void removeDialog(Object component) {
		controller.remove(component);
	}

	/**
	 * Configure a new provider.  The class of this provider is provided as an attachment
	 * to the selected list item in the provided list.
	 * @param lsProviders
	 */
	@SuppressWarnings("unchecked")
	public void configureNewService(Object lsProviders) {
		Object selectedItem = controller.getSelectedItem(lsProviders);
		clearDesktop();
		try {
			String providerClassName = (String)controller.getAttachedObject(selectedItem);
			LOG.info("Attempting to init SmsInternetService class: " + providerClassName);
			Class<? extends SmsInternetService> providerClass = (Class<? extends SmsInternetService>) Class.forName(providerClassName);
			SmsInternetService service = providerClass.getConstructor().newInstance();
			showConfigureService(service, newServiceWizard);
		} catch(Throwable t) {
			LOG.warn("Error initialising SmsInternetService.", t);
			throw new RuntimeException(t);
		}
	}
	
	public void cancelAction(Object btCancel, Object dialog) {
		Object attached = controller.getAttachedObject(btCancel);
		removeDialog(dialog);
		if (attached != null) {
			controller.add(attached);
		}
	}
	
	/**
	 * Enables/Disables fields from panel, according to list selection.
	 * @param list
	 * @param panel
	 */
	public void selectionChanged(Object list, Object panel) {
		for (Object item : controller.getItems(panel)) {
			String name = controller.getName(item); 
			if (!"btNew".equals(name)
					&& !"btCancel".equals(name)) {
				controller.setEnabled(item, controller.getSelectedItem(list) != null);
			}
		}
	}

	/**
	 * Show the dialog for configuring a provider.
	 * @param service
	 */
	public void showConfigureService(SmsInternetService service, Object fromDialog) {
		configurator = controller.loadComponentFromFile(UI_CONFIGURE, this);
		String icon = getProviderIcon(service.getClass());
		if (icon != null) {
			controller.setIcon(configurator, controller.getIcon(icon));
		}
		controller.setAttachedObject(configurator, service);
		controller.setText(configurator, getProviderName(service.getClass()) + " " + controller.getText(configurator));
		Object configPanel = controller.find(configurator, "pnConfigFields");
		StructuredProperties properties = service.getPropertiesStructure();
		if (service.getSettings() != null) {
			properties.loadPropertiesFromDbIntoStructure(service.getSettings().getProperties());
		}
		for (String key : properties.keySet()) {
			Object value = properties.get(key);
			for (Object comp : getPropertyComponents(key, value))
				controller.add(configPanel, comp);
		}
		
		if (fromDialog != null) {
			controller.setAttachedObject(controller.find(configurator, "btCancel"), fromDialog);
			if (fromDialog.equals(newServiceWizard)) {
				controller.setAttachedObject(controller.find(configurator, "btSave"), settingsDialog);
			} else {
				controller.setAttachedObject(controller.find(configurator, "btSave"), fromDialog);
			}
		}
		
		clearDesktop();
		controller.add(configurator);
	}

	/** Confirms deletes of {@link SmsInternetService}(s) from the system and removes them from the list of services */
	public void removeServices() {
		controller.removeConfirmationDialog();
		removeServices(controller.find(settingsDialog, "lsSmsInternetServices"));
	}

	/**
	 * Delete the selected services from the system and remove them from the list.
	 * @param lsProviders
	 */
	private void removeServices(Object lsProviders) {
		Object[] obj = controller.getSelectedItems(lsProviders);
		for (Object object : obj) {
			SmsInternetService service = (SmsInternetService) controller.getAttachedObject(object);
			controller.getSmsInternetServiceSettingsDao().deleteSmsInternetServiceSettings(service.getSettings());
			controller.remove(object);
			
			this.eventBus.notifyObservers(new InternetServiceEventNotification(InternetServiceEventNotification.EventType.DELETE, service));
		}
		selectionChanged(lsProviders, controller.find(settingsDialog, "pnButtons"));
	}

	/**
	 * Gets a Thinlet UI component for configuring this property.  The current value of the property will
	 * be inserted into the UI component.
	 * @param key key for the property
	 * @param valueObj current value of the property
	 * @return UI components for the property
	 */
	@SuppressWarnings("unchecked")
	private Object[] getPropertyComponents(String key, Object valueObj) {
		Object[] components;
		String label;
		try {
			label = InternationalisationUtils.getI18nString(key);
		} catch(MissingResourceException ex) {
			label = key;
		}
		String valueString = SmsInternetServiceSettings.toValue(valueObj).getValue();

		if(valueObj instanceof String || valueObj instanceof Integer || valueObj instanceof PasswordString) {
			// FIXME can we clean up this use of valueString here?  Surely password string should have
			// only one extra thing: thinlet.setBoolean(tf, "hidden", true) ?
			// If we have a db value, use that cos it's the right one
			components = new Object[2];
			components[0] = controller.createLabel(label);
			if (iconProperties.hasIcon(key)) {
				controller.setIcon(components[0], controller.getIcon(iconProperties.getIcon(key)));
			}
			Object tf;
			if (valueObj instanceof PasswordString) {
				tf = controller.createPasswordfield(key, ((PasswordString)valueObj).getValue());
			} else {
				tf = controller.createTextfield(key, valueString);
			}
			controller.setColumns(tf, 25);
			controller.setInteger(tf, "weightx", 1);
			components[1] = tf;
		} else if(valueObj instanceof Boolean) {
			//If we have a db value, use that cos it's the right one
			Object checkbox = controller.createCheckbox(key, label, Boolean.parseBoolean(valueString));
			if (iconProperties.hasIcon(key)) {
				controller.setIcon(checkbox, controller.getIcon(iconProperties.getIcon(key)));
			}
			controller.setColspan(checkbox, 2);
			components = new Object[] {checkbox};
		} else if (valueObj instanceof PhoneSection) {
			Object panel = controller.createPanel("pn" + key.replace(".", "_"));
			controller.setInteger(panel, "gap", 5);
			controller.setInteger(panel, "weightx", 1);
			Object lb = controller.createLabel(label);
			if (iconProperties.hasIcon(key)) {
				controller.setIcon(lb, controller.getIcon(iconProperties.getIcon(key)));
			}
			//If we have a db value, use that cos it's the right one
			Object tf = controller.createTextfield(key, valueString);
			controller.setInteger(tf, "weightx", 1);
			//controller.setInteger(tf, Thinlet.ATTRIBUTE_COLUMNS, 20);
			Object bt = controller.createButton("");
			controller.setIcon(bt, controller.getIcon(PhoneSection.BUTTON_ICON));
			controller.setAttachedObject(bt, tf);
			controller.add(panel, tf);
			controller.add(panel, bt);
			controller.setAction(bt, "showContacts(this)", panel, this);
			components = new Object[] {lb, panel};
		} else if (valueObj instanceof OptionalSection) {
			OptionalSection section = (OptionalSection) valueObj;
			boolean toSet = Boolean.parseBoolean(valueString);
			Object checkbox = controller.createCheckbox(key, label, toSet);
			if (iconProperties.hasIcon(key)) {
				controller.setIcon(checkbox, controller.getIcon(iconProperties.getIcon(key)));
			}
			controller.setColspan(checkbox, 2);
			Object panel = controller.createPanel("pn" + key.replace(".", "_"));
			controller.setColspan(panel, 2);
			controller.setColumns(panel, 2);
			controller.setGap(panel, 8);
			controller.setInteger(panel, "top", 10); // TODO these should call methods in ExtendedThinlet
			controller.setInteger(panel, "right", 10);
			controller.setInteger(panel, "left", 10);
			controller.setInteger(panel, "bottom", 10);
			controller.setBorder(panel, true);
			List<Object> objects = new LinkedList<Object>();
			objects.add(checkbox);
			for (String child : section.getDependencies().keySet()) {
				for (Object comp : getPropertyComponents(child, section.getDependencies().get(child))) {
					controller.add(panel, comp);
				}
			}
			objects.add(panel);
			components = objects.toArray();
			controller.setAction(checkbox, "enableFields(this.selected, " + controller.getName(panel) + ")", panel, this);
			enableFields(controller.isSelected(checkbox), panel);
		} else if (valueObj instanceof OptionalRadioSection) {
			OptionalRadioSection section = (OptionalRadioSection) valueObj;
			Object panel = controller.createPanel(key);
			controller.setColspan(panel, 2);
			controller.setColumns(panel, 1);
			controller.setGap(panel, 8);
			controller.setInteger(panel, "top", 10); // TODO these should call methods in ExtendedThinlet
			controller.setInteger(panel, "right", 10);
			controller.setInteger(panel, "left", 10);
			controller.setInteger(panel, "bottom", 10);
			controller.setInteger(panel, "weightx", 1);
			controller.setBorder(panel, true);
			controller.setText(panel, label);
			if (iconProperties.hasIcon(key)) {
				controller.setIcon(panel, controller.getIcon(iconProperties.getIcon(key)));
			}

			valueString = valueString.substring(valueString.lastIndexOf(".") + 1);
			try {
				Method getValues = section.getValue().getClass().getMethod("values");
				Enum[] vals = (Enum[]) getValues.invoke(null);
				for (Enum val : vals) {
					Object rb = controller.createRadioButton(key + val.name(), val.name(), key, val.name().equals(valueString));
					controller.add(panel, rb);
					StructuredProperties child = section.getDependencies(val);
					Object panelChild = controller.createPanel(key + val.ordinal());
					controller.setColspan(panelChild, 2);
					controller.setColumns(panelChild, 2);
					controller.setGap(panelChild, 8);
					controller.setInteger(panelChild, "top", 10);
					controller.setInteger(panelChild, "right", 10);
					controller.setInteger(panelChild, "left", 10);
					controller.setInteger(panelChild, "bottom", 10);
					controller.setInteger(panelChild, "weightx", 1);
					for (String childKey : child.keySet()) {
						for (Object comp : getPropertyComponents(childKey, child.get(childKey))) {
							controller.add(panelChild, comp);
						}
					}
					controller.add(panel, panelChild);
					controller.setAttachedObject(rb, panelChild);
					controller.setAction(rb, "enableFields(" + controller.getName(panel) + ")", panel, this);
				}
				enableFields(panel);
			} catch (Throwable t) {
				LOG.error("Could not get values from enum [" + valueObj.getClass() + "]", t);
			}
			components = new Object[] {panel};
		} else if (valueObj instanceof Enum<?>) {
			components = new Object[1];
			Object panel = controller.createPanel(key);
			controller.setColspan(panel, 2);
			controller.setColumns(panel, 1);
			controller.setInteger(panel, "gap", 8);
			controller.setInteger(panel, "top", 10);
			controller.setInteger(panel, "right", 10);
			controller.setInteger(panel, "left", 10);
			controller.setInteger(panel, "bottom", 10);
			controller.setBorder(panel, true);
			controller.setText(panel, label);
			if (iconProperties.hasIcon(key)) {
				controller.setIcon(panel, controller.getIcon(iconProperties.getIcon(key)));
			}
			try {
				Method getValues = valueObj.getClass().getMethod("values");
				Enum[] vals = (Enum[]) getValues.invoke(null);
				for (Enum val : vals) {
					controller.add(panel, controller.createRadioButton(key + val.name(), val.name(), key, val.name().equals(valueString)));
				}
			} catch (Throwable t) {
				LOG.error("Could not get values from enum [" + valueObj.getClass() + "]", t);
			}
			components[0] = panel;
		} else throw new RuntimeException("Unsupported property type for property '"+key+"': " + valueObj.getClass());

		return components;
	}

	public void showContacts(Object button) {
		Object textField = controller.getAttachedObject(button);
		ContactSelecter contactSelecter = new ContactSelecter(controller);
		final boolean shouldHaveEmail = false;
		contactSelecter.show(InternationalisationUtils.getI18nString(FrontlineSMSConstants.COMMON_SENDER_NUMBER), "setContactNumber(contactSelecter_contactList, contactSelecter)", textField, this, shouldHaveEmail);
	}

	public void setContactNumber(Object list, Object dialog) {
		Object textField = controller.getAttachedObject(dialog);
		Object selectedItem = controller.getSelectedItem(list);
		if (selectedItem == null) {
			controller.alert(InternationalisationUtils.getI18nString(FrontlineSMSConstants.MESSAGE_NO_CONTACT_SELECTED));
			return;
		}
		Contact selectedContact = controller.getContact(selectedItem);
		controller.setText(textField, selectedContact.getPhoneNumber());
		removeDialog(dialog);
	}

	public void enableFields(boolean checked, Object panel) {
		controller.setEnabled(panel, checked);
		for (Object obj : controller.getItems(panel)) {
			enableFields(checked, obj);
		}
	}

	public void enableFields(Object panel) {
		for (Object child : controller.getItems(panel)) {
			if (Thinlet.getClass(child).equals(Thinlet.WIDGET_CHECKBOX)) {
				Object childPanel = controller.getAttachedObject(child);
				enableFields(controller.isSelected(child), childPanel);
			}
		}
	}

	/**
	 * Gets the value of a property from its UI components.  This method reverses the action of
	 * {@link #getPropertyComponents(String, Object)}
	 * @param comp the ui component containing this property value
	 * @param clazz The class of this property value
	 * @return
	 */
	private Object getPropertyValue(Object comp, Class<?> clazz) {
		if(clazz.equals(String.class))
			return controller.getText(comp);
		if(clazz.equals(Integer.class))
			return Integer.parseInt(controller.getText(comp));
		if(clazz.equals(Boolean.class))
			return new Boolean(controller.isSelected(comp));
		if(clazz.equals(PasswordString.class))
			return new PasswordString(controller.getText(comp));
		if (clazz.equals(OptionalSection.class))
			return new Boolean(controller.isSelected(comp));
		if(clazz.equals(PhoneSection.class))
			return new PhoneSection(controller.getText(comp));
		if (clazz.equals(OptionalRadioSection.class)) {
			for (Object child : controller.getItems(comp)) {
				if (Thinlet.getClass(child).equals(Thinlet.WIDGET_CHECKBOX) && controller.isSelected(child)) {
					return controller.getText(child);
				}
			}
		}
		if (clazz.isEnum()) {
			for (Object child : controller.getItems(comp)) {
				if (Thinlet.getClass(child).equals(Thinlet.WIDGET_CHECKBOX) && controller.isSelected(child)) {
					return controller.getText(child);
				}
			}
		}
		throw new RuntimeException("Unsupported property type: " + clazz);
	}

	/**
	 * Save the settings of the {@link SmsInternetService} and return to the main settings dialog.
	 * @param pnSmsInternetServiceConfigure
	 * @throws DuplicateKeyException 
	 */
	public void saveSettings(Object pnSmsInternetServiceConfigure, Object btSave) throws DuplicateKeyException {
		SmsInternetService service = (SmsInternetService)controller.getAttachedObject(pnSmsInternetServiceConfigure);
		SmsInternetServiceSettings serviceSettings = service.getSettings(); 
		if (serviceSettings == null) {
			serviceSettings = new SmsInternetServiceSettings(service);
			controller.getSmsInternetServiceSettingsDao().saveSmsInternetServiceSettings(serviceSettings);
		}
		StructuredProperties properties = service.getPropertiesStructure();
		saveSettings(pnSmsInternetServiceConfigure, serviceSettings, properties);
		service.setSettings(serviceSettings);
		controller.getSmsInternetServiceSettingsDao().updateSmsInternetServiceSettings(service.getSettings());
		// Add this service to the frontline controller.  TODO surely there is a nicer way of doing this?
		removeDialog(pnSmsInternetServiceConfigure);
		
		this.eventBus.notifyObservers(new InternetServiceEventNotification(InternetServiceEventNotification.EventType.ADD, service));
	}

	@SuppressWarnings("unchecked")
	private void saveSettings(Object pnSmsInternetServiceConfigure, SmsInternetServiceSettings serviceSettings, StructuredProperties properties) {
		for(String key : properties.keySet()) {
			Object propertyUiComponent = controller.find(pnSmsInternetServiceConfigure, key);
			Object newValue = getPropertyValue(propertyUiComponent, properties.get(key).getClass());
			if (properties.get(key) instanceof OptionalSection) {
				OptionalSection section = (OptionalSection) properties.get(key);
				section.setValue((Boolean) newValue);
				serviceSettings.set(key, section);
				saveSettings(pnSmsInternetServiceConfigure, serviceSettings, section.getDependencies());
			} else if (properties.get(key) instanceof OptionalRadioSection) {
				OptionalRadioSection section = (OptionalRadioSection) properties.get(key);
				try {
					Method getValueOf = section.getValue().getClass().getMethod("valueOf", String.class);
					Enum enumm = (Enum) getValueOf.invoke(null, newValue);
					section.setValue(enumm);
					serviceSettings.set(key, section);
					Method getValues = enumm.getClass().getMethod("values");
					Enum[] vals = (Enum[]) getValues.invoke(null);
					for (Enum val : vals) {
						saveSettings(pnSmsInternetServiceConfigure, serviceSettings, section.getDependencies(val));
					}
				} catch (Throwable t) {
					LOG.error("Could not get values from enum.", t);
				}

			} else {
				serviceSettings.set(key, newValue);
			}
		}
	}

	/**
	 * Gets the icon associated with a particular {@link SmsInternetService}.
	 * @param clazz The class of the {@link SmsInternetService}.
	 * @return the path at which the icon file is located
	 */
	public static String getProviderIcon(Class<?> clazz) {
		String ret = null; //Default return value
		if (clazz.isAnnotationPresent(Provider.class)) {
			Provider provider = clazz.getAnnotation(Provider.class);
			if (provider != null && !provider.icon().equals("")) {
				ret = provider.icon();
			}
		}
		return ret;
	}

	/**
	 * Gets the name associated with a particular {@link SmsInternetService}.
	 * @param clazz The class of the {@link SmsInternetService}.
	 * @return the name to display for a provider
	 */
	public static String getProviderName(Class<?> clazz) {
		String ret = clazz.getCanonicalName(); //Default return value
		if (clazz.isAnnotationPresent(Provider.class)) {
			Provider provider = clazz.getAnnotation(Provider.class);
			if (provider != null && !provider.name().equals("")) {
				ret = provider.name();
			}
		}
		return ret;
	}
}

final class IconMap extends UserHomeFilePropertySet {
//> STATIC CONSTANTS

//> INSTANCE PROPERTIES
	
//> CONSTRUCTORS
	/**
	 * Set up a new {@link IconMap}.
	 * @param name the name of the icon map
	 */
	IconMap(String name) {
		super(name);
	}

//> ACCESSORS
	/**
	 * Check if there is an icon available. 
	 * @param key The key for the icon.
	 * @return <code>true</code> if there is an icon specified for this key; <code>false</code> otherwise.
	 */
	public boolean hasIcon(String key) {
		return this.getIcon(key) != null;
	}
	
	/**
	 * Get the icon path for the specified key.
	 * @param key
	 * @return
	 */
	public String getIcon(String key) {
		return super.getProperty(key);
	}

//> INSTANCE HELPER METHODS

//> STATIC FACTORIES

//> STATIC HELPER METHODS
}