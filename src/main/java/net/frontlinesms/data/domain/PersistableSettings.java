/**
 * 
 */
package net.frontlinesms.data.domain;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.*;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;
import net.frontlinesms.serviceconfig.ConfigurableService;
import net.frontlinesms.serviceconfig.OptionalRadioSection;
import net.frontlinesms.serviceconfig.OptionalSection;
import net.frontlinesms.serviceconfig.PasswordString;
import net.frontlinesms.serviceconfig.PhoneSection;
import net.frontlinesms.serviceconfig.StructuredProperties;

/**
 * Class encapsulating settings of a {@link SmsInternetService}.
 * @author Alex
 */
@Entity(name="SmsInternetServiceSettings")
public class PersistableSettings {
//> INSTANCE PROPERTIES
	/** Unique id for this entity.  This is for hibernate usage. */
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(unique=true,nullable=false,updatable=false)
	private long id;
	/** The name of the class of the {@link SmsInternetService} these settings apply to. */
	private String serviceClassName;
	/** The class of service that these settings apply to.  Default must be SmsInternetService for backward compatibility. */
	@SuppressWarnings("unused")
	private String serviceTypeSuperclass = SmsInternetService.class.getSimpleName();
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
		this.serviceTypeSuperclass = service.getSuperType().getSimpleName();
		this.serviceClassName = service.getClass().getCanonicalName();
	}
	
//> ACCESSOR METHODS
	/** @return the database ID of these settings. */
	public long getId() {
		return id;
	}
	
	/**
	 * Sets the value of a setting.
	 * FIXME value should not just be an OBJECT - some interface at least i would expect!
	 * @param key The key of the property to save
	 * @param value The value of the property to save
	 */
	public void set(String key, Object value) {
		this.properties.put(key, toValue(value));
	}
	
	/**
	 * @param key the key of the property to fetch
	 * @return the value stored for the supplied key, or <code>null</code> if no value is stored.
	 */
	public PersistableSettingValue get(String key) {
		return this.properties.get(key);
	}
	
	/** @return the class name of {@link SmsInternetService} implementation that these settings apply to */
	public String getServiceClassName() {
		return this.serviceClassName;
	}
	
	/**
	 * Get an ordered list of the properties set on this object.
	 * @return
	 */
	public Map<String, PersistableSettingValue> getProperties() {
		return this.properties;
	}
	

//> STATIC HELPER METHODS
	/**
	 * Converts the supplied property value to the string representation of it. 
	 * @param value
	 * @return
	 * TODO move to {@link PersistableSettingValue}
	 */
	public static PersistableSettingValue toValue(Object value) {
		String stringValue;
		if (value instanceof String) stringValue = (String)value;
		else if (value instanceof Boolean) stringValue = Boolean.toString((Boolean) value);
		else if (value instanceof Integer) stringValue = Integer.toString((Integer) value);
		else if (value instanceof Long) stringValue = Long.toString((Long) value);
		else if (value instanceof BigDecimal) stringValue = ((BigDecimal) value).toString();
		else if (value instanceof PasswordString) stringValue = FrontlineUtils.encodeBase64(((PasswordString)value).getValue());
		else if (value instanceof OptionalSection) stringValue = Boolean.toString(((OptionalSection)value).getValue());
		else if (value instanceof Enum<?>) stringValue = ((Enum<?>)value).name();
		else if (value instanceof PhoneSection) stringValue = ((PhoneSection)value).getValue();
		else if (value instanceof OptionalRadioSection<?>) {
			OptionalRadioSection<?> ors = (OptionalRadioSection<?>) value;
			stringValue = ors.getValue().name();
		}
		else throw new RuntimeException("Unsupported property type: " + value.getClass());
		
		return new PersistableSettingValue(stringValue);
	}

	/**
	 * Gets a property value from a string, and the canonical name of that class.
	 * @param property 
	 * @param value 
	 * @return
	 * TODO move to {@link PersistableSettingValue}
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object fromValue(Object property, PersistableSettingValue value) {
		String stringValue = value.getValue();
		if (property.getClass().equals(String.class))
			return stringValue;
		if (property.getClass().equals(Boolean.class))
			return Boolean.parseBoolean(stringValue);
		if (property.getClass().equals(Integer.class))
			return Integer.parseInt(stringValue);
		if (property.getClass().equals(PasswordString.class))
			return new PasswordString(FrontlineUtils.decodeBase64(stringValue));
		if (property.getClass().equals(OptionalSection.class)) {
			return Boolean.parseBoolean(stringValue);
		}
		if (property.getClass().equals(PhoneSection.class))
			return new PhoneSection(stringValue);
		if (property.getClass().equals(OptionalRadioSection.class)) {
			try {
				OptionalRadioSection section = (OptionalRadioSection) property;
				Method getValueOf = section.getValue().getClass().getMethod("valueOf", String.class);
				Enum enumm = (Enum) getValueOf.invoke(null, stringValue);
				return new OptionalRadioSection(enumm);
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}
		try {
			if (property.getClass().isEnum()) {
				Method getValueOf = property.getClass().getMethod("valueOf", String.class);
				Enum enumm = (Enum) getValueOf.invoke(null, stringValue);
				return enumm;
			}
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
		throw new RuntimeException("Unsupported property type: " + property.getClass());
	}
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
		else return (T) PersistableSettings.fromValue(defaultValue, setValue);
	}

//> GENERATED METHODS
	/** @see java.lang.Object#hashCode() */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((serviceClassName == null) ? 0 : serviceClassName.hashCode());
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
		if (serviceClassName == null) {
			if (other.serviceClassName != null)
				return false;
		} else if (!serviceClassName.equals(other.serviceClassName))
			return false;
		return true;
	}
}