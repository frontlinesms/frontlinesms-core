/**
 * 
 */
package net.frontlinesms;

import net.frontlinesms.resources.UserHomeFilePropertySet;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

/**
 * @author Alex Anderson <alex@frontlinesms.com>
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public final class AppProperties extends UserHomeFilePropertySet {
//> STATIC CONSTANTS
	
//> PROPERTY STRINGS
	/** Property key (String): the file of the language file */
	public static final String KEY_LANGUAGE_FILE_PATH = "language.file.path";
	/** Property key (boolean): show the first time wizard or not */
	public static final String KEY_SHOW_WIZARD = "first.time.wizard";
	/** Property key (String): the version of the application last time it was run */
	public static final String KEY_VERSION_LAST_RUN = "version";
	/** Property key (String): Path to the database config file to use */
	public static final String KEY_DATABASE_CONFIG_PATH = "database.config";
	/** Property key (String): User ID */
	public static final String KEY_USER_ID = "user.id";
	/** Property key (String): User Email address */
	public static final String KEY_USER_EMAIL = "user.email";
	/** Property key (String): Date of last statistics submission */
	public static final String KEY_LAST_STATS_SUBMISSION = "stats.submit.lastdate";
	/** Property key (String): Date of last prompt */
	public static final String KEY_LAST_STATS_PROMPT = "stats.prompt.lastdate";
	/** Property key (String): Mms Polling Frequency */
	public static final String KEY_MMS_POLLING_FREQUENCY = "mms.polling.frequency";
	/** Property key (String): Whether or not the device connection dialog is shown when a connection problem occurs*/
	public static final String KEY_SHOW_DEVICE_CONNECTION_DIALOG = "smsdevice.connection.problem.dialog.show";
	/** Property key (String) indicating whether or not the statistics dialog should be prompted **/
	public static final String KEY_PROMPT_STATS_DIALOG = "prompt.stats";
	/** Property key (String) indicating whether or not the statistics can be sent without asking **/
	public static final String KEY_AUTHORIZE_STATS_SENDING = "authorize.stats.sending";
	/** Property key (String) indicating whether or not all devices should be disabled **/
	public static final String KEY_DISABLE_ALL_DEVICES = "disable.all.devices";
	/** Property key (String) indicating whether or not devices should be detected at startup **/
	public static final String KEY_START_DETECTING_AT_STARTUP = "start.detecting.at.startup";
	/** Property key (double) indicating the price per SMS sent */
	public static final String KEY_SMS_COST_SENT_MESSAGES = "sms.cost.sent.messages";
	/** Property key (double) indicating the price per SMS received */
	public static final String KEY_SMS_COST_RECEIVED_MESSAGES = "sms.cost.received.messages";
	/** Property key (String) indicating the country the user is currently in */
	public static final String KEY_USER_COUNTRY = "user.country";

//> DEFAULT VALUES
	/** Default value for {@link #KEY_DATABASE_CONFIG_PATH} */
	private static final String DEFAULT_DATABASE_CONFIG_PATH = "h2.database.xml";
	
	/** Singleton instance of this class. */
	private static AppProperties instance;

//> INSTANCE PROPERTIES

//> CONSTRUCTORS
	/** Create a new App properties file. */
	private AppProperties() {
		super("app");
	}

//> ACCESSORS
	/** @return the name of the language filename */
	public String getLanguageFilePath() {
		return super.getProperty(KEY_LANGUAGE_FILE_PATH);
	}
	/** @param filename the name of the language filename */
	public void setLanguageFilename(String filename) {
		super.setProperty(KEY_LANGUAGE_FILE_PATH, filename);
	}
	/** @return <code>true</code> if first time wizard should be shown; <code>false</code> otherwise */
	public boolean isShowWizard() {
		return super.getPropertyAsBoolean(KEY_SHOW_WIZARD, true);
	}
	/** @param showWizard <code>true</code> if the wizard should be shown, <code>false</code> otherwise */
	public void setShowWizard(boolean showWizard) {
		super.setPropertyAsBoolean(KEY_SHOW_WIZARD, showWizard);
	}
	/** @return the last version of the app that was run */
	public String getLastRunVersion() {
		return super.getProperty(KEY_VERSION_LAST_RUN);
	}
	/** @param version the version of the app currently running */
	public void setLastRunVersion(String version) {
		super.setProperty(KEY_VERSION_LAST_RUN, version);
	}
	/** @return the path to the database config file */
	public String getDatabaseConfigPath() {
		return super.getProperty(KEY_DATABASE_CONFIG_PATH, DEFAULT_DATABASE_CONFIG_PATH);
	}
	/** @param databaseConfigPath new value for the path to the database config file */
	public void setDatabaseConfigPath(String databaseConfigPath) {
		super.setProperty(KEY_DATABASE_CONFIG_PATH, databaseConfigPath);
	}
	
	/** @return the user Id */
	public String getUserId() {
		return super.getProperty(KEY_USER_ID);
	}
	/** @param userId The userId to set to the property */
	public void setUserId(String userId) {
		super.setProperty(KEY_USER_ID, userId);
	}
	public String getUserEmail() {
		return super.getProperty(KEY_USER_EMAIL, "");
	}
	public void setUserEmail(String userEmail) {
		super.setProperty(KEY_USER_EMAIL, userEmail);
	}
	
	/** @return the date of last stats submission, or <code>null</code> if none has been set */
	public Long getLastStatisticsSubmissionDate() {
		String lastSubmitDate = super.getProperty(KEY_LAST_STATS_SUBMISSION);
		return lastSubmitDate == null ? null : Long.parseLong(lastSubmitDate);
	}
	
	/** Sets last stat submission date to NOW */
	public void setLastStatisticsSubmissionDate() {
		super.setProperty(KEY_LAST_STATS_SUBMISSION, Long.toString(System.currentTimeMillis()));
	}
	
	/** @return the last date the dialog was prompted, or <code>null</code> if it has never been filled */
	public Long getLastStatisticsPromptDate() {
		String lastPrompt = super.getProperty(KEY_LAST_STATS_PROMPT);
		return lastPrompt == null ? null : Long.valueOf(lastPrompt);
	}
	
	/** Sets last stat submission prompt date to NOW */
	public void setLastStatisticsPromptDate() {
		super.setProperty(KEY_LAST_STATS_PROMPT, Long.toString(System.currentTimeMillis()));
	}
	
	/** @return the MMS E-Mail polling frequency */
	public int getMmsPollingFrequency() {
		String pollFrequency = super.getProperty(KEY_MMS_POLLING_FREQUENCY);
		try {
			return pollFrequency == null ? FrontlineSMSConstants.DEFAULT_MMS_POLLING_FREQUENCY : Integer.parseInt(pollFrequency);
		} catch (NumberFormatException e) {
			return FrontlineSMSConstants.DEFAULT_MMS_POLLING_FREQUENCY;
		}
	}
	
	/** Sets the MMS E-Mail polling frequency */
	public void setMmsPollingFrequency(int pollFrequency) {
		super.setProperty(KEY_MMS_POLLING_FREQUENCY, String.valueOf(pollFrequency));
	}
	
	/** @return <code>true</code> if the device connection dialog must be shown when a connection problem occurs, <code>false</code> otherwise */
	public boolean shouldPromptDeviceConnectionDialog() {
		return super.getPropertyAsBoolean(KEY_SHOW_DEVICE_CONNECTION_DIALOG, true);
	}
	/** @param showDialog whether the device connection dialog is shown when a connection problem occurs */
	public void setShouldPromptDeviceConnectionDialog(boolean showDialog) {
		super.setPropertyAsBoolean(KEY_SHOW_DEVICE_CONNECTION_DIALOG, showDialog);
	}
	
	/** @return whether or not the statistics dialog should be prompted */
	public boolean shouldPromptStatsDialog() {
		return super.getPropertyAsBoolean(KEY_PROMPT_STATS_DIALOG, true);
	}
	
	/**
	 * Set whether or not the statistics dialog should be prompted.
	 * @param shouldPrompStatsDialog value for property {@link #KEY_PROMPT_STATS_DIALOG}
	 */
	public void shouldPromptStatsDialog(boolean shouldPrompStatsDialog) {
		super.setPropertyAsBoolean(KEY_PROMPT_STATS_DIALOG, shouldPrompStatsDialog);
	}
	
	/** @return whether or not the statistics dialog should be prompted */
	public boolean isStatsSendingAuthorized() {
		return super.getPropertyAsBoolean(KEY_AUTHORIZE_STATS_SENDING, true);
	}
	
	/**
	 * Set whether or not the statistics can be sent without asking.
	 * @param authorizeStatsSending value for property {@link #KEY_AUTHORIZE_STATS_SENDING}
	 */
	public void setAuthorizeStatsSending(boolean authorizeStatsSending) {
		super.setPropertyAsBoolean(KEY_AUTHORIZE_STATS_SENDING, authorizeStatsSending);
	}
	
	/** @return whether or not all devices should be disabled. **/
	public boolean disableAllDevices() {
		return super.getPropertyAsBoolean(KEY_DISABLE_ALL_DEVICES, false);
	}
	
	/**
	 * Set whether or not all devices should be disabled.
	 * @param allDevicesDisabled value for property {@link #KEY_DISABLE_ALL_DEVICES}
	 */
	public void shouldDisableAllDevices(boolean allDevicesDisabled) {
		super.setPropertyAsBoolean(KEY_DISABLE_ALL_DEVICES, allDevicesDisabled);
	}
	
	/** @return whether or not devices should be detected at startup. **/
	public boolean startDetectingAtStartup() {
		return super.getPropertyAsBoolean(KEY_START_DETECTING_AT_STARTUP, true);
	}
	
	/**
	 * Set whether or not all devices should be disabled.
	 * @param allDevicesDisabled value for property {@link #KEY_DISABLE_ALL_DEVICES}
	 */
	public void shouldStartDetectingAtStartup(boolean shouldStartDetectingAtStartup) {
		super.setPropertyAsBoolean(KEY_START_DETECTING_AT_STARTUP, shouldStartDetectingAtStartup);
	}
	
//> INSTANCE HELPER METHODS
	
	/** @return number representing the cost of one SMS sent */
	public double getCostPerSmsSent() {
		// TODO ideally this would be an int in the least significant denomination of the currency, e.g. pennies or cents
		String val = super.getProperty(KEY_SMS_COST_SENT_MESSAGES);
		double cost = 0.1; // the default cost
		if(val != null) {
			try { cost = Double.parseDouble(val); } catch(NumberFormatException ex) { /* just use the default */ }
		}
		return cost;
	}
	
	/** @param costPerSmsSent the price of one sms */
	public void setCostPerSmsSent(double costPerSmsSent) {
		super.setProperty(KEY_SMS_COST_SENT_MESSAGES, Double.toString(costPerSmsSent));
	}
	
	/** @return a {@link Double} representing the cost of one SMS sent */
	public double getCostPerSmsReceived() {
		// TODO ideally this would be an int in the least significant denomination of the currency, e.g. pennies or cents
		String val = super.getProperty(KEY_SMS_COST_RECEIVED_MESSAGES);
		double cost = 0.0; // the default cost
		if(val != null) {
			try { cost = Double.parseDouble(val); } catch(NumberFormatException ex) { /* just use the default */ }
		}
		return cost;
	}
	
	/** @param costPerSmsReceived the price of one sms */
	public void setCostPerSmsReceived(double costPerSmsReceived) {
		super.setProperty(KEY_SMS_COST_RECEIVED_MESSAGES, Double.toString(costPerSmsReceived));
	}
	
	/** @return the two-letter ISO-FIXME code for the country the user is located in. */
	public String getUserCountry() {
		String countryCode = super.getProperty(KEY_USER_COUNTRY);
		if (countryCode == null) {
			return InternationalisationUtils.getCurrentLocale().getCountry().toUpperCase();
		} else {
			return countryCode;
		}
	}
	
	/** @param the current country the user is located in. */
	public void setUserCountry(String country) {
		super.setProperty(KEY_USER_COUNTRY, country);
	}

//> STATIC FACTORIES
	/**
	 * Lazy getter for {@link #instance}
	 * @return The singleton instance of this class
	 */
	public static synchronized AppProperties getInstance() {
		if(instance == null) {
			instance = new AppProperties();
		}
		return instance;
	}
//> STATIC HELPER METHODS
}
