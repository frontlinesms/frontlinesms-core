/**
 * 
 */
package net.frontlinesms.data.domain;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.*;

import net.frontlinesms.messaging.sms.internet.SmsInternetService;
import net.frontlinesms.serviceconfig.ConfigurableService;
import net.frontlinesms.serviceconfig.StructuredProperties;

/**
 * Class encapsulating settings of a {@link ConfigurableService}.
 * @author Alex
 */
@Entity
public class PersistableSettings {
//> INSTANCE PROPERTIES
	/** Unique id for this entity.  This is for hibernate usage. */
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(unique=true,nullable=false,updatable=false)
	private long id;
	/** The name class of the {@link ConfigurableService} these settings apply to. */
	private Class<? extends ConfigurableService> serviceClass;
	/** The class of service that these settings apply to. */
	private Class<? extends ConfigurableService> serviceTypeSuperclass;
	/** The properties for a {@link SmsInternetService} */
	@OneToMany(targetEntity=PersistableSettingValue.class, fetch=FetchType.EAGER, cascade=CascadeType.ALL)
	private final Map<String, PersistableSettingValue> properties = new HashMap<String, PersistableSettingValue>();
	
//> CONSTRUCTORS
	/** Empty constructor for hibernate */
	PersistableSettings() {}
	
	/**
	 * Create a new instance of service settings for the supplied service.
	 * @param service
	 */
	public PersistableSettings(ConfigurableService service) {
		this(service.getSuperType(), service.getClass());
	}
	
	public PersistableSettings(Class<? extends ConfigurableService> serviceTypeSuperclass,
			Class<? extends ConfigurableService> serviceClass) {
		this.serviceTypeSuperclass = serviceTypeSuperclass;
		this.serviceClass = serviceClass;
	}

//> ACCESSOR METHODS
	/** @return the database ID of these settings. */
	public long getId() {
		return id;
	}
	
	/**
	 * Sets the value of a setting.
	 * @param key The key of the property to save
	 * @param value The value of the property to save
	 */
	public void set(String key, Object value) {
		this.properties.put(key, PersistableSettingValue.create(value));
	}
	
	/**
	 * @param key the key of the property to fetch
	 * @return the value stored for the supplied key, or <code>null</code> if no value is stored.
	 */
	public PersistableSettingValue get(String key) {
		return this.properties.get(key);
	}
	
	/** @return the class name of {@link ConfigurableService} implementation that these settings apply to */
	public Class<? extends ConfigurableService> getServiceClass() {
		return this.serviceClass;
	}
	
	/** @return the superclass of the service that this implements, e.g. {@link SmsInternetService} */
	public Class<? extends ConfigurableService> getServiceTypeSuperclass() {
		return serviceTypeSuperclass;
	}
	
	/**
	 * Get an ordered list of the properties set on this object.
	 * @return
	 */
	public Map<String, PersistableSettingValue> getProperties() {
		return this.properties;
	}
	
	public StructuredProperties getStructuredProperties() {
		try {
			StructuredProperties structured = serviceClass.newInstance().getPropertiesStructure();
			structured.load(this.properties);
			return structured;
		} catch(Exception ex) {
			throw new RuntimeException("Unable to load structured properties.", ex);
		}
	}

//> STATIC HELPER METHODS
	/**
	 * @param key The key of the property
	 * @param clazz The class of the property's value
	 * @param <T> The class of the property's value
	 * @return The property value, either the one stored on db (if any) or the default value.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Object> T getPropertyValue(StructuredProperties defaults, PersistableSettings settings, 
			String key, Class<T> clazz) {
		T defaultValue = (T) defaults.getDeep(key);
		if (defaultValue == null) throw new IllegalArgumentException("No default value could be found for key: " + key);
		
		PersistableSettingValue setValue = settings.get(key);
		if(setValue == null) return defaultValue;
		else return (T) setValue.toObject(defaultValue);
	}

//> GENERATED METHODS
	/** @see java.lang.Object#hashCode() */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ serviceClass.hashCode();
		return result;
	}

	/** @see java.lang.Object#equals(java.lang.Object) */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PersistableSettings other = (PersistableSettings) obj;
		if (serviceClass == null) {
			if (other.serviceClass != null)
				return false;
		} else if (!serviceClass.equals(other.serviceClass))
			return false;
		return true;
	}
}