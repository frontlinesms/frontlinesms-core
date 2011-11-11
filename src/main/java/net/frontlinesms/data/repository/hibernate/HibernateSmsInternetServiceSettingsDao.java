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
		extends BaseHibernateConfigurableServiceSettingsDao
		implements SmsInternetServiceSettingsDao {
	@Override
	public Class<SmsInternetService> getServiceClass() {
		return SmsInternetService.class;
	}
}
