/**
 * 
 */
package net.frontlinesms.data.repository;

import net.frontlinesms.data.domain.*;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;

/**
 * Data Access Object interface for {@link PersistableSettings}.
 * @author Alex
 */
public interface SmsInternetServiceSettingsDao extends ConfigurableServiceSettingsDao<SmsInternetService> {}
