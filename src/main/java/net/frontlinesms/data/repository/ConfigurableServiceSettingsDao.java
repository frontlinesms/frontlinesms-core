/**
 * 
 */
package net.frontlinesms.data.repository;

import java.util.Collection;

import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.domain.*;

/**
 * Data Access Object interface for {@link PersistedSettings}.
 * @author Alex
 */
public interface ConfigurableServiceSettingsDao {
	/**
	 * Saves {@link PersistedSettings} to the data source 
	 * @param settings settings to save
	 * @throws DuplicateKeyException
	 */
	public void saveServiceSettings(PersistedSettings settings) throws DuplicateKeyException;
	
	/**
	 * Updates {@link PersistedSettings} to the data source 
	 * @param settings settings to update
	 */
	public void updateServiceSettings(PersistedSettings settings);

	/** @return all {@link PersistedSettings} */
	public Collection<PersistedSettings> getServiceAccounts();
	
	/**
	 * Deletes {@link PersistedSettings} from the data source
	 * @param settings settings to delete
	 */
	public void deleteServiceSettings(PersistedSettings settings);
}
