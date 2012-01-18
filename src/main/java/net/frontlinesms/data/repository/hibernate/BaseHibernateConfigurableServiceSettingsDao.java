/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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

	public PersistableSettings getById(long id) {
		DetachedCriteria c = getCriterion();
		c.add(Restrictions.eq("id", id));
		return getUnique(c);
	}
	
	public PersistableSettings getByProperty(String key, String value) {
		return getByProperties(key, value);
	}
	
	public PersistableSettings getByProperties(String... keyValuePairs) {
		assert((keyValuePairs.length & 1) == 0);
		Collection<PersistableSettings> allByProperty = getAllByProperties(keyValuePairs);
		if(allByProperty.size() == 0) return null;
		else return (PersistableSettings) allByProperty.toArray()[0];
	}
	
	public Collection<PersistableSettings> getAllByProperty(String key, String value) {
		return getAllByProperties(key, value);
	}
	
	public Collection<PersistableSettings> getAllByProperties(String... keyValuePairs) {
		assert((keyValuePairs.length & 1) == 0);
		// FIXME should do something clever here with criteria/HQL instead of manual match, e.g.:
//		return getList("PersistableSettings_PersistableSettingValue as pv, " +
//				"PersistableSettingValue as v " +
//				"WHERE pv.key=? " +
//				"AND pv.PersistableSettings_id=v.id " +
//				"AND v.value=? " +
//				"AND pv IN properties", key, value);
//		return getList(getByPropertyCriteria(key, value));
		List<PersistableSettings> all = getAll();
		List<PersistableSettings> matching = new LinkedList<PersistableSettings>();
		for(PersistableSettings s : all) {
			boolean keep = true;
			for(int i=0; i<keyValuePairs.length; i+=2) {
				String key = keyValuePairs[i];
				String value = keyValuePairs[i+1];
				if(keep) {
					boolean isSet = s.getProperties().containsKey(key);
					if(value == null) keep = !isSet;
					else keep = isSet && value.equals(s.get(key).getValue());
				}
			}
			if(keep) matching.add(s);
		}
		return matching;
	}

	/** @see SmsInternetServiceSettingsDao#deleteSmsInternetServiceSettings(PersistableSettings) */
	public void deleteServiceSettings(PersistableSettings settings) {
		delete(settings);
	}
	
	public List<PersistableSettings> getServiceAccounts() {
		return getAll();
	}
	
	@Override
	protected DetachedCriteria getCriterion() {
		DetachedCriteria c = super.getCriterion();
		if(getServiceClass().equals(SmsInternetService.class)) {
			c.add(Restrictions.or(
					Restrictions.eq("serviceTypeSuperclass", getServiceClass()),
					Restrictions.isNull("serviceTypeSuperclass")));
		} else {
			c.add(Restrictions.eq("serviceTypeSuperclass", getServiceClass()));
		}
		return c;
	}
	
	public abstract Class<? extends ConfigurableService> getServiceClass();

	/** @see ServiceSettingsDao#saveServiceSettings(PersistableSettings) */
	public void saveServiceSettings(PersistableSettings settings) throws DuplicateKeyException {
		save(settings);
	}

	/** @see ServiceSettingsDao#updateServiceSettings(PersistableSettings) */
	public void updateServiceSettings(PersistableSettings settings) {
		updateWithoutDuplicateHandling(settings);
	}
}
