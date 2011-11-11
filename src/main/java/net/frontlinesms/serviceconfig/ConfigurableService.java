package net.frontlinesms.serviceconfig;

import net.frontlinesms.data.domain.PersistableSettings;

public interface ConfigurableService {
	StructuredProperties getPropertiesStructure();
	PersistableSettings getSettings();
	void setSettings(PersistableSettings settings);
	Class<? extends ConfigurableService> getSuperType();
}
