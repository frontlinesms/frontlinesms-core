package net.frontlinesms.serviceconfig.ui;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.MissingResourceException;

import org.apache.log4j.Logger;

import thinlet.Thinlet;

import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.domain.PersistableSettingValue;
import net.frontlinesms.data.domain.PersistableSettings;
import net.frontlinesms.data.events.DatabaseEntityNotification;
import net.frontlinesms.data.events.EntityDeleteWarning;
import net.frontlinesms.data.repository.ConfigurableServiceSettingsDao;
import net.frontlinesms.events.EventObserver;
import net.frontlinesms.events.FrontlineEventNotification;
import net.frontlinesms.serviceconfig.ConfigurableService;
import net.frontlinesms.serviceconfig.OptionalRadioSection;
import net.frontlinesms.serviceconfig.OptionalSection;
import net.frontlinesms.serviceconfig.PasswordString;
import net.frontlinesms.serviceconfig.PhoneNumber;
import net.frontlinesms.serviceconfig.ConfigurableServiceProperties;
import net.frontlinesms.serviceconfig.SmsModemReference;
import net.frontlinesms.serviceconfig.StructuredProperties;
import net.frontlinesms.settings.BaseSectionHandler;
import net.frontlinesms.settings.FrontlineValidationMessage;
import net.frontlinesms.ui.IconMap;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiDestroyEvent;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.events.FrontlineUiUpdateJob;
import net.frontlinesms.ui.handler.contacts.ContactSelecter;
import net.frontlinesms.ui.handler.phones.ConfiguredModemSelecter;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.settings.UiSettingsSectionHandler;

/**
 * Ui Handler for service settings.
 * @author Alex Anderson, Carlos Eduardo Genz
 */
public abstract class BaseServiceSettingsHandler<T extends ConfigurableService>
		extends BaseSectionHandler
		implements ThinletUiEventHandler, UiSettingsSectionHandler, EventObserver {
//> CONSTANTS
	private static final String UI_SECTION_SERVICE_LIST = "/ui/core/settings/services/pnServiceList.xml";
	/** Path to XML for UI layout for settings screen, {@link #settingsDialog} */
	private static final String UI_SETTINGS = "/ui/core/settings/services/dgSettings.xml";
	/** Path to XML for UI layout for provider choosing screen, {@link #newServiceWizard} */
	private static final String UI_CHOOSE_PROVIDER = "/ui/core/settings/services/dgChooseProvider.xml";
	/** Path to XML for UI layout for configuration screen, {@link #configurator} */
	private static final String UI_CONFIGURE = "/ui/core/settings/services/dgConfigure.xml";

	private static final String UI_COMPONENT_LS_ACCOUNTS = "lsServices";
	private static final String UI_COMPONENT_LS_PROVIDERS = "lsProviders";
	private static final String UI_COMPONENT_PN_BUTTONS = "pnButtons";

	/** Logging object */
	private final Logger log = FrontlineUtils.getLogger(getClass());
	

//> INSTANCE PROPERTIES
	private final String title;
	private final String icon;
	
	/** dialog for editing {@link PersistableSettings} instances */
	private Object settingsDialog;
	/** dialog for choosing the class of a new {@link ConfigurableService} */
	private Object newServiceWizard;
	/** dialog for configuring a new {@link ConfigurableService} */
	private Object configurator;

	/** Properties file containing mappings from property names to the icons that should be displayed next to input fields for these properties. */
	private final IconMap iconProperties;
	/** All possible {@link SmsInternetService} classes available. */
	private final Collection<Class<? extends T>> serviceProviders;
	
	protected final ConfigurableServiceSettingsDao<T> settingsDao;

//> CONSTRUCTORS
	/**
	 * Creates a new instance of this UI.
	 * @param ui thinlet controller that owns this {@link SmsInternetServiceSettingsHandler}.
	 */
	public BaseServiceSettingsHandler(UiGeneratorController ui,
			ConfigurableServiceSettingsDao<T> settingsDao,
			Collection<Class<? extends T>> serviceProviders,
			String title, String icon) {
		super(ui);
		this.ui = ui;
		this.iconProperties = ui.getIconMap();
		this.title = title;
		this.icon = icon;

		this.serviceProviders = serviceProviders;
		this.settingsDao = settingsDao;
	}
	
	@Override
	protected void init() {
		this.panel = ui.loadComponentFromFile(UI_SECTION_SERVICE_LIST, this);

		// Update the list of accounts from the list provided
		refresh();
		
		eventBus.registerObserver(this);
	}
	
	public abstract Class<T> getServiceSupertype();
	public abstract String getIconMapLocation();

	/** Clears the desktop of all dialogs that this controls. */
	private void clearDesktop() {
		if(newServiceWizard != null) removeDialog(newServiceWizard);
	}
	
//> INTERNAL HELPER METHODS
	private final void refresh() {
		new FrontlineUiUpdateJob() {
			public void run() {
				Object accountList = find(UI_COMPONENT_LS_ACCOUNTS);
				if (accountList != null) {
					refreshAccounts(accountList);
				}
				
				selectionChanged(accountList, find(UI_COMPONENT_PN_BUTTONS));
			}
		}.execute();
	}

	public void refreshAccounts(Object accountList) {
		ui.removeAll(accountList);
		for(PersistableSettings s : settingsDao.getServiceAccounts()) {
			String description = getProviderName(s.getServiceClass()) + " - " +
					s.getId(); // TODO should use something more user friendly than database ID here
			Object listItem = ui.createListItem(description, s);
			ui.setIcon(listItem, getProviderIcon(s.getServiceClass()));
			ui.add(accountList, listItem);
		}
	}

//> UI EVENT METHODS
	/**
	 * Enables/Disables fields from panel, according to list selection.
	 * @param list
	 * @param panel
	 */
	public final void selectionChanged(Object list, Object panel) {
		boolean enableButtons = this.ui.getSelectedItem(list) != null;
		for (Object item : this.ui.getItems(panel)) {
			String name = this.ui.getName(item); 
			if (!"btNew".equals(name) && !"btCancel".equals(name)) {
				this.ui.setEnabled(item, enableButtons);
			}
		}
	}
	
	
	/**
	 * Shows the general confirmation dialog (for removal). 
	 * @param methodToBeCalled the method to be called if the confirmation is affirmative
	 */
	public void showConfirmationDialog(String methodToBeCalled){
		ui.showConfirmationDialog(methodToBeCalled, this);
	}

	/** Show this dialog to the user. */
	public void showSettingsDialog() {
		clearDesktop();

		settingsDialog = ui.loadComponentFromFile(UI_SETTINGS, this);

		// Update the list of accounts from the list provided
		Object accountList = ui.find(settingsDialog, UI_COMPONENT_LS_ACCOUNTS);
		refreshAccounts(accountList);
		
		selectionChanged(accountList, ui.find(settingsDialog, UI_COMPONENT_PN_BUTTONS));
		ui.add(settingsDialog);
	}

	/** Show the wizard for creating a new service. */
	public void showNewServiceWizard() {
		clearDesktop();

		newServiceWizard = ui.loadComponentFromFile(UI_CHOOSE_PROVIDER, this);
		Object providerList = ui.find(newServiceWizard, UI_COMPONENT_LS_PROVIDERS);
		if (providerList != null) {
			for (Class<? extends T> provider : serviceProviders) {
				Object item = ui.createListItem(getProviderName(provider), provider);
				String icon = getProviderIcon(provider);
				if (icon != null) {
					ui.setIcon(item, ui.getIcon(icon));
				}
				ui.add(providerList, item);
			}
		}

		selectionChanged(providerList, ui.find(newServiceWizard, UI_COMPONENT_PN_BUTTONS));
		ui.add(newServiceWizard);
	}

	/**
	 * Configure a provider given its UI component.
	 * @param lsProviders
	 */
	public void configureService(Object lsProviders) {
		Object serviceComponent = this.ui.getSelectedItem(lsProviders);
		showConfigureService((PersistableSettings) ui.getAttachedObject(serviceComponent), settingsDialog);
	}

	/**
	 * Removes the provided component from the view.
	 * @param component
	 */
	public void removeDialog(Object component) {
		ui.remove(component);
	}

	/**
	 * Configure a new provider.  The class of this provider is provided as an attachment
	 * to the selected list item in the provided list.
	 * @param lsProviders
	 */
	@SuppressWarnings("unchecked")
	public void configureNewService(Object lsProviders) {
		Object selectedItem = ui.getSelectedItem(lsProviders);
		clearDesktop();
		Class<? extends T> providerClass = ui.getAttachedObject(selectedItem, Class.class);
		log.info("Attempting to init SmsInternetService class: " + providerClass.getName());
		showConfigureService(new PersistableSettings(getServiceSupertype(), providerClass), newServiceWizard);
	}
	
	public void cancelAction(Object btCancel, Object dialog) {
		Object attached = ui.getAttachedObject(btCancel);
		removeDialog(dialog);
		if (attached != null) {
			ui.add(attached);
		}
	}
	
	public void showConfigureService(PersistableSettings settings) {
		showConfigureService(settings, null);
	}

	/**
	 * Show the dialog for configuring a provider.
	 * @param service
	 */
	public void showConfigureService(PersistableSettings settings, Object fromDialog) {
		configurator = ui.loadComponentFromFile(UI_CONFIGURE, this);
		String icon = getProviderIcon(settings.getServiceClass());
		if (icon != null) {
			ui.setIcon(configurator, ui.getIcon(icon));
		}
		ui.setAttachedObject(configurator, settings);
		ui.setText(configurator, getProviderName(settings.getServiceClass()) + " " + ui.getText(configurator));
		Object configPanel = ui.find(configurator, "pnConfigFields");
		StructuredProperties properties = settings.getStructuredProperties();
		for (String key : properties.keySet()) {
			Object value = properties.getShallow(key);
			for (Object comp : getPropertyComponents(key, value))
				ui.add(configPanel, comp);
		}
		
		if (fromDialog != null) {
			ui.setAttachedObject(ui.find(configurator, "btCancel"), fromDialog);
			if (fromDialog.equals(newServiceWizard)) {
				ui.setAttachedObject(ui.find(configurator, "btSave"), settingsDialog);
			} else {
				ui.setAttachedObject(ui.find(configurator, "btSave"), fromDialog);
			}
		}
		
		clearDesktop();
		ui.add(configurator);
	}

	/** Confirms deletes of {@link SmsInternetService}(s) from the system and removes them from the list of services */
	public void removeServices() {
		removeServices(find(UI_COMPONENT_LS_ACCOUNTS));
		ui.removeConfirmationDialog();
	}

	/**
	 * Delete the selected services from the system and remove them from the list.
	 * @param lsProviders
	 */
	private void removeServices(Object lsProviders) {
		Object[] obj = ui.getSelectedItems(lsProviders);
		for (Object object : obj) {
			PersistableSettings settings = (PersistableSettings) ui.getAttachedObject(object, getServiceSupertype());
			settingsDao.deleteServiceSettings(settings);
		}
	}
	
	/**
	 * Gets a Thinlet UI component for configuring this property.  The current value of the property will
	 * be inserted into the UI component.
	 * @param key key for the property
	 * @param valueObj current value of the property
	 * @return UI components for the property
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object[] getPropertyComponents(String key, Object valueObj) {
		Object[] components;
		String label;
		try {
			label = InternationalisationUtils.getI18nString(key);
		} catch(MissingResourceException ex) {
			label = key;
		}
		String valueString = PersistableSettingValue.create(valueObj).getValue();

		if(valueObj instanceof String || valueObj instanceof Integer
				|| valueObj instanceof PasswordString || valueObj instanceof Long
				|| valueObj instanceof BigDecimal) {
			// FIXME can we clean up this use of valueString here?  Surely password string should have
			// only one extra thing: thinlet.setBoolean(tf, "hidden", true) ?
			// If we have a db value, use that cos it's the right one
			components = new Object[2];
			components[0] = ui.createLabel(label);
			if (iconProperties.hasIcon(key)) {
				ui.setIcon(components[0], ui.getIcon(iconProperties.getIcon(key)));
			}
			Object tf;
			if (valueObj instanceof PasswordString) {
				tf = ui.createPasswordfield(key, ((PasswordString)valueObj).getValue());
			} else {
				tf = ui.createTextfield(key, valueString);
			}
			ui.setColumns(tf, 25);
			ui.setInteger(tf, "weightx", 1);
			components[1] = tf;
		} else if(valueObj instanceof Boolean) {
			//If we have a db value, use that cos it's the right one
			Object checkbox = ui.createCheckbox(key, label, (Boolean) valueObj);
			if (iconProperties.hasIcon(key)) {
				ui.setIcon(checkbox, ui.getIcon(iconProperties.getIcon(key)));
			}
			ui.setColspan(checkbox, 2);
			components = new Object[] {checkbox};
		} else if (valueObj instanceof PhoneNumber) {
			Object panel = ui.createPanel("pn" + key.replace(".", "_"));
			ui.setInteger(panel, "gap", 5);
			ui.setInteger(panel, "weightx", 1);
			Object lb = ui.createLabel(label);
			if (iconProperties.hasIcon(key)) {
				ui.setIcon(lb, ui.getIcon(iconProperties.getIcon(key)));
			}
			//If we have a db value, use that cos it's the right one
			Object tf = ui.createTextfield(key, valueString);
			ui.setInteger(tf, "weightx", 1);
			//controller.setInteger(tf, Thinlet.ATTRIBUTE_COLUMNS, 20);
			Object bt = ui.createButton("");
			ui.setIcon(bt, ui.getIcon(PhoneNumber.BUTTON_ICON));
			ui.setAttachedObject(bt, tf);
			ui.add(panel, tf);
			ui.add(panel, bt);
			ui.setAction(bt, "showContacts(this)", panel, this);
			components = new Object[] {lb, panel};
		} else if (valueObj instanceof SmsModemReference) {
			Object panel = ui.createPanel("pn" + key.replace(".", "_"));
			ui.setInteger(panel, "gap", 5);
			ui.setInteger(panel, "weightx", 1);
			Object lb = ui.createLabel(label);
			if (iconProperties.hasIcon(key)) {
				ui.setIcon(lb, ui.getIcon(iconProperties.getIcon(key)));
			}
			
			Object tf = ui.createTextfield(key, valueString);
			ui.setEnabled(tf, false);
			ui.setInteger(tf, "weightx", 1);
			Object bt = ui.createButton("");
			ui.setIcon(bt, ui.getIcon(SmsModemReference.BUTTON_ICON));
			ui.setAttachedObject(bt, tf);
			ui.add(panel, tf);
			ui.add(panel, bt);
			ui.setAction(bt, "showConfiguredModems(this)", panel, this);
			components = new Object[] {lb, panel};
		} else if (valueObj instanceof OptionalSection) {
			OptionalSection section = (OptionalSection) valueObj;
			Object checkbox = ui.createCheckbox(key, label, section.getValue());
			if (iconProperties.hasIcon(key)) {
				ui.setIcon(checkbox, ui.getIcon(iconProperties.getIcon(key)));
			}
			ui.setColspan(checkbox, 2);
			Object panel = ui.createPanel("pn" + key.replace(".", "_"));
			ui.setColspan(panel, 2);
			ui.setColumns(panel, 2);
			ui.setGap(panel, 8);
			ui.setInteger(panel, "top", 10); // TODO these should call methods in ExtendedThinlet
			ui.setInteger(panel, "right", 10);
			ui.setInteger(panel, "left", 10);
			ui.setInteger(panel, "bottom", 10);
			ui.setBorder(panel, true);
			List<Object> objects = new LinkedList<Object>();
			objects.add(checkbox);
			for (String child : section.getDependencies().keySet()) {
				for (Object comp : getPropertyComponents(child, section.getDependencies().getShallow(child))) {
					ui.add(panel, comp);
				}
			}
			objects.add(panel);
			components = objects.toArray();
			ui.setAction(checkbox, "enableFields(this.selected, " + ui.getName(panel) + ")", panel, this);
			enableFields(ui.isSelected(checkbox), panel);
		} else if (valueObj instanceof OptionalRadioSection) {
			OptionalRadioSection section = (OptionalRadioSection) valueObj;
			Object panel = ui.createPanel(key);
			ui.setColspan(panel, 2);
			ui.setColumns(panel, 1);
			ui.setGap(panel, 8);
			ui.setInteger(panel, "top", 10); // TODO these should call methods in ExtendedThinlet
			ui.setInteger(panel, "right", 10);
			ui.setInteger(panel, "left", 10);
			ui.setInteger(panel, "bottom", 10);
			ui.setInteger(panel, "weightx", 1);
			ui.setBorder(panel, true);
			ui.setText(panel, label);
			if (iconProperties.hasIcon(key)) {
				ui.setIcon(panel, ui.getIcon(iconProperties.getIcon(key)));
			}

			valueString = valueString.substring(valueString.lastIndexOf(".") + 1);
			try {
				Method getValues = section.getValue().getClass().getMethod("values");
				Enum[] vals = (Enum[]) getValues.invoke(null);
				for (Enum val : vals) {
					Object rb = ui.createRadioButton(key + val.name(), val.name(), key, val.name().equals(valueString));
					ui.add(panel, rb);
					StructuredProperties child = section.getDependencies(val);
					Object panelChild = ui.createPanel(key + val.ordinal());
					ui.setColspan(panelChild, 2);
					ui.setColumns(panelChild, 2);
					ui.setGap(panelChild, 8);
					ui.setInteger(panelChild, "top", 10);
					ui.setInteger(panelChild, "right", 10);
					ui.setInteger(panelChild, "left", 10);
					ui.setInteger(panelChild, "bottom", 10);
					ui.setInteger(panelChild, "weightx", 1);
					for (String childKey : child.keySet()) {
						for (Object comp : getPropertyComponents(childKey, child.getShallow(childKey))) {
							ui.add(panelChild, comp);
						}
					}
					ui.add(panel, panelChild);
					ui.setAttachedObject(rb, panelChild);
					ui.setAction(rb, "enableFields(" + ui.getName(panel) + ")", panel, this);
				}
				enableFields(panel);
			} catch (Throwable t) {
				log.error("Could not get values from enum [" + valueObj.getClass() + "]", t);
			}
			components = new Object[] {panel};
		} else if (valueObj instanceof Enum<?>) {
			components = new Object[1];
			Object panel = ui.createPanel(key);
			ui.setColspan(panel, 2);
			ui.setColumns(panel, 1);
			ui.setInteger(panel, "gap", 8);
			ui.setInteger(panel, "top", 10);
			ui.setInteger(panel, "right", 10);
			ui.setInteger(panel, "left", 10);
			ui.setInteger(panel, "bottom", 10);
			ui.setBorder(panel, true);
			ui.setText(panel, label);
			if (iconProperties.hasIcon(key)) {
				ui.setIcon(panel, ui.getIcon(iconProperties.getIcon(key)));
			}
			try {
				Method getValues = valueObj.getClass().getMethod("values");
				Enum[] vals = (Enum[]) getValues.invoke(null);
				for (Enum val : vals) {
					ui.add(panel, ui.createRadioButton(key + val.name(), val.name(), key, val.name().equals(valueString)));
				}
			} catch (Throwable t) {
				log.error("Could not get values from enum [" + valueObj.getClass() + "]", t);
			}
			components[0] = panel;
		} else throw new RuntimeException("Unsupported property type for property '"+key+"': " + valueObj.getClass());

		return components;
	}

	public void showContacts(Object button) {
		Object textField = ui.getAttachedObject(button);
		ContactSelecter contactSelecter = new ContactSelecter(ui);
		final boolean shouldHaveEmail = false;
		contactSelecter.show(InternationalisationUtils.getI18nString(FrontlineSMSConstants.COMMON_SENDER_NUMBER), "setContactNumber(contactSelecter_contactList, contactSelecter)", textField, this, shouldHaveEmail);
	}

	public void setContactNumber(Object list, Object dialog) {
		Object textField = ui.getAttachedObject(dialog);
		Object selectedItem = ui.getSelectedItem(list);
		if (selectedItem == null) {
			ui.alert(InternationalisationUtils.getI18nString(FrontlineSMSConstants.MESSAGE_NO_CONTACT_SELECTED));
			return;
		}
		Contact selectedContact = ui.getContact(selectedItem);
		ui.setText(textField, selectedContact.getPhoneNumber());
		removeDialog(dialog);
	}
	
	public void showConfiguredModems(Object button) {
		Object textfield = ui.getAttachedObject(button);
		ConfiguredModemSelecter selecter = new ConfiguredModemSelecter(ui, textfield);
		selecter.show();
	}

	public void enableFields(boolean checked, Object panel) {
		ui.setEnabled(panel, checked);
		for (Object obj : ui.getItems(panel)) {
			enableFields(checked, obj);
		}
	}

	public void enableFields(Object panel) {
		for (Object child : ui.getItems(panel)) {
			if (Thinlet.getClass(child).equals(Thinlet.WIDGET_CHECKBOX)) {
				Object childPanel = ui.getAttachedObject(child);
				enableFields(ui.isSelected(child), childPanel);
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
			return ui.getText(comp);
		if(clazz.equals(Integer.class))
			return Integer.parseInt(ui.getText(comp));
		if(clazz.equals(Long.class))
			return Long.parseLong(ui.getText(comp));
		if(clazz.equals(BigDecimal.class))
			return new BigDecimal(ui.getText(comp));
		if(clazz.equals(Boolean.class))
			return new Boolean(ui.isSelected(comp));
		if(clazz.equals(PasswordString.class))
			return new PasswordString(ui.getText(comp));
		if (clazz.equals(OptionalSection.class))
			return new Boolean(ui.isSelected(comp));
		if(clazz.equals(PhoneNumber.class))
			return new PhoneNumber(ui.getText(comp));
		if (clazz.equals(OptionalRadioSection.class)) {
			for (Object child : ui.getItems(comp)) {
				if (Thinlet.getClass(child).equals(Thinlet.WIDGET_CHECKBOX) && ui.isSelected(child)) {
					return ui.getText(child);
				}
			}
		}
		if (clazz.isEnum()) {
			for (Object child : ui.getItems(comp)) {
				if (Thinlet.getClass(child).equals(Thinlet.WIDGET_CHECKBOX) && ui.isSelected(child)) {
					return ui.getText(child);
				}
			}
		}
		if(clazz.equals(SmsModemReference.class)) {
			return new SmsModemReference(ui.getText(comp));
		}
		throw new RuntimeException("Unsupported property type: " + clazz);
	}

	/**
	 * Save the settings of the {@link SmsInternetService} and return to the main settings dialog.
	 * @param pnSmsInternetServiceConfigure
	 * @throws DuplicateKeyException 
	 */
	public void saveSettings(Object pnSmsInternetServiceConfigure, Object btSave) throws DuplicateKeyException {
		PersistableSettings settings = ui.getAttachedObject(pnSmsInternetServiceConfigure, PersistableSettings.class); 
		StructuredProperties properties = settings.getStructuredProperties();
		saveSettings(pnSmsInternetServiceConfigure, settings, properties);
		
		if(settings.getId() == 0) {
			settingsDao.saveServiceSettings(settings);
		} else {
			settingsDao.updateServiceSettings(settings);
		}
		
		removeDialog(pnSmsInternetServiceConfigure);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void saveSettings(Object pnSmsInternetServiceConfigure, PersistableSettings serviceSettings, StructuredProperties properties) {
		for(String key : properties.keySet()) {
			Object propertyUiComponent = ui.find(pnSmsInternetServiceConfigure, key);
			Object newValue = getPropertyValue(propertyUiComponent, properties.getShallow(key).getClass());
			if (properties.getShallow(key) instanceof OptionalSection) {
				OptionalSection section = (OptionalSection) properties.getShallow(key);
				section.setValue((Boolean) newValue);
				serviceSettings.set(key, section);
				saveSettings(pnSmsInternetServiceConfigure, serviceSettings, section.getDependencies());
			} else if (properties.getShallow(key) instanceof OptionalRadioSection) {
				OptionalRadioSection section = (OptionalRadioSection) properties.getShallow(key);
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
					log.error("Could not get values from enum.", t);
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
		if (clazz.isAnnotationPresent(ConfigurableServiceProperties.class)) {
			ConfigurableServiceProperties provider = clazz.getAnnotation(ConfigurableServiceProperties.class);
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
		if (clazz.isAnnotationPresent(ConfigurableServiceProperties.class)) {
			ConfigurableServiceProperties provider = clazz.getAnnotation(ConfigurableServiceProperties.class);
			if (provider != null && !provider.name().equals("")) {
				ret = provider.name();
			}
		}
		return ret;
	}
	
	@Override
	public Object getSectionNode() {
		return createSectionNode(title, this, icon);
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public void save() {}
	
	public List<FrontlineValidationMessage> validateFields() {
		return null;
	}
	
	public void notify(FrontlineEventNotification notification) {
	if (notification instanceof DatabaseEntityNotification<?>
			&& !(notification instanceof EntityDeleteWarning<?>)) {
		Object entity = ((DatabaseEntityNotification<?>) notification).getDatabaseEntity();
		if(entity instanceof PersistableSettings
				&& ((PersistableSettings) entity).getServiceTypeSuperclass().equals(getServiceSupertype())) {
			this.refresh();
		}
	} else if (notification instanceof UiDestroyEvent) {
		if(((UiDestroyEvent) notification).isFor(this.ui)) {
			this.ui.getFrontlineController().getEventBus().unregisterObserver(this);
		}
	}
}
}