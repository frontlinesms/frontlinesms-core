/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;


import net.frontlinesms.data.domain.SmsModemSettings;
import net.frontlinesms.data.repository.SmsModemSettingsDao;
import net.frontlinesms.junit.HibernateTestCase;

import org.springframework.beans.factory.annotation.Required;

/**
 * Test class for {@link HibernateSmsModemSettingsDao}
 * @author Alex
 */
public class HibernateSmsModemSettingsDaoTest extends HibernateTestCase {
//> CONSTANTS
	private static final String SERIAL_ONE = "Serial ONE";
	
	private static final String SERIAL_TWO = "Serial TWO";
	
//> INSTANCE PROPERTIES
	/** Instance of this DAO implementation we are testing. */
	private SmsModemSettingsDao dao;

//> TEST METHODS
	/**
	 * Test everything all at once!
	 */
	public void test() {
		//changes to allow extra method setters used smsc number
		//SmsModemSettings settingsOne = new SmsModemSettings(SERIAL_ONE, "Manufacturer", "Model", true, false, true, false);
		SmsModemSettings settingsOne = new SmsModemSettings(SERIAL_ONE);
		settingsOne.setManufacturer("Manufacturer");
		settingsOne.setModel("Model");
		settingsOne.setUseForSending(true);
		settingsOne.setUseForReceiving(false);
		settingsOne.setDeleteMessagesAfterReceiving(true);
		settingsOne.setUseDeliveryReports(false);
		
		assertNull(dao.getSmsModemSettings(SERIAL_ONE));
		
		dao.saveSmsModemSettings(settingsOne);
		
		assertEquals(settingsOne, dao.getSmsModemSettings(SERIAL_ONE));

		SmsModemSettings settingsTwo = new SmsModemSettings( SERIAL_TWO);
		settingsTwo.setManufacturer("Manufacturer");
		settingsTwo.setModel("Model");
		settingsTwo.setUseForSending(false);
		settingsTwo.setUseForReceiving(true);
		settingsTwo.setDeleteMessagesAfterReceiving(false);
		settingsTwo.setUseDeliveryReports(true);
		
		assertNull(dao.getSmsModemSettings(SERIAL_TWO));
		
		dao.saveSmsModemSettings(settingsTwo);

		assertEquals(settingsOne, dao.getSmsModemSettings(SERIAL_ONE));
		assertEquals(settingsTwo, dao.getSmsModemSettings(SERIAL_TWO));

		SmsModemSettings settingsOneFetched = dao.getSmsModemSettings(SERIAL_ONE);
		assertEquals(settingsOne, settingsOneFetched);
		SmsModemSettings settingsTwoFetched = dao.getSmsModemSettings(SERIAL_TWO);
		assertEquals(settingsTwo, settingsTwoFetched);

		assertTrue(settingsOne.useForSending());
		settingsOne.setUseForSending(false);
		dao.updateSmsModemSettings(settingsOne);
		settingsOne = dao.getSmsModemSettings(SERIAL_ONE);
		assertFalse(settingsOne.useForSending());
		
		final int expectedSettingsCount = 2;
		assertEquals(expectedSettingsCount, dao.getCount());
	}
	
//> ACCESSORS
	/** @param d The DAO to use for the test. */
	@Required
	public void setSmsModemSettingsDao(SmsModemSettingsDao d) {
		// we can just set the DAO once in the test
		this.dao = d;
	}
}
