/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;

import net.frontlinesms.junit.HibernateTestCase;
import net.frontlinesms.messaging.sms.internet.ClickatellInternetService;

import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.domain.PersistableSettings;
import net.frontlinesms.data.repository.SmsInternetServiceSettingsDao;

import org.springframework.beans.factory.annotation.Required;

/**
 * Test class for {@link HibernateSmsInternetServiceSettingsDao}
 * @author Alex
 */
public class HibernateSmsInternetServiceSettingsDaoTest extends HibernateTestCase {
//> PROPERTIES
	/** Instance of this DAO implementation we are testing. */
	private SmsInternetServiceSettingsDao dao;

//> TEST METHODS
	/**
	 * Test everything all at once!
	 * @throws DuplicateKeyException 
	 */
	public void test() throws DuplicateKeyException {
		assertEquals(0, dao.getServiceAccounts().size());
		
		ClickatellInternetService clickatell = new ClickatellInternetService();
		PersistableSettings settings = new PersistableSettings(clickatell);
		
		dao.saveServiceSettings(settings);

		assertEquals(1, dao.getServiceAccounts().size());
		
		dao.deleteServiceSettings(settings);
		
		assertEquals(0, dao.getServiceAccounts().size());
	}

//> ACCESSORS
	/** @param d The DAO to use for the test. */
	@Required
	public void setSmsInternetServiceSettingsDao(SmsInternetServiceSettingsDao d) {
		this.dao = d;
	}
}
