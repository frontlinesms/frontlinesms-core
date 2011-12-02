/**
 * 
 */
package net.frontlinesms.data.domain;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.serviceconfig.OptionalRadioSection;
import net.frontlinesms.serviceconfig.OptionalSection;
import net.frontlinesms.serviceconfig.PasswordString;
import net.frontlinesms.serviceconfig.PhoneSection;

/**
 * Wraps a {@link String} value for a property in a persistable {@link Entity}.
 * @author Alex
 */
@Entity
public class PersistableSettingValue {
//> STATIC CONSTANTS

//> INSTANCE PROPERTIES
	/** Unique id for this entity.  This is for hibernate usage. */
	@SuppressWarnings("unused")
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(unique=true,nullable=false,updatable=false)
	private long id;
	/** the value of this String */
	private String value;

//> CONSTRUCTORS
	/** Empty default constructor for Hibernate. */
	@SuppressWarnings("unused")
	private PersistableSettingValue() {}
	
	/**
	 * Creates a new {@link PersistableSettingValue}.
	 * @param value The value for {@link #value}.
	 */
	public PersistableSettingValue(String value) {
		this.value = value;
	}

//> ACCESSORS
	/** @return {@link #value} */
	public String getValue() {
		return this.value;
	}

//> INSTANCE HELPER METHODS
	/**
	 * @param exampleValue 
	 * @param value 
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object toObject(Object exampleValue) {
		String stringValue = value;
		if (exampleValue.getClass().equals(String.class))
			return stringValue;
		if (exampleValue.getClass().equals(Boolean.class))
			return Boolean.parseBoolean(stringValue);
		if (exampleValue.getClass().equals(Integer.class))
			return Integer.parseInt(stringValue);
		if (exampleValue.getClass().equals(Long.class))
			return Long.parseLong(stringValue);
		if (exampleValue.getClass().equals(BigDecimal.class))
			return new BigDecimal(stringValue);
		if (exampleValue.getClass().equals(PasswordString.class))
			return new PasswordString(FrontlineUtils.decodeBase64(stringValue));
		if (exampleValue.getClass().equals(OptionalSection.class)) {
			return Boolean.parseBoolean(stringValue);
		}
		if (exampleValue.getClass().equals(PhoneSection.class))
			return new PhoneSection(stringValue);
		if (exampleValue.getClass().equals(OptionalRadioSection.class)) {
			try {
				OptionalRadioSection section = (OptionalRadioSection) exampleValue;
				Method getValueOf = section.getValue().getClass().getMethod("valueOf", String.class);
				Enum enumm = (Enum) getValueOf.invoke(null, stringValue);
				return new OptionalRadioSection(enumm);
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		}
		try {
			if (exampleValue.getClass().isEnum()) {
				Method getValueOf = exampleValue.getClass().getMethod("valueOf", String.class);
				Enum enumm = (Enum) getValueOf.invoke(null, stringValue);
				return enumm;
			}
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
		throw new RuntimeException("Unsupported property type: " + exampleValue.getClass());
	}

	// TODO implement .equals() and .hashcode().  May need to watch out as 2 different instances may have the same value.  For this reason, might be safer not to implement for now.
	
//> STATIC FACTORIES
	/**
	 * Converts the supplied property value to the string representation of it. 
	 * @param value
	 * @return
	 */
	public static PersistableSettingValue create(Object value) {
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

//> STATIC HELPER METHODS
	
}
