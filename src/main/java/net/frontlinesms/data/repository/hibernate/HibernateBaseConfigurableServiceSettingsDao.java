/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;

import java.util.Collection;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.domain.PersistedSettings;
import net.frontlinesms.data.repository.ConfigurableServiceSettingsDao;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;

/**
 * @author Alex Anderson <alex@frontlinesms.com>
 */
public abstract class HibernateBaseConfigurableServiceSettingsDao<T> extends BaseHibernateDao<PersistedSettings> implements ConfigurableServiceSettingsDao {
	/** Create instance of this class */
	public HibernateBaseConfigurableServiceSettingsDao() {
		super(PersistedSettings.class);
	}

	/** @see SmsInternetServiceSettingsDao#deleteSmsInternetServiceSettings(PersistedSettings) */
	public void deleteServiceSettings(PersistedSettings settings) {
		super.delete(settings);
	}

	/** @see ServiceSettingsDao#getServiceAccounts() */
	public Collection<PersistedSettings> getServiceAccounts() {
		DetachedCriteria c = super.getCriterion();
		if(getServiceClass().equals(SmsInternetService.class)) {
			c.add(Restrictions.or(
					Restrictions.eq("serviceTypeSuperclass", getServiceClass().getSimpleName()),
					Restrictions.isNull("serviceTypeSuperclass")));
		} else {
			c.add(Restrictions.eq("serviceTypeSuperclass", getServiceClass().getSimpleName()));
		}
		return super.getList(c);
	}
	
	abstract Class<T> getServiceClass();

	/** @see ServiceSettingsDao#saveServiceSettings(PersistedSettings) */
	public void saveServiceSettings(PersistedSettings settings) throws DuplicateKeyException {
		super.save(settings);
	}

	/** @see ServiceSettingsDao#updateServiceSettings(PersistedSettings) */
	public void updateServiceSettings(PersistedSettings settings) {
		super.updateWithoutDuplicateHandling(settings);
	}
}
