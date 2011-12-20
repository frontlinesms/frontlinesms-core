/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;

import net.frontlinesms.junit.HibernateTestCase;
import net.frontlinesms.listener.SmsListener;
import net.frontlinesms.messaging.sms.internet.ClickatellInternetService;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;
import net.frontlinesms.messaging.sms.internet.SmsInternetServiceStatus;
import net.frontlinesms.serviceconfig.ConfigurableService;
import net.frontlinesms.serviceconfig.StructuredProperties;

import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.data.domain.PersistableSettings;
import net.frontlinesms.data.repository.SmsInternetServiceSettingsDao;
import net.frontlinesms.events.EventBus;

import org.springframework.beans.factory.annotation.Required;

/**
 * Test class for {@link HibernateSmsInternetServiceSettingsDao}
 * @author Alex
 */
public class HibernateSmsInternetServiceSettingsDaoTest extends HibernateTestCase {
//> PROPERTIES
	/** Instance of this DAO implementation we are testing. */
	private HibernateSmsInternetServiceSettingsDao dao;

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
	
	public void testGetByProperty_noMatches() {
		// when
		PersistableSettings s = dao.getByProperty("whatever", "???");
		
		// then
		assertNull(s);
	}
	
	public void testGetByProperty_matches() throws Exception {
		// given
		createWithProperties(
				"test-property-1", "abc",
				"test-property-2", "def");
		PersistableSettings settings2 = createWithProperties(
				"test-property-1", "xyz",
				"test-property-2", "def");
		createWithProperties(
				"test-property-1", "def",
				"test-property-2", "xyz");
		createWithProperties(
				"abc", "test-property-1",
				"xyz", "test-property-2");
		
		// when
		PersistableSettings s = dao.getByProperty("test-property-1", "xyz");
		
		// then
		assertEquals(settings2.getId(), s.getId());
	}

	private PersistableSettings createWithProperties(String... propertyNamesAndValues) throws DuplicateKeyException {
		if((propertyNamesAndValues.length & 1) != 0) throw new IllegalArgumentException("Need value for every property!");
		PersistableSettings s = new PersistableSettings(SmsInternetService.class, FakeInternetService.class);
		for (int i = 0; i < propertyNamesAndValues.length; i+=2) {
			s.set(propertyNamesAndValues[i], propertyNamesAndValues[i+1]);
		}
		dao.save(s);
		return s;
	}

//> ACCESSORS
	/** @param d The DAO to use for the test. */
	@Required
	public void setSmsInternetServiceSettingsDao(SmsInternetServiceSettingsDao d) {
		this.dao = (HibernateSmsInternetServiceSettingsDao) d;
	}
}

class FakeInternetService implements SmsInternetService {

	public void setUseForSending(boolean use) {
		// TODO Auto-generated method stub
		
	}

	public boolean supportsReceive() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setUseForReceiving(boolean use) {
		// TODO Auto-generated method stub
		
	}

	public void sendSMS(FrontlineMessage outgoingMessage) {
		// TODO Auto-generated method stub
		
	}

	public boolean isBinarySendingSupported() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isUcs2SendingSupported() {
		// TODO Auto-generated method stub
		return false;
	}

	public String getStatusDetail() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getServiceName() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getServiceIdentification() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isUseForReceiving() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isUseForSending() {
		// TODO Auto-generated method stub
		return false;
	}

	public String getDisplayPort() {
		// TODO Auto-generated method stub
		return null;
	}
	public Class<? extends ConfigurableService> getSuperType() {
		return null;
	}
	public String getIdentifier() {
		return null;
	}
	public PersistableSettings getSettings() {
		return null;
	}
	public void setSettings(PersistableSettings settings) {}
	public void setSmsListener(SmsListener smsListener) {}
	public void setEventBus(EventBus eventBus) {}
	public boolean isConnected() {
		return false;
	}
	public void startService() {}
	public void restartService() {}
	public void stopService() {}
	public boolean isEncrypted() {
		return false;
	}
	public String getMsisdn() {
		return null;
	}
	public StructuredProperties getPropertiesStructure() {
		return null;
	}
	public SmsInternetServiceStatus getStatus() {
		return null;
	}
}
