/**
 * 
 */
package net.frontlinesms.data.repository;

import java.util.Collection;

import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.domain.*;
import net.frontlinesms.serviceconfig.ConfigurableService;

/**
 * Data Access Object interface for {@link PersistableSettings}.
 * @author Alex
 */
public interface ConfigurableServiceSettingsDao<T extends ConfigurableService> {
	/** Gets an instance of {@link PersistableSettings} by its database ID. */
	public PersistableSettings getById(long id);

	public PersistableSettings getByProperty(String key, String value);
	
	public PersistableSettings getByProperties(String... keyValuePairs);

	public Collection<PersistableSettings> getAllByProperty(String key, String value);
	
	public Collection<PersistableSettings> getAllByProperties(String... keyValuePairs);
	
	/**
	 * Saves {@link PersistableSettings} to the data source 
	 * @param settings settings to save
	 * @throws DuplicateKeyException
	 */
	public void saveServiceSettings(PersistableSettings settings) throws DuplicateKeyException;
	
	/**
	 * Updates {@link PersistableSettings} to the data source 
	 * @param settings settings to update
	 */
	public void updateServiceSettings(PersistableSettings settings);

	/** @return all {@link PersistableSettings} */
	public Collection<PersistableSettings> getServiceAccounts();
	
	/**
	 * Deletes {@link PersistableSettings} from the data source
	 * @param settings settings to delete
	 */
	public void deleteServiceSettings(PersistableSettings settings);
}
