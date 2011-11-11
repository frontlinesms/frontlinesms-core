package net.frontlinesms.serviceconfig;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.domain.PersistableSettingValue;
import net.frontlinesms.data.domain.PersistableSettings;

public class StructuredProperties {
	private static final Logger LOG = FrontlineUtils.getLogger(StructuredProperties.class);
	
	private Map<String, Object> properties = new LinkedHashMap<String, Object>();
	
//> MAP METHODS
	public Object getShallow(String key) {
		return this.properties.get(key);
	}
	
	/**
	 * Deep-searches nested maps for a propertt's value.  Maps may be nested as values
	 * inside other maps by wrapping them in either an {@link OptionalSection} or an
	 * {@link OptionalRadioSection}.
	 * @param key
	 * @param map
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object getDeep(String key) {
		if (properties.containsKey(key)) {
			return properties.get(key);
		} else {
			for(Object mapValue : properties.values()) {
				if(mapValue instanceof OptionalSection) {
					Object value = ((OptionalSection) mapValue).getDependencies().getDeep(key);
					if(value != null) return value;
				} else if(mapValue instanceof OptionalRadioSection) {
					Collection<StructuredProperties> dependencies = ((OptionalRadioSection) mapValue).getAllDependencies();
					for(StructuredProperties dependencyMap : dependencies) {
						Object value = dependencyMap.getDeep(key);
						if(value != null) return value;
					}
				}
			}
		}
		return null;
	}

	public Set<String> keySet() {
		return this.properties.keySet();
	}

	public void put(String property, Object propValue) {
		this.properties.put(property, propValue);
	}

	public boolean containsKey(String key) {
		return this.properties.containsKey(key);
	}

	public Collection<Object> values() {
		return this.properties.values();
	}

	/**
	 * @param properties
	 * @param dbProperties
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void load(Map<String, PersistableSettingValue> dbProperties) {
		Map<String, Object> toUpdate = new LinkedHashMap<String, Object>();
		for (String key : properties.keySet()) {
			Object value = properties.get(key);
			if (properties.get(key) instanceof OptionalSection) {
				OptionalSection section = (OptionalSection) value;
				value = section.getValue();
				if (dbProperties.containsKey(key)) {
					value = PersistableSettings.fromValue(section, dbProperties.get(key));
				}
				section.setValue((Boolean) value);
				section.getDependencies().load(dbProperties);
				toUpdate.put(key, section);
			} else if (properties.get(key) instanceof OptionalRadioSection) {
				OptionalRadioSection section = (OptionalRadioSection) value;
				value = section.getValue();
				if (dbProperties.containsKey(key)) {
					OptionalRadioSection tmp = (OptionalRadioSection) PersistableSettings.fromValue(section, dbProperties.get(key));
					section.setValue(tmp.getValue());
					value = section.getValue();
				}
				Enum enumm = (Enum) value;
				section.setValue(enumm);
				try {
					Method getValues = enumm.getClass().getMethod("values");
					Enum[] vals = (Enum[]) getValues.invoke(null);
					for (Enum val : vals) {
						section.getDependencies(val).load(dbProperties);
					}
				} catch (Throwable t) {
					LOG.error("Could not get values from enum.", t);
				}
				toUpdate.put(key, section);
			} else {
				if (dbProperties.containsKey(key)) {
					value = PersistableSettings.fromValue(value, dbProperties.get(key));
					toUpdate.put(key, value);
				}
			}
		}
		for (String key : toUpdate.keySet()) {
			properties.put(key, toUpdate.get(key));
		}
	}
}
