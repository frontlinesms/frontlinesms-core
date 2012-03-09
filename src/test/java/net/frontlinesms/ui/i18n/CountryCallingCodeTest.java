/**
 * 
 */
package net.frontlinesms.ui.i18n;

import java.util.Locale;

import net.frontlinesms.junit.BaseTestCase;

import static net.frontlinesms.ui.i18n.CountryCallingCode.*;

/**
 * Unit tests for {@link CountryCallingCode}.
 * @author Alex Anderson <alex@frontlinesms.com>
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public class CountryCallingCodeTest extends BaseTestCase {
	public void testValidity() {
		for(CountryCallingCode code : CountryCallingCode.values()) {
			assertTrue("Country code was made of characters other than digits: " + code + " (+" + code.getCountryCode() + "))",
					code.getCountryCode().matches("\\d+"));
		}
	}
	
	public void testIsInInternationalFormat() {
		assertTrue(CountryCallingCode.isInInternationalFormat("+15559999"));
		assertTrue(CountryCallingCode.isInInternationalFormat("+336123456789"));
		assertTrue(CountryCallingCode.isInInternationalFormat("+447762258741"));
		
		assertFalse(CountryCallingCode.isInInternationalFormat("0612215656"));
		assertFalse(CountryCallingCode.isInInternationalFormat("00336123456"));
		assertFalse(CountryCallingCode.isInInternationalFormat("+1-(555)-9999"));
		assertFalse(CountryCallingCode.isInInternationalFormat("+44(0)7762975852"));
	}
	
	public void testFormat() {
		assertEquals("+15559999", CountryCallingCode.format("1-(555)-9999", Locale.US.getCountry()));
		assertEquals("+44712345678", CountryCallingCode.format("0712345678", Locale.UK.getCountry()));
		assertEquals("+15559999", CountryCallingCode.format("555-9999", Locale.US.getCountry()));
		assertEquals("+336123456789", CountryCallingCode.format("06123456789", Locale.FRANCE.getCountry()));
	}
	
//> SPECIFIC COUNTRY TESTS
	private static final Object[] TEST_CASES = {
		// COUNTRY | INTERNATIONAL? | VALID? | phone number
		US,          false,           true,    "3038181234",
		US,          false,           false,   "303818123",
		US,          false,           false,   "30381812345",
		US,          true,            true,    "+13038181234",
		US,          true,            false,   "+1303818123",
		US,          true,            false,   "+130381812345",

		GB,          false,           true,    "07890123456",
		GB,          false,           false,   "0789012345",
		GB,          false,           false,   "078901234567",
		GB,          true,            true,    "+447890123456",
		GB,          true,            false,   "+44789012345",
		GB,          true,            false,   "+4478901234567",

		KE,          false,           true,    "0789012345",
		KE,          false,           false,   "078901234",
		KE,          false,           false,   "07890123456",
		KE,          true,            true,    "+254789012345",
		KE,          true,            false,   "+25478901234",
		KE,          true,            false,   "+2547890123456",
	};
	
	public void testByCountry() {
		for(int i=0; i<TEST_CASES.length; i+=4) {
			CountryCallingCode country = (CountryCallingCode) TEST_CASES[i];
			boolean international = (Boolean) TEST_CASES[i+1];
			boolean valid = (Boolean) TEST_CASES[i+2];
			String phoneNumber = (String) TEST_CASES[i+3];
			
			assertEquals("Number: " + phoneNumber +
					"; for country " + country +
					"; international? " + international,
					valid,
					international? CountryCallingCode.isValidInternationalNumber(phoneNumber): country.isValidLocalNumber(phoneNumber));
		}
	}
}
