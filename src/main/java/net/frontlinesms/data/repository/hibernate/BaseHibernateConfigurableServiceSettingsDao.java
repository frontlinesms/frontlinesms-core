/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;

import java.util.Collection;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.domain.PersistableSettings;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;
import net.frontlinesms.serviceconfig.ConfigurableService;

/**
 * @author Alex Anderson <alex@frontlinesms.com>
 */
public abstract class BaseHibernateConfigurableServiceSettingsDao extends BaseHibernateDao<PersistableSettings> {
	/** Create instance of this class */
	public BaseHibernateConfigurableServiceSettingsDao() {
		super(PersistableSettings.class);
	}

	/** @see SmsInternetServiceSettingsDao#deleteSmsInternetServiceSettings(PersistableSettings) */
	public void deleteServiceSettings(PersistableSettings settings) {
		super.delete(settings);
	}

	/** @see ServiceSettingsDao#getServiceAccounts() */
	public Collection<PersistableSettings> getServiceAccounts() {
		DetachedCriteria c = super.getCriterion();
		if(getServiceClass().equals(SmsInternetService.class)) {
			c.add(Restrictions.or(
					Restrictions.eq("serviceTypeSuperclass", getServiceClass()),
					Restrictions.isNull("serviceTypeSuperclass")));
		} else {
			c.add(Restrictions.eq("serviceTypeSuperclass", getServiceClass()));
		}
		return super.getList(c);
	}
	
	public abstract Class<? extends ConfigurableService> getServiceClass();

	/** @see ServiceSettingsDao#saveServiceSettings(PersistableSettings) */
	public void saveServiceSettings(PersistableSettings settings) throws DuplicateKeyException {
		super.save(settings);
	}

	/** @see ServiceSettingsDao#updateServiceSettings(PersistableSettings) */
	public void updateServiceSettings(PersistableSettings settings) {
		super.updateWithoutDuplicateHandling(settings);
	}
}
