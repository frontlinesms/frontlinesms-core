/*
 * FrontlineSMS <http://www.frontlinesms.com>
 * Copyright 2007, 2008 kiwanja
 * 
 * This file is part of FrontlineSMS.
 * 
 * FrontlineSMS is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * FrontlineSMS is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FrontlineSMS. If not, see <http://www.gnu.org/licenses/>.
 */
package net.frontlinesms;

import java.io.File;
import java.util.*;

import net.frontlinesms.data.*;
import net.frontlinesms.data.domain.*;
import net.frontlinesms.data.domain.FrontlineMessage.Status;
import net.frontlinesms.data.domain.FrontlineMessage.Type;
import net.frontlinesms.data.events.DatabaseEntityNotification;
import net.frontlinesms.data.events.EntityDeletedNotification;
import net.frontlinesms.data.events.EntitySavedNotification;
import net.frontlinesms.data.repository.*;
import net.frontlinesms.events.EventBus;
import net.frontlinesms.events.EventObserver;
import net.frontlinesms.events.FrontlineEventNotification;
import net.frontlinesms.listener.*;
import net.frontlinesms.messaging.IncomingMessageProcessor;
import net.frontlinesms.messaging.MessageFormatter;
import net.frontlinesms.messaging.mms.MmsServiceManager;
import net.frontlinesms.messaging.mms.events.MmsReceivedNotification;
import net.frontlinesms.messaging.sms.DummySmsService;
import net.frontlinesms.messaging.sms.SmsService;
import net.frontlinesms.messaging.sms.SmsServiceManager;
import net.frontlinesms.messaging.sms.events.SmsModemStatusNotification;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;
import net.frontlinesms.messaging.sms.modem.SmsModem;
import net.frontlinesms.messaging.sms.modem.SmsModemStatus;
import net.frontlinesms.mms.MmsMessage;
import net.frontlinesms.plugins.PluginController;
import net.frontlinesms.plugins.PluginControllerProperties;
import net.frontlinesms.plugins.PluginProperties;
import net.frontlinesms.resources.ResourceUtils;
import net.frontlinesms.ui.UiGeneratorController;

import org.apache.log4j.Logger;
import org.smslib.CIncomingMessage;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ListFactoryBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.FileSystemResource;

/**
 * 
 * FrontlineSMS - an SMS gateway in a box.
 * 
 * Built for the not-for-profit sector, to provide 
 * -> Group messaging
 * -> Auto-responders
 * -> SMS data gathering
 * 
 * This system is built to provide out of the box auto-detection of standard GSM phones for
 * sending messages, and also to use a built-in or external database for storing the results.
 * 
 * The architecture is built so that it can be easily extended.
 * 
 * The default usage is as a desktop application, looking after the phones and providing the 
 * GUI interface at the same time, but it should be straightforward to separate it into a 
 * server and client interface, or even a web driven interface if that would be required.
 * 
 * The architecture is as follows:
 * 
 * FrontlineSMS.java - starts the application and passes messages and events around the system.
 * SmsHandler.java - runs as a separate thread, will create and manage GSM phone handlers and also internet SMS messaging centres
 *  
 * see {@link "http://www.frontlinesms.net"} for more details. 
 * copyright owned by Kiwanja.net
 * 
 * @author Ben Whitaker ben(at)masabi(dot)com
 * @author Alex Anderson alex(at)masabi(dot)com
 */
public class FrontlineSMS implements SmsSender, SmsListener, EmailListener, EventObserver  {
	/** Logging object */
	private static Logger LOG = FrontlineUtils.getLogger(FrontlineSMS.class);
	/** SMS device emulator */
	public static final SmsService EMULATOR = new DummySmsService(FrontlineSMSConstants.EMULATOR_MSISDN);
	
//> INSTANCE VARIABLES

//> DATA ACCESS OBJECTS
	/** Data Access Object for {@link Keyword}s */
	private KeywordDao keywordDao;
	/** Data Access Object for {@link Group}s */
	private GroupDao groupDao;
	/** Data Access Object for {@link Group}s */
	private GroupMembershipDao groupMembershipDao;
	/** Data Access Object for {@link Contact}s */
	private ContactDao contactDao;
	/** Data Access Object for {@link FrontlineMessage}s */
	private MessageDao messageDao;
	/** Data Access Object for {@link KeywordAction}s */
	private KeywordActionDao keywordActionDao;
	/** Data Access Object for {@link SmsModemSettings} */
	private SmsModemSettingsDao smsModemSettingsDao;
	/** Data Access Object for {@link PersistableSettings} */
	private SmsInternetServiceSettingsDao smsInternetServiceSettingsDao;
	/** Data Access Object for {@link EmailAccount}s */
	private EmailAccountDao emailAccountDao;
	/** Data Access Object for {@link Email}s */
	private EmailDao emailDao;
	
//> SERVICE MANAGERS
	/** Class that handles sending of email messages */
	private EmailServerHandler emailServerManager;
	/** Manager of SMS services */
	private SmsServiceManager smsServiceManager;
	/** Manager of ?MS services */
	private MmsServiceManager mmsServiceManager;
	/** Processor of received SMS & MMS. */
	private IncomingMessageProcessor incomingMessageProcessor;
	private PluginManager pluginManager;

	//> EVENT LISTENERS
	/** Listener for email events */
	private EmailListener emailListener;
	/** Listener for UI-related events */
	private UIListener uiListener;
	/** Main {@link EventBus} through which all core events should be channelled. */
	private EventBus eventBus;
	
//> INITIALISATION METHODS
	/** The application context describing dependencies of the application. */
	private ConfigurableApplicationContext applicationContext;
	
	/** Initialise {@link #applicationContext}. */
	public void initApplicationContext() {
		// Load the data mode from the app.properties file
		AppProperties appProperties = AppProperties.getInstance();
		
		LOG.info("Load Spring/Hibernate application context to initialise DAOs");
			
		// Create a base ApplicationContext defining the hibernate config file we need to import
		StaticApplicationContext baseApplicationContext = new StaticApplicationContext();
		baseApplicationContext.registerBeanDefinition("hibernateConfigLocations", createHibernateConfigLocationsBeanDefinition());
		
		// Get the spring config locations
		String databaseExternalConfigPath = ResourceUtils.getConfigDirectoryPath() + ResourceUtils.PROPERTIES_DIRECTORY_NAME + File.separatorChar + appProperties.getDatabaseConfigPath();
		String[] configLocations = getSpringConfigLocations(databaseExternalConfigPath);
		baseApplicationContext.refresh();
		
		FileSystemXmlApplicationContext applicationContext = new FileSystemXmlApplicationContext(configLocations, false, baseApplicationContext);
		this.applicationContext = applicationContext;
		
		// Add post-processor to handle substituted database properties
		PropertyPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer();
		String databasePropertiesPath = ResourceUtils.getConfigDirectoryPath() + ResourceUtils.PROPERTIES_DIRECTORY_NAME + File.separatorChar + appProperties.getDatabaseConfigPath() + ".properties";
		propertyPlaceholderConfigurer.setLocation(new FileSystemResource(new File(databasePropertiesPath)));
		propertyPlaceholderConfigurer.setIgnoreResourceNotFound(true);
		applicationContext.addBeanFactoryPostProcessor(propertyPlaceholderConfigurer);
		applicationContext.refresh();
		
		LOG.info("Context loaded successfully.");
		
		this.pluginManager = new PluginManager(this, applicationContext);
		
		LOG.info("Getting DAOs from application context...");
		groupDao = (GroupDao) applicationContext.getBean("groupDao");
		groupMembershipDao = (GroupMembershipDao) applicationContext.getBean("groupMembershipDao");
		contactDao = (ContactDao) applicationContext.getBean("contactDao");
		keywordDao = (KeywordDao) applicationContext.getBean("keywordDao");
		keywordActionDao = (KeywordActionDao) applicationContext.getBean("keywordActionDao");
		messageDao = (MessageDao) applicationContext.getBean("messageDao");
		emailDao = (EmailDao) applicationContext.getBean("emailDao");
		emailAccountDao = (EmailAccountDao) applicationContext.getBean("emailAccountDao");
		smsInternetServiceSettingsDao = (SmsInternetServiceSettingsDao) applicationContext.getBean("smsInternetServiceSettingsDao");
		smsModemSettingsDao = (SmsModemSettingsDao) applicationContext.getBean("smsModemSettingsDao");
		eventBus = (EventBus) applicationContext.getBean("eventBus");
	}
	
	/** Deinitialise {@link #applicationContext}. */
	public void deinitApplicationContext() {
		this.applicationContext.close();
	}
	
	/**
	 * Start services for the {@link FrontlineSMS} instance.  N.B. this must be called <strong>after</strong> {@link #initApplicationContext()}
	 * This method should be called only once before the services are stopped using {@link #stopServices()}.
	 */
	public void startServices() {
		try {
			LOG.debug("Creating blank keyword...");
			Keyword blankKeyword = new Keyword("", "");
			keywordDao.saveKeyword(blankKeyword);
			LOG.debug("Blank keyword created.");
		} catch (DuplicateKeyException e) {
			// Looks like this has been created already, so ignore the exception
			LOG.debug("Blank keyword creation failed - already exists.");
		}
		try {
			LOG.debug("Creating MMS keyword...");
			Keyword mmsKeyword = new Keyword(FrontlineSMSConstants.MMS_KEYWORD, "");
			keywordDao.saveKeyword(mmsKeyword);
			LOG.debug("MMS keyword created.");
		} catch (DuplicateKeyException e) {
			// Looks like this has been created already, so ignore the exception
			LOG.debug("MMS keyword creation failed - already exists.");
		}
		
		if (this.eventBus != null) {
			this.eventBus.registerObserver(this);
		}
		
		LOG.debug("Initialising email server handler...");
		emailServerManager = new EmailServerHandler();
		emailServerManager.setEmailListener(this);

		LOG.debug("Initialising incoming message processor...");
		
		// Initialise the incoming message processor
		incomingMessageProcessor = new IncomingMessageProcessor(this);
		incomingMessageProcessor.start();
		
		LOG.debug("Starting Phone Manager...");
		smsServiceManager = new SmsServiceManager(this, eventBus);
		smsServiceManager.listComPortsAndOwners(false);
		smsServiceManager.start();
		
		mmsServiceManager = new MmsServiceManager();
		mmsServiceManager.setEventBus(getEventBus());
		mmsServiceManager.setEmailAccountDao(this.emailAccountDao);
		mmsServiceManager.start();

		initSmsInternetServices();
		initMmsEmailServices();
		
		this.pluginManager.initPluginControllers();

		LOG.debug("Starting E-mail Manager...");
		emailServerManager.start();

		LOG.debug("Re-Loading messages to outbox.");
		//We need to reload all messages, which status is OUTBOX, to the outbox.
		for (FrontlineMessage m : messageDao.getMessages(Type.OUTBOUND, Status.OUTBOX, Status.PENDING)) {
			smsServiceManager.sendSMS(m);
		}

		LOG.debug("Re-Loading e-mails to outbox.");
		//We need to reload all email, which status is RETRYING, to the outbox.
		for (Email m : emailDao.getEmailsForStatus(new Email.Status[] {Email.Status.RETRYING, Email.Status.PENDING, Email.Status.OUTBOX})) {
			emailServerManager.sendEmail(m);
		}
	}

	private void stopServices() {
		// de-initialise plugin controllers
		if(this.pluginManager != null) {
			for(PluginController pluginController : this.pluginManager.getPluginControllers()) {
				try {
					pluginController.deinit();
				} catch(Throwable t) {
					LOG.error("There was a problem stopping one of the plugin controller.", t);
				}
			}
		}
		
		if (smsServiceManager != null) {
			LOG.debug("Stopping Phone Manager...");
			smsServiceManager.stopRunning();
		}
		if (mmsServiceManager != null) {
			LOG.debug("Stopping MMS Manager...");
			mmsServiceManager.stopRunning();
		}
		if (emailServerManager != null) {
			LOG.debug("Stopping E-mail Manager...");
			emailServerManager.stopRunning();
		}
		if(this.incomingMessageProcessor != null) {
			LOG.debug("Stopping the incoming message processor...");
			this.incomingMessageProcessor.die();
		}
	}
	
	/**
	 * Shutdown and re-initialise the {@link #applicationContext}.
	 * TODO it has not been confirmed this method exectutes cleanly, so it should be used with caution.
	 */
	public void reboot() {
		deinitApplicationContext();
		stopServices();
		initApplicationContext();
		startServices();
	}
	
	/** Initialise {@link SmsInternetService}s. */
	private void initSmsInternetServices() {
		for (PersistableSettings settings : this.smsInternetServiceSettingsDao.getServiceAccounts()) {
			String className = settings.getServiceClass().getName();
			LOG.info("Initializing SmsInternetService of class: " + className);
			try {
				this.smsServiceManager.addSmsInternetService(settings);
			} catch (Throwable t) {
				LOG.warn("Unable to initialize SmsInternetService of class: " + className, t);
			}
		}
	}
	
	/**
	 * Gives all receiving e-mail accounts to the {@link MmsServiceManager} so it can use them as {@link PopImapEmailMmsReceiver}s
	 */
	private void initMmsEmailServices() {
		this.mmsServiceManager.clearMmsEmailReceivers();
		
		for (EmailAccount mmsEmailAccount : this.emailAccountDao.getReceivingEmailAccounts()) {
			this.mmsServiceManager.addMmsEmailReceiver(mmsEmailAccount);
		}
	}
	
	/**
	 * Create the configLocations bean for the Hibernate config.
	 * This method should only be called from within {@link FrontlineSMS#initApplicationContext()}
	 * @return {@link BeanDefinition} containing details of the hibernate config for the app and its plugins.
	 */
	private BeanDefinition createHibernateConfigLocationsBeanDefinition() {
		// Initialise list of hibernate config files
		List<String> hibernateConfigList = new ArrayList<String>();
		// Add main hibernate config location
		hibernateConfigList.add("classpath:frontlinesms.hibernate.cfg.xml");
		// Add hibernate config locations for plugins
		for(Class<? extends PluginController> pluginClass : PluginProperties.getInstance().getPluginClasses()) {
			LOG.info("Processing plugin class: " + pluginClass.getName());
			if(pluginClass.isAnnotationPresent(PluginControllerProperties.class)) {
				PluginControllerProperties properties = pluginClass.getAnnotation(PluginControllerProperties.class);
				String pluginHibernateLocation = properties.hibernateConfigPath();
				if(!PluginControllerProperties.NO_VALUE.equals(pluginHibernateLocation)) {
					hibernateConfigList.add(pluginHibernateLocation);
				}
			}
		}
		
		GenericBeanDefinition myBeanDefinition = new GenericBeanDefinition();
		myBeanDefinition.setBeanClass(ListFactoryBean.class);
		MutablePropertyValues values = new MutablePropertyValues();
		values.addPropertyValue("sourceList", hibernateConfigList);
		myBeanDefinition.setPropertyValues(values);
		
		return myBeanDefinition;
	}
	
	/**
	 * Gets a list of configLocations required for initialising Spring {@link ApplicationContext}.
	 * This method should only be called from within {@link FrontlineSMS#initApplicationContext()}
	 * @param externalConfigPath Spring path to the external Spring database config file 
	 * @return list of configLocations used for initialising {@link ApplicationContext}
	 */
	private String[] getSpringConfigLocations(String...externalConfigPaths) {
		ArrayList<String> configLocations = new ArrayList<String>();
		
		// Custom Spring configurations
		for(String externalConfigPath : externalConfigPaths) {
			LOG.info("Loading spring application context from: " + externalConfigPath);
			configLocations.add("file:" + externalConfigPath);
		}
		
		// Main Spring configuration
		configLocations.add("classpath:frontlinesms-spring-hibernate.xml");
		
		// Add config locations for plugins
		for(Class<? extends PluginController> pluginControllerClass : PluginProperties.getInstance().getPluginClasses()) {
			if(pluginControllerClass.isAnnotationPresent(PluginControllerProperties.class)) {
				PluginControllerProperties properties = pluginControllerClass.getAnnotation(PluginControllerProperties.class);
				String pluginConfigLocation = properties.springConfigLocation();
				if(!PluginControllerProperties.NO_VALUE.equals(pluginConfigLocation)) {
					LOG.trace("Adding plugin Spring config location: " + pluginConfigLocation);
					configLocations.add(pluginConfigLocation);
				}
			}
		}
		
		return configLocations.toArray(new String[configLocations.size()]);
	}
	
	/**
	 * This method makes the phone manager thread stop.
	 */
	public void destroy() {
		LOG.trace("ENTER");
		stopServices();
		deinitApplicationContext();
		LOG.trace("EXIT");
	}
	
//> EVENT HANDLER METHODS
	
	/** Called by the SmsHandler when an SMS message is received. */
	public synchronized void incomingMessageEvent(SmsService receiver, CIncomingMessage incomingMessage) {
		this.incomingMessageProcessor.queue((SmsService)receiver, incomingMessage);
	}

	/** Passes an outgoing message event to the SMS Listener if one is specified. */
	public synchronized void outgoingMessageEvent(SmsService sender, FrontlineMessage outgoingMessage) {
		// The message status will have changed, so save it here
		this.messageDao.updateMessage(outgoingMessage);
		
		// FIXME should log this message here
		if (uiListener != null) {
			uiListener.outgoingMessageEvent(outgoingMessage);
		}
	}

	/** Passes an outgoing email event to {@link #emailListener} if it is defined */
	public synchronized void outgoingEmailEvent(EmailSender sender, Email email) {
		// The email status will have changed, so save it here
		this.emailDao.updateEmail(email);
		
		if (emailListener != null) {
			emailListener.outgoingEmailEvent(sender, email);
		}
	}

//> SMS SEND METHODS
	/** Persists and sends an SMS message. */
	public void sendMessage(FrontlineMessage message) {
		messageDao.saveMessage(message);
		smsServiceManager.sendSMS(message);
		if (uiListener != null) { 
			uiListener.outgoingMessageEvent(message);
		}
	}
	
	/**
	 * Sends an SMS using the phoneManager in the standard way.  The only advantage this
	 * method provides over using the phoneManager is that this will redirect emulator
	 * messages in the correct manner.
	 * 
	 * @param targetNumber The recipient number.
	 * @param textContent The message to be sent.
	 * @return the {@link FrontlineMessage} describing the sent message
	 */
	public FrontlineMessage sendTextMessage(String targetNumber, String textContent) {
		LOG.trace("ENTER");
		
		// By default, currently replaces the name by the phone number if it's not in the contacts
		// TODO: have a setting for this
		String recipientName = targetNumber;
		
		Contact c = this.contactDao.getFromMsisdn(targetNumber);
		if (c != null) {
			recipientName = c.getDisplayName();
		}
		
		// Replace the content of the message by substituting variables
		textContent = MessageFormatter.formatMessage(textContent, 
								MessageFormatter.MARKER_RECIPIENT_NAME, recipientName,
								MessageFormatter.MARKER_RECIPIENT_NUMBER, targetNumber);
		
		FrontlineMessage m;
		if (targetNumber.equals(FrontlineSMSConstants.EMULATOR_MSISDN)) {
			m = FrontlineMessage.createOutgoingMessage(System.currentTimeMillis(), FrontlineSMSConstants.EMULATOR_MSISDN, FrontlineSMSConstants.EMULATOR_MSISDN, textContent.trim());
			m.setStatus(Status.SENT);
			messageDao.saveMessage(m);
			outgoingMessageEvent(EMULATOR, m);
			incomingMessageEvent(EMULATOR, new CIncomingMessage(System.currentTimeMillis(), FrontlineSMSConstants.EMULATOR_MSISDN, textContent.trim(), 1, "NYI"));
		} else {
			m = FrontlineMessage.createOutgoingMessage(System.currentTimeMillis(), "", targetNumber, textContent.trim());
			this.sendMessage(m);
		}
		LOG.trace("EXIT");
		return m;
	}
	
//> ACCESSOR METHODS
	/** @return a named bean from the application context */
	@SuppressWarnings("unchecked")
	public <T> T getBean(String name, Class<T> clazz) {
		return (T) this.applicationContext.getBean(name);
	}
	
	/** @return {@link #contactDao} */
	public ContactDao getContactDao() {
		return this.contactDao;
	}
	/** @return {@link #groupDao} */
	public GroupDao getGroupDao() {
		return this.groupDao;
	}
	/** @return {@link #groupMembershipDao} */
	public GroupMembershipDao getGroupMembershipDao() {
		return this.groupMembershipDao;
	}
	/** @return {@link #messageDao} */
	public MessageDao getMessageDao() {
		return this.messageDao;
	}
	/** @return {@link #keywordDao} */
	public KeywordDao getKeywordDao() {
		return this.keywordDao;
	}
	/** @return {@link #keywordActionDao} */
	public KeywordActionDao getKeywordActionDao() {
		return this.keywordActionDao;
	}
	/** @return {@link #smsModemSettingsDao} */
	public SmsModemSettingsDao getSmsModemSettingsDao() {
		return smsModemSettingsDao;
	}
	/** @return a new instance of {@link StatisticsManager} */
	public StatisticsManager getStatisticsManager() {
		return (StatisticsManager) applicationContext.getBean("statisticsManager");
	}
	/** @return {@link #emailAccountDao} */
	public EmailAccountDao getEmailAccountFactory() {
		return emailAccountDao;
	}
	/** @return {@link #emailDao} */
	public EmailDao getEmailDao() {
		return emailDao;
	}
	/** @return {@link #eventBus} */
	public EventBus getEventBus() {
		return eventBus;
	}
	/** @return {@link #emailServerManager} */
	public EmailServerHandler getEmailServerHandler() {
		return emailServerManager;
	}
	/** @return {@link #pluginManager} */
	public PluginManager getPluginManager() {
		return pluginManager;
	}
	
	/** @return {@link #uiListener} */
	public UIListener getUiListener() {
		return uiListener;
	}
	/** @param uiListener new value for {@link #uiListener} */
	public void setUiListener(UIListener uiListener) {
		this.uiListener = uiListener;
		this.incomingMessageProcessor.setUiListener(uiListener);
	}
	
	/** @return {@link #smsServiceManager} */
	public SmsServiceManager getSmsServiceManager() {
		return this.smsServiceManager;
	}
	
	/** @return {@link #smsServiceManager} */
	public MmsServiceManager getMmsServiceManager() {
		return this.mmsServiceManager;
	}

	/** @param emailListener new value for {@link #emailListener} */
	public void setEmailListener(EmailListener emailListener) {
		this.emailListener = emailListener;
	}
	
	/**
	 * Adds another {@link IncomingMessageListener} to {@link IncomingMessageProcessor}.
	 * @param incomingMessageListener new {@link IncomingMessageListener}
	 * @see IncomingMessageProcessor#addIncomingMessageListener(IncomingMessageListener)
	 */
	public void addIncomingMessageListener(IncomingMessageListener incomingMessageListener) {
		this.incomingMessageProcessor.addIncomingMessageListener(incomingMessageListener);
	}
	
	/**
	 * Removes a {@link IncomingMessageListener} from {@link IncomingMessageProcessor}.
	 * @param incomingMessageListener {@link IncomingMessageListener} to be removed
	 * @see IncomingMessageProcessor#removeIncomingMessageListener(IncomingMessageListener)
	 */
	public void removeIncomingMessageListener(IncomingMessageListener incomingMessageListener) {
		this.incomingMessageProcessor.removeIncomingMessageListener(incomingMessageListener);
	}

	/** @return {@link #smsInternetServiceSettingsDao} */
	public SmsInternetServiceSettingsDao getSmsInternetServiceSettingsDao() {
		return this.smsInternetServiceSettingsDao;
	}

	/** @return {@link #smsServiceManager}'s {@link SmsInternetService}s */
	public Collection<SmsInternetService> getSmsInternetServices() {
		return this.smsServiceManager.getSmsInternetServices();
	}

	/**
	 * Handles the way statistics are loaded or not at startup.
	 * TODO this method should not be here - most likely should be in {@link UiGeneratorController}
	 * @param uiController
	 */
	public void handleStatistics(UiGeneratorController uiController) {
		AppProperties appProperties = AppProperties.getInstance();
		if (shouldLaunchStatsCollection()) {
			if (appProperties.shouldPromptStatsDialog()) {
				uiController.showStatsDialog();
			} else {
				// The user doesn't want to prompt the statistics dialog
				// Let's check if he authorized to send the stats automatically
				if (appProperties.isStatsSendingAuthorized()) {
					StatisticsManager statisticsManager = getStatisticsManager();
					statisticsManager.setUserEmailAddress(appProperties.getUserEmail());
					statisticsManager.collectData();
					statisticsManager.sendStatistics(this);
				}
			}
		}
	}
	
	/**
	 * Check if it's time to collect statistics 
	 */
	public boolean shouldLaunchStatsCollection() {
		AppProperties appProperties = AppProperties.getInstance();
		Long dateLastPrompt = appProperties.getLastStatisticsPromptDate();
		if (dateLastPrompt == null) {
			// This is the first time we are checking if the dialog must be prompted, this should then be the first launch.
			// We set the last prompt date to the current date to delay the prompt until STATISTICS_DAYS_BEFORE_RELAUNCH days of use.
			appProperties.setLastStatisticsPromptDate();
			appProperties.saveToDisk();
			
			return false;
		} else {
			long dateNextPrompt = dateLastPrompt + (FrontlineSMSConstants.MILLIS_PER_DAY * FrontlineSMSConstants.STATISTICS_DAYS_BEFORE_RELAUNCH);
			return System.currentTimeMillis() >= dateNextPrompt;
		}
	}
	
	/**
	 * @return The total number of active connections running (SMS & MMS)
	 */
	public int getNumberOfActiveConnections() {
		return this.smsServiceManager.getNumberOfActiveConnections() 
			 + this.mmsServiceManager.getNumberOfActiveConnections();
	}

	public void notify(FrontlineEventNotification notification) {
		if (notification instanceof MmsReceivedNotification) {
			MmsMessage mmsMessage = ((MmsReceivedNotification) notification).getMessage();
			this.incomingMessageProcessor.queue(mmsMessage);
		} else if (notification instanceof DatabaseEntityNotification<?>) {
			// Database notification
			Object entity = ((DatabaseEntityNotification<?>) notification).getDatabaseEntity();
			if (entity instanceof EmailAccount && ((EmailAccount) entity).isForReceiving()) {
				// If there is any change in the E-Mail accounts, we refresh the list of MmsEmailServices
				if (notification instanceof EntityDeletedNotification<?>) {
					this.mmsServiceManager.removeMmsEmailReceiver((EmailAccount) entity);
				} else if (notification instanceof EntitySavedNotification<?>) {
					this.mmsServiceManager.addMmsEmailReceiver((EmailAccount) entity);
				} else {
					this.mmsServiceManager.updateMmsEmailService((EmailAccount) entity);	
				}
			}
		} else if (notification instanceof SmsModemStatusNotification) {
			SmsModemStatus status = ((SmsModemStatusNotification) notification).getStatus();
			// check for modem connected status here
			if (status == SmsModemStatus.TRY_TO_CONNECT) {
				// If we're trying to connect, set the device's SMSC number
				SmsModem modem = (SmsModem) ((SmsModemStatusNotification) notification).getService();
				String serial = modem.getSerial();
				SmsModemSettings settings = this.smsModemSettingsDao.getSmsModemSettings(serial);
				if(settings != null) {
					modem.setSmscNumber(settings.getSmscNumber());
					// Only set the PIN number if it hasn't been set in the Manual Connection dialog
					if(modem.getSimPin() == null) {
						modem.setSimPin(settings.getSimPin());
					}
				}
			}
		}
	}
}
