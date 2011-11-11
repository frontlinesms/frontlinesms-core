package net.frontlinesms.data;

import net.frontlinesms.data.domain.PersistedSettings;

public interface ConfigurableService {
	StructuredProperties getPropertiesStructure();
	PersistedSettings getSettings();
	void setSettings(PersistedSettings settings);
	Class<? extends ConfigurableService> getSuperType();
}
