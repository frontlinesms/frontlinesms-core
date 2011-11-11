package net.frontlinesms.data;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.domain.PersistableSettingValue;
import net.frontlinesms.data.domain.PersistableSettings;
import net.frontlinesms.messaging.sms.properties.OptionalRadioSection;
import net.frontlinesms.messaging.sms.properties.OptionalSection;

public class StructuredProperties {
	private static final Logger LOG = FrontlineUtils.getLogger(StructuredProperties.class);
	
	private Map<String, Object> properties = new LinkedHashMap<String, Object>();
	
	public void loadPropertiesFromDbIntoStructure(Map<String, PersistableSettingValue> dbProperties) {
		loadPropertiesFromDbIntoStructure(this, dbProperties);
	}
	
//> MAP METHODS
	public Object get(String key) {
		return this.properties.get(key);
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

//> STATIC HELPER METHODS
	/**
	 * @param properties
	 * @param dbProperties
	 */
	private static void loadPropertiesFromDbIntoStructure(StructuredProperties properties, Map<String, PersistableSettingValue> dbProperties) {
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
				loadPropertiesFromDbIntoStructure(section.getDependencies(), dbProperties);
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
						loadPropertiesFromDbIntoStructure(section.getDependencies(val), dbProperties);
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
