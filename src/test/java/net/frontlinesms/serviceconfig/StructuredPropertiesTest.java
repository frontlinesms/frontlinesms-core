/**
 * 
 */
package net.frontlinesms.serviceconfig;

import net.frontlinesms.data.domain.SmsInternetServiceSettingsDaoTest.Test;
import net.frontlinesms.junit.BaseTestCase;
import net.frontlinesms.messaging.sms.internet.AbstractSmsInternetService;
import net.frontlinesms.serviceconfig.OptionalRadioSection;
import net.frontlinesms.serviceconfig.OptionalSection;
import net.frontlinesms.serviceconfig.PasswordString;
import net.frontlinesms.serviceconfig.PhoneNumber;
import net.frontlinesms.serviceconfig.StructuredProperties;

/**
 * Tests the various methods dealing with {@link AbstractSmsInternetService}'s properties classes.
 * 
 * 
 * @author Alex
 * @author Carlos Eduardo Genz
 */
public class StructuredPropertiesTest extends BaseTestCase {
	private StructuredProperties defaultSettings = new StructuredProperties();
	private StructuredProperties expectedValues = new StructuredProperties();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		// TODO might be nicer to have e.g. addDefaultSettings() method, which adds to both defaultSettings and expectedValues
		String key = "a";
		defaultSettings.put(key, "");
		expectedValues.put(key, defaultSettings.getShallow(key));
		
		defaultSettings.put(key = "b", new PasswordString(""));
		expectedValues.put(key, defaultSettings.getShallow(key));
		
		defaultSettings.put(key = "c", new PhoneNumber(""));
		expectedValues.put(key, defaultSettings.getShallow(key));
		
		defaultSettings.put(key = "d", Boolean.FALSE);
		expectedValues.put(key, defaultSettings.getShallow(key));
		
		defaultSettings.put(key = "e", 997);
		expectedValues.put(key, defaultSettings.getShallow(key));
		
		OptionalRadioSection<Test> a = new OptionalRadioSection<Test>(Test.A);
		String objj = "";
		a.addDependency(Test.A, key = "f", objj);
		expectedValues.put(key, objj);
		
		PasswordString obj = new PasswordString("");
		a.addDependency(Test.A, key = "g", obj);
		expectedValues.put(key, obj);
		
		PhoneNumber obj2 = new PhoneNumber("");
		a.addDependency(Test.B, key = "h", obj2);
		expectedValues.put(key, obj2);
		
		defaultSettings.put(key = "i", a);
		expectedValues.put(key, defaultSettings.getShallow(key));
		
		OptionalSection section = new OptionalSection();
		section.setValue(true);
		section.addDependency(key = "j", objj);
		expectedValues.put(key, objj);
		
		section.addDependency(key = "k", obj);
		expectedValues.put(key, obj);
		
		defaultSettings.put(key = "l", section);
		expectedValues.put(key, defaultSettings.getShallow(key));
	}
	
	/**
	 * Unit tests for {@link AbstractSmsInternetService#getValue(String, java.util.LinkedHashMap)}.
	 * It does test deeper levels included in {@link OptionalRadioSection}s and {@link OptionalSection}s.
	 */
	public void testGetValue() {
		for (String key : expectedValues.keySet()) {
			Object obj = defaultSettings.getDeep(key);
			assertEquals("Checking get value for key '" + key + "'", obj, expectedValues.getShallow(key)); 
		}
		String invalidKey = "invalidKey";
		
		// Invalid key
		assertNull("Checking get value from null map", defaultSettings.getDeep(invalidKey));
	}
}
