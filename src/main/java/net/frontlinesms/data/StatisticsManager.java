package net.frontlinesms.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.frontlinesms.AppProperties;
import net.frontlinesms.BuildProperties;
import net.frontlinesms.FrontlineSMS;
import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.data.domain.KeywordAction;
import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.data.domain.PersistableSettings;
import net.frontlinesms.data.domain.SmsModemSettings;
import net.frontlinesms.data.repository.ContactDao;
import net.frontlinesms.data.repository.KeywordActionDao;
import net.frontlinesms.data.repository.KeywordDao;
import net.frontlinesms.data.repository.MessageDao;
import net.frontlinesms.data.repository.SmsInternetServiceSettingsDao;
import net.frontlinesms.data.repository.SmsModemSettingsDao;
import net.frontlinesms.email.EmailException;
import net.frontlinesms.email.smtp.SmtpEmailSender;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;
import net.frontlinesms.serviceconfig.ConfigurableServiceProperties;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 * @author Alex Anderson <alex@frontlinesms.com>
 */
public class StatisticsManager {
	private static final String I18N_KEY_STATS_CONTACTS = FrontlineSMSConstants.COMMON_CONTACTS;
	private static final String I18N_KEY_STATS_KEYWORDS = FrontlineSMSConstants.COMMON_KEYWORDS;
	private static final String I18N_KEY_STATS_KEYWORD_ACTIONS = FrontlineSMSConstants.COMMON_KEYWORD_ACTIONS;
	private static final String I18N_KEY_STATS_LAST_SUBMISSION_DATE = "stats.data.last.submission.date";
	private static final String I18N_KEY_STATS_OS = "stats.data.os";
	private static final String I18N_KEY_STATS_PHONES_CONNECTED = "stats.data.phones.connected";
	private static final String I18N_KEY_STATS_PHONES_DETAILS = "stats.data.phones.details";
	private static final String I18N_KEY_STATS_RECEIVED_MESSAGES = FrontlineSMSConstants.COMMON_RECEIVED_MESSAGES;
	private static final String I18N_KEY_STATS_RECEIVED_MESSAGES_SINCE_LAST_SUBMISSION = "stats.data.received.messages.since.last.submission";
	private static final String I18N_KEY_STATS_SENT_MESSAGES = FrontlineSMSConstants.COMMON_SENT_MESSAGES;
	private static final String I18N_KEY_STATS_SENT_MESSAGES_SINCE_LAST_SUBMISSION = "stats.data.sent.messages.since.last.submission";
	private static final String I18N_KEY_STATS_USER_ID = "stats.data.user.id";
	private static final String I18N_KEY_STATS_VERSION_NUMBER = "stats.data.version.number";
	private static final String I18N_KEY_INTERNET_SERVICE_ACCOUNTS = "stats.data.smsdevice.internet.accounts";

	/** Separates the i18n key from the ID keys in the {@link #statisticsList} for composite keys */
	private static final String STATS_LIST_KEY_SEPARATOR = ":";

	/** Separator used between different stat values in a statistics SMS message */
	private static final char STATISTICS_SMS_SEPARATOR = ',';
	/** Separator used between stat key and value for optional keys */
	private static final char STATISTICS_SMS_OPTIONAL_KEY_VALUE_SEPARATOR = ':';
	/** SMS keyword that statistics SMS will start with.  This allows the FrontlineSMS's statistics
	 * generator to filter statistics SMS by keyword :-) */
	private static final char STATISTICS_SMS_KEYWORD = '\u03A3';
	
	private static final String PROPERTY_OS_NAME = "os.name";
	private static final String PROPERTY_OS_VERSION = "os.version";
	
//> DATA ACCESS OBJECTS
	/** Logging object */
	private final Logger log = FrontlineUtils.getLogger(this.getClass());
	/** Data Access Object for {@link Keyword}s */
	@Autowired
	private KeywordDao keywordDao;
	/** Data Access Object for {@link Contact}s */
	@Autowired
	private ContactDao contactDao;
	/** Data Access Object for {@link FrontlineMessage}s */
	@Autowired
	private MessageDao messageDao;
	/** Data Access Object for {@link KeywordAction}s */
	@Autowired
	private KeywordActionDao keywordActionDao;
	/** Data Access Object for {@link SmsInternetServiceSettingsDao}s */
	@Autowired
	private SmsInternetServiceSettingsDao smsInternetServiceSettingsDao;
	/** Data Access Object for {@link SmsModemSettings}s */
	@Autowired
	private SmsModemSettingsDao smsModemSettingsDao;

	/** List of statistics to send. */
	private Map<String, String> statisticsList;
	/** The email address of the user.  If set, this is used as the From address of stats emails, and is included in the SMS too. */
	private String userEmailAddress;
	
	public StatisticsManager () {
		this.statisticsList = new LinkedHashMap<String, String>();
	}
	
	/**
	 * SETTERS
	 */
	public void setContactDao(ContactDao contactDao) {
		this.contactDao = contactDao;
	}
	
	public void setKeywordDao(KeywordDao keywordDao) {
		this.keywordDao = keywordDao;
	}

	public void setMessageDao(MessageDao messageDao) {
		this.messageDao = messageDao;
	}

	public void setKeywordActionDao(KeywordActionDao keywordActionDao) {
		this.keywordActionDao = keywordActionDao;
	}
	
	public void setSmsInternetServiceSettingsDao(SmsInternetServiceSettingsDao smsInternetServiceSettingsDao) {
		this.smsInternetServiceSettingsDao = smsInternetServiceSettingsDao;
	}
	
	public void setSmsModemSettingsDao(SmsModemSettingsDao smsModemSettingsDao) {
		this.smsModemSettingsDao = smsModemSettingsDao;
	}
	
	/** @return {@link #statisticsList} */
	public Map<String, String> getStatisticsList() {
		return statisticsList;
	}

	
	/**
	 * Launches the collection of all the statistics which are trying to be sent to FLSMS
	 */
	public void collectData () {
		log.trace("COLLECTING DATA");

		this.collectVersionNumber();
		this.collectUserId();
		this.collectOSInfo();
		this.collectLastSubmissionDate();
		this.collectNumberOfContacts();
		this.collectNumberOfReceivedMessages();
		this.collectNumberOfSentMessages();
		this.collectNumberOfKeyword();
		this.collectNumberOfKeywordActions();
		this.collectNumberOfRecognizedPhones();
		this.collectPhonesDetails();
		this.collectSmsInternetServices();
		this.collectLanguage();
		
		// Log the stats data.
		log.info(getDataAsEmailString());

		
		log.trace("FINISHED COLLECTING DATA");
	}

	/**
	 * Collects the User ID
	 */
	private void collectUserId() {
		AppProperties appProperties = AppProperties.getInstance();
		final String userId = appProperties.getUserId();
		this.statisticsList.put(I18N_KEY_STATS_USER_ID, userId);
	}

	/**
	 * Collects the FrontlineSMS version number
	 */
	private void collectVersionNumber() {
		final String version = BuildProperties.getInstance().getVersion(); 
		this.statisticsList.put(I18N_KEY_STATS_VERSION_NUMBER, version);
	}
	
	/**
	 * Collects the name and version of the user's Operating System
	 */
	private void collectOSInfo() {
		final String osInfo = System.getProperty(PROPERTY_OS_NAME) + " " + System.getProperty(PROPERTY_OS_VERSION);
		this.statisticsList.put(I18N_KEY_STATS_OS, osInfo);
	}
	
	private void collectLanguage() {
		// TODO we should collect this
	}
	
	/**
	 * Collects the date of the last actual submission
	 */
	private void collectLastSubmissionDate() {
		final Long dateLastSubmit = AppProperties.getInstance().getLastStatisticsSubmissionDate();
		String formattedDate;
		if(dateLastSubmit == null || dateLastSubmit == 0) {
			formattedDate = "";
		} else {
			formattedDate = InternationalisationUtils.getDateFormat().format(dateLastSubmit);
		}
		this.statisticsList.put(I18N_KEY_STATS_LAST_SUBMISSION_DATE, formattedDate);
	}

	/**
	 * Collects the total number of contacts
	 */
	private void collectNumberOfContacts() {
		final int numberOfContacts = contactDao.getContactCount();
		this.statisticsList.put(I18N_KEY_STATS_CONTACTS, String.valueOf(numberOfContacts));
	}

	/**
	 * Collects the total number of received messages
	 */
	private void collectNumberOfReceivedMessages() {
		final int totalReceived = messageDao.getMessageCount(FrontlineMessage.Type.RECEIVED, null, null);
		this.statisticsList.put(I18N_KEY_STATS_RECEIVED_MESSAGES, String.valueOf(totalReceived));

		final Long lastSubmitDate = AppProperties.getInstance().getLastStatisticsSubmissionDate();
		final int receivedSinceLastSubmit = messageDao.getMessageCount(FrontlineMessage.Type.RECEIVED, lastSubmitDate, null);
		this.statisticsList.put(I18N_KEY_STATS_RECEIVED_MESSAGES_SINCE_LAST_SUBMISSION, String.valueOf(receivedSinceLastSubmit));
	}

	/**
	 * Collects the total number of sent messages
	 */
	private void collectNumberOfSentMessages() {
		final int numberOfSentMessages = messageDao.getMessageCount(FrontlineMessage.Type.OUTBOUND, null, null);
		this.statisticsList.put(I18N_KEY_STATS_SENT_MESSAGES, String.valueOf(numberOfSentMessages));

		Long lastSubmitDate = AppProperties.getInstance().getLastStatisticsSubmissionDate();
		final int numberOfSentMessagesSinceLastSubmission = messageDao.getMessageCount(FrontlineMessage.Type.OUTBOUND, lastSubmitDate , null);
		this.statisticsList.put(I18N_KEY_STATS_SENT_MESSAGES_SINCE_LAST_SUBMISSION, String.valueOf(numberOfSentMessagesSinceLastSubmission));
	}

	/**
	 * Collects the total number of keywords
	 */
	private void collectNumberOfKeyword() {
		final int numberOfKeyword = keywordDao.getTotalKeywordCount() - 1; // We don't want the blank keyword
		this.statisticsList.put(I18N_KEY_STATS_KEYWORDS, String.valueOf(numberOfKeyword));
	}

	/**
	 * Collects the total number of keyword actions
	 */
	private void collectNumberOfKeywordActions() {
		final int numberOfKeywordActions = keywordActionDao.getCount();
		this.statisticsList.put(I18N_KEY_STATS_KEYWORD_ACTIONS, String.valueOf(numberOfKeywordActions));
	}

	/**
	 * Collects the number of phones recognized by FLSMS on this computer
	 * NB: It actually gets the number of configurations in the database
	 */
	private void collectNumberOfRecognizedPhones() {
		final int numberOfRecognizedPhones = smsModemSettingsDao.getCount();
		this.statisticsList.put(I18N_KEY_STATS_PHONES_CONNECTED, String.valueOf(numberOfRecognizedPhones));
	}
	
	/**
	 * Collects the details on the phones recognized by FLSMS on this computer
	 */
	private void collectPhonesDetails() {
		StringBuilder phonesWorking = new StringBuilder();
		
		final List<SmsModemSettings> modemsSettings = smsModemSettingsDao.getAll();
		for (int i = 0 ; i < modemsSettings.size() ; ++i) {
			SmsModemSettings modemSettings = modemsSettings.get(i);
			if (modemSettings.getManufacturer() != null) {
				if (i > 0) phonesWorking.append(", ");
				phonesWorking.append(modemSettings.getManufacturer() + STATS_LIST_KEY_SEPARATOR + modemSettings.getModel());
			}
		}
		this.statisticsList.put(I18N_KEY_STATS_PHONES_DETAILS, phonesWorking.toString());
	}
	
	/** Collects the number of {@link SmsInternetService} accounts. */
	@SuppressWarnings("unchecked")
	private void collectSmsInternetServices() {
		Collection<PersistableSettings> smsInternetServicesSettings = this.smsInternetServiceSettingsDao.getServiceAccounts();
		
		Map<String, Integer> counts = new HashMap<String, Integer>();
		for(PersistableSettings settings : smsInternetServicesSettings) {
			String className = settings.getServiceClass().getName();
			if(!counts.containsKey(className)) {
				counts.put(className, 1);
			} else {
				counts.put(className, counts.get(className) + 1);
			}
		}
		
		for(Entry<String, Integer> e : counts.entrySet()) {
			String value = Integer.toString(e.getValue());
			
			try {
				Class<SmsInternetService> serviceClass = (Class<SmsInternetService>) Class.forName(e.getKey());
				ConfigurableServiceProperties anna = (ConfigurableServiceProperties) serviceClass.getAnnotation(ConfigurableServiceProperties.class);
				this.statisticsList.put(I18N_KEY_INTERNET_SERVICE_ACCOUNTS + STATS_LIST_KEY_SEPARATOR + anna.name(), value);
			} catch (Exception ex) {
				log.warn("Ignoring unrecognized internet service for stats: " + e.getKey(), ex);
			}
		}
	}
	
	public void sendStatistics(FrontlineSMS frontlineController) {
		if (!sendStatisticsViaEmail()) {
			sendStatisticsViaSms(frontlineController);
		}
	}
	
	/**
	 * Actually sends an SMS containing the statistics in a short version
	 */
	private void sendStatisticsViaSms(FrontlineSMS frontlineController) {
		String content = getDataAsSmsString();
		String number = FrontlineSMSConstants.FRONTLINE_STATS_PHONE_NUMBER;
		frontlineController.sendTextMessage(number, content);
	}
	
	/**
	 * Tries to send an e-mail containing the statistics in plain text
	 * @return true if the statistics were successfully sent
	 */
	private boolean sendStatisticsViaEmail() {
		try {
			SmtpEmailSender smtpEmailSender = new SmtpEmailSender(FrontlineSMSConstants.FRONTLINE_SUPPORT_EMAIL_SERVER);
			smtpEmailSender.sendEmail(
					FrontlineSMSConstants.FRONTLINE_STATS_EMAIL,
					smtpEmailSender.getLocalEmailAddress(getUserEmailAddress(), "User " + this.statisticsList.get(I18N_KEY_STATS_USER_ID)),
					"FrontlineSMS Statistics",
					getStatisticsForEmail());
			return true;
		} catch(EmailException ex) { 
			log.info("Sending statistics via email failed.", ex);
			return false;
		}
	}
	
	/**
	 * Gets the statistics in a format suitable for emailing.
	 * @param bob {@link StringBuilder} used for compiling the body of the e-mail.
	 */
	private String getStatisticsForEmail() {
		StringBuilder bob = new StringBuilder();
		beginSection(bob, "Statistics");
	    bob.append(getDataAsEmailString());
		endSection(bob, "Statistics");
	    return bob.toString();
	}
	
	/**
	 * Starts a section of the e-mail's body.
	 * Sections started with this method should be ended with {@link #endSection(StringBuilder, String)}
	 * @param bob The {@link StringBuilder} used for building the e-mail's body.
	 * @param sectionName The name of the section of the report that is being started.
	 */
	private static void beginSection(StringBuilder bob, String sectionName) {
		bob.append("\n### Begin Section '" + sectionName + "' ###\n");
	}
	
	/**
	 * Ends a section of the e-mail's body.
	 * Sections ended with this should have been started with {@link #beginSection(StringBuilder, String)}
	 * @param bob The {@link StringBuilder} used for building the e-mail's body.
	 * @param sectionName The name of the section of the report that is being started.
	 */
	private static void endSection(StringBuilder bob, String sectionName) {
		bob.append("### End Section '" + sectionName + "' ###\n");
	}
	
//> USER DATA SETTER METHODS 
	public void setUserEmailAddress(String userEmailAddress) {
		this.userEmailAddress = userEmailAddress;
	}
	public String getUserEmailAddress() {
		return userEmailAddress;
	}
	
//> REPORT GENERATION METHODS

	/**
	 * Generate the text which will be sent via SMS.
	 * This is the {@link #STATISTICS_SMS_KEYWORD} followed by each data separated by {@link #STATISTICS_SMS_SEPARATOR}
	 * @return The generated String
	 */
	public String getDataAsSmsString() {
		StringBuilder statsOutput = new StringBuilder();

		statsOutput.append(this.getUserEmailAddress());
		
		for(Entry<String, String> entry : statisticsList.entrySet()) {
			statsOutput.append(STATISTICS_SMS_SEPARATOR);
			
			// For composite values, we need the id from the key to be included in the
			// SMS so we can make sense of the stat
			String key = entry.getKey();
			if(isCompositeKey(key)) {
				int shortKeyBeginIndex = key.indexOf(STATS_LIST_KEY_SEPARATOR) + 1;
				String shortKey = key.substring(shortKeyBeginIndex, Math.min(key.length(), shortKeyBeginIndex + 2));
				statsOutput.append(shortKey);
				statsOutput.append(STATISTICS_SMS_OPTIONAL_KEY_VALUE_SEPARATOR);
			}
			if (key.equals(I18N_KEY_STATS_PHONES_DETAILS)) {
				statsOutput.append(this.shortenPhonesDetails(entry.getValue()));
			} else {
				statsOutput.append(entry.getValue());
			}
		}
		
		return STATISTICS_SMS_KEYWORD + " " + 
				statsOutput.toString();
	}
	
	private String shortenPhonesDetails(String phonesDetails) {
		StringBuilder shortenedPhonesDetails = new StringBuilder();
		
		for (String phoneDetails : phonesDetails.split(", ")) {
			// For each phone details
			String [] details = phoneDetails.split(STATS_LIST_KEY_SEPARATOR);
			String manufactuer = details[0];
			
			// We take the two first characters of the Manufacturer and the whole model
			// Ex.: SonyEricsson K800i > SoK800i
			if (manufactuer.length() < 3) {
				shortenedPhonesDetails.append(manufactuer);
			} else {
				shortenedPhonesDetails.append(manufactuer.substring(0, 2));
			}
			if (details.length > 1) {
				// if there is a model (just in case there is not), we remove spaces to gain space
				shortenedPhonesDetails.append(details[1]);
			}
			shortenedPhonesDetails.append(STATS_LIST_KEY_SEPARATOR);
		}
		if (shortenedPhonesDetails.length() > 0) {
			shortenedPhonesDetails.deleteCharAt(shortenedPhonesDetails.length() - 1);
		}
		return shortenedPhonesDetails.toString();
	}

	/**
	 * Generate the text which will be sent via e-mail
	 * It represents each data with its full title
	 * @return The generated String
	 */
	public String getDataAsEmailString () {
		String statsOutput = "";
		
		 for (Entry<String, String> entry : statisticsList.entrySet()) {
			statsOutput += entry.getKey() + " = " + entry.getValue() + "\n";
		}
		 
		return statsOutput;
	}
	
	public String toString () {
		return getDataAsEmailString();
	}
	
	public int getReceivedMessages() {
		return Integer.parseInt(this.statisticsList.get(I18N_KEY_STATS_RECEIVED_MESSAGES));
	}
	public int getSentMessages() {
		return Integer.parseInt(this.statisticsList.get(I18N_KEY_STATS_SENT_MESSAGES));
	}

//> STATIC HELPER METHODS
	/** Checks if a key from {@link #statisticsList} is composite */
	public static boolean isCompositeKey(String key) {
		return key.indexOf(STATS_LIST_KEY_SEPARATOR) != -1;
	}
	
	/** Splits stats map key into constituent parts. */
	public static String[] splitStatsMapKey(String key) {
		if(!isCompositeKey(key)) {
			return new String[]{key};
		} else {
			return key.split(STATS_LIST_KEY_SEPARATOR);
		}
	}
}
