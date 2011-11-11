/**
 * 
 */
package net.frontlinesms.data.domain;

import java.util.*;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.domain.SmsInternetServiceSettings;
import net.frontlinesms.data.repository.SmsInternetServiceSettingsDao;
import net.frontlinesms.junit.BaseTestCase;
import net.frontlinesms.messaging.sms.properties.OptionalRadioSection;
import net.frontlinesms.messaging.sms.properties.OptionalSection;
import net.frontlinesms.messaging.sms.properties.PasswordString;
import net.frontlinesms.messaging.sms.properties.PhoneSection;

/**
 * Unit tests for {@link SmsInternetServiceSettingsDao}.
 * 
 * @author Kadu
 */
public class SmsInternetServiceSettingsTest extends BaseTestCase {
	private Map<Object, Class<?>> expectedTypes = new HashMap<Object, Class<?>>();
	private Map<Object, String> values = new HashMap<Object, String>();

	public enum Test { A,B }

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		// TODO might be cleaner to have e.g. addValue(String key, String value, Class clazz)
		Object obj = "String";
		expectedTypes.put(obj, String.class);
		values.put(obj, String.valueOf(obj));

		obj = Boolean.TRUE;
		expectedTypes.put(obj, Boolean.class);
		values.put(obj, String.valueOf(obj));

		obj = 997;
		expectedTypes.put(obj, Integer.class);
		values.put(obj, String.valueOf(obj));

		obj = new PasswordString("test");
		expectedTypes.put(obj, PasswordString.class);
		values.put(obj, FrontlineUtils.encodeBase64("test"));

		OptionalSection o = new OptionalSection();
		o.setValue(Boolean.TRUE);
		expectedTypes.put(o, Boolean.class);
		values.put(o, String.valueOf(o.getValue()));

		obj = new PhoneSection("phone");
		expectedTypes.put(obj, PhoneSection.class);
		values.put(obj, "phone");

		obj = new OptionalRadioSection<Test>(Test.A);
		expectedTypes.put(obj, OptionalRadioSection.class);
		values.put(obj, Test.A.toString());

		// TODO add deeper levels to OptionalSection and OptionalRadioSection
		// Kadu: I don't think we do need deeper levels.. I'll leave to discuss with you later.
		
		obj = Test.A;
		expectedTypes.put(obj, Test.class);
		values.put(obj, String.valueOf(obj));
	}

	/**
	 * Unit tests for {@link SmsInternetServiceSettingsFactory#getValueFromString(Object, String)}.
	 */
	public void testGetValueFromString() {
		for (Object obj : expectedTypes.keySet()) {
			Object value = SmsInternetServiceSettings.fromValue(obj, new PersistedSettingValue(values.get(obj)));
			assertEquals("Checking get value from string for class [" + obj.getClass() + "] and value [" + values.get(obj) + "]", value.getClass(), expectedTypes.get(obj));
		}
	}

	/**
	 * Unit tests for {@link SmsInternetServiceSettingsFactory#toValue(Object)}.
	 */
	public void testGetValueAsString() {
		for (Object obj : expectedTypes.keySet()) {
			String ret = SmsInternetServiceSettings.toValue(obj).getValue();
			assertEquals("Checking get value as string for obj [" + obj + "], class [" + obj.getClass() + "]", ret, values.get(obj));
		}
	}

	@Override
	protected void tearDown() throws Exception {
		expectedTypes.clear();
		values.clear();
	}
}
