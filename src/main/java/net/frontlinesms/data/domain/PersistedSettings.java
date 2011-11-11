/**
 * 
 */
package net.frontlinesms.data.domain;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.*;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.ConfigurableService;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;
import net.frontlinesms.messaging.sms.properties.OptionalRadioSection;
import net.frontlinesms.messaging.sms.properties.OptionalSection;
import net.frontlinesms.messaging.sms.properties.PasswordString;
import net.frontlinesms.messaging.sms.properties.PhoneSection;

/**
 * Class encapsulating settings of a {@link SmsInternetService}.
 * @author Alex
 */
@Entity(name="SmsInternetServiceSettings")
public class PersistedSettings {
//> INSTANCE PROPERTIES
	/** Unique id for this entity.  This is for hibernate usage. */
	@SuppressWarnings("unused")
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(unique=true,nullable=false,updatable=false)
	private long id;
	/** The name of the class of the {@link SmsInternetService} these settings apply to. */
	private String serviceClassName;
	/** The class of service that these settings apply to.  Default must be SmsInternetService for backward compatibility. */
	@SuppressWarnings("unused")
	private String serviceTypeSuperclass = SmsInternetService.class.getSimpleName();
	/** The properties for a {@link SmsInternetService} */
	@OneToMany(targetEntity=PersistedSettingValue.class, fetch=FetchType.EAGER, cascade=CascadeType.ALL)
	private final Map<String, PersistedSettingValue> properties = new HashMap<String, PersistedSettingValue>();
	
//> CONSTRUCTORS
	/** Empty constructor for hibernate */
	PersistedSettings() {}
	
	/**
	 * Create a new instance of service settings for the supplied service.
	 * @param service
	 */
	public PersistedSettings(ConfigurableService service) {
		this.serviceTypeSuperclass = service.getSuperType().getSimpleName();
		this.serviceClassName = service.getClass().getCanonicalName();
	}
	
//> ACCESSOR METHODS
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
	public PersistedSettingValue get(String key) {
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
	public Map<String, PersistedSettingValue> getProperties() {
		return this.properties;
	}
	

//> STATIC HELPER METHODS
	/**
	 * Converts the supplied property value to the string representation of it. 
	 * @param value
	 * @return
	 * TODO move to {@link PersistedSettingValue}
	 */
	public static PersistedSettingValue toValue(Object value) {
		String stringValue;
		if (value instanceof String) stringValue = (String)value;
		else if (value instanceof Boolean) stringValue = Boolean.toString((Boolean)value);
		else if (value instanceof Integer) stringValue = Integer.toString((Integer)value);
		else if (value instanceof PasswordString) stringValue = FrontlineUtils.encodeBase64(((PasswordString)value).getValue());
		else if (value instanceof OptionalSection) stringValue = Boolean.toString(((OptionalSection)value).getValue());
		else if (value instanceof Enum<?>) stringValue = ((Enum<?>)value).name();
		else if (value instanceof PhoneSection) stringValue = ((PhoneSection)value).getValue();
		else if (value instanceof OptionalRadioSection<?>) {
			OptionalRadioSection<?> ors = (OptionalRadioSection<?>) value;
			stringValue = ors.getValue().name();
		}
		else throw new RuntimeException("Unsupported property type: " + value.getClass());
		
		return new PersistedSettingValue(stringValue);
	}

	/**
	 * Gets a property value from a string, and the canonical name of that class.
	 * @param property 
	 * @param value 
	 * @return
	 * TODO move to {@link PersistedSettingValue}
	 */
	@SuppressWarnings("unchecked")
	public static Object fromValue(Object property, PersistedSettingValue value) {
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
		PersistedSettings other = (PersistedSettings) obj;
		if (serviceClassName == null) {
			if (other.serviceClassName != null)
				return false;
		} else if (!serviceClassName.equals(other.serviceClassName))
			return false;
		return true;
	}
}