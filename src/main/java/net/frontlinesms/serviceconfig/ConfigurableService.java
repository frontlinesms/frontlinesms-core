package net.frontlinesms.serviceconfig;

import net.frontlinesms.data.domain.PersistableSettings;

public interface ConfigurableService {
	/**
	 * @return the structure of user-configured properties for this service.
	 * N.B. other properties may be set in the service's {@link PersistableSettings},
	 * but these will not be available for the user to edit directly in generated UIs
	 */
	StructuredProperties getPropertiesStructure();
	
	/** @return the current settings of this service */ 
	PersistableSettings getSettings();
	
	/**
	 * Sets the settings of this properties.  If the service is already running,
	 * behaviour of this method is (currently) undefined.
	 */
	void setSettings(PersistableSettings settings);
	
	/** @return the type of {@link ConfigurableService} that this class extends. */
	Class<? extends ConfigurableService> getSuperType();
}
