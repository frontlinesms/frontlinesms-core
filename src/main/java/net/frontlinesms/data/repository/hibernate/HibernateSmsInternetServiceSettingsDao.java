/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;

import net.frontlinesms.data.repository.SmsInternetServiceSettingsDao;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;

/**
 * @author Alex Anderson <alex@frontlinesms.com>
 */
public class HibernateSmsInternetServiceSettingsDao
		extends HibernateBaseConfigurableServiceSettingsDao<SmsInternetService>
		implements SmsInternetServiceSettingsDao {
	@Override
	Class<SmsInternetService> getServiceClass() {
		return SmsInternetService.class;
	}
}
