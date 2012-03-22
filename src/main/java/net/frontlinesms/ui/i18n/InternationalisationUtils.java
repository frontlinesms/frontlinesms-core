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
package net.frontlinesms.ui.i18n;

import static net.frontlinesms.FrontlineSMSConstants.COMMON_FAILED;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_OUTBOX;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_PENDING;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_RETRYING;
import static net.frontlinesms.FrontlineSMSConstants.COMMON_SENT;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.frontlinesms.AppProperties;
import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.domain.Email;
import net.frontlinesms.resources.ResourceUtils;
import net.frontlinesms.ui.FrontlineUI;
import net.frontlinesms.ui.UiProperties;

import org.apache.log4j.Logger;

import thinlet.Thinlet;

/**
 * Utilities for helping internationalise text etc.
 * 
 * @author Alex Anderson
 * @author Gonçalo Silva
 */
public class InternationalisationUtils {

//> STATIC PROPERTIES
	/**
	 * Name of the directory containing the languages files. This is located
	 * within the config directory.
	 */
	private static final String LANGUAGES_DIRECTORY_NAME = "languages";
	/** The filename of the default language bundle. */
	public static final String DEFAULT_LANGUAGE_BUNDLE_FILENAME = "frontlineSMS.properties";
	/** The path to the default language bundle on the classpath. */
	public static final String DEFAULT_LANGUAGE_BUNDLE_PATH = "/resources/languages/"
			+ DEFAULT_LANGUAGE_BUNDLE_FILENAME;
	/** Logging object for this class */
	private static Logger LOG = FrontlineUtils
			.getLogger(InternationalisationUtils.class);

//> GENERAL i18n HELP METHODS
	/** The default characterset, UTF-8. This must be available for every JVM. */
	public static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

//>
	public static String getI18nString(Internationalised i) {
		return getI18nString(i.getI18nKey());
	}
	
	/**
	 * Return an internationalised message for this key, with the current resource bundle
	 * This method tries to get the string for the current bundle and if it does
	 * not exist, it looks into the default bundle (English GB).
	 * 
	 * @param key
	 * @return the internationalised text, or the english text if no
	 *         internationalised text could be found
	 */
	public static String getI18nString(String key) {
		if (FrontlineUI.currentResourceBundle != null) {
			try {
				return FrontlineUI.currentResourceBundle.getValue(key);
			} catch (MissingResourceException ex) {
			}
		}
		return Thinlet.DEFAULT_ENGLISH_BUNDLE.get(key);
	}
	
	/**
	 * Return an internationalised message for this key and the given resource bundle. 
	 * <br> This method tries to get the string for the bundle given in parameter and looks into
	 * the default bundle (English GB) if <code>null</code>. 
	 * @param key
	 * @param languageBundle
	 * @return the internationalised text, or the english text if no internationalised text could be found
	 */
	public static String getI18nString(String key, LanguageBundle languageBundle) {
		if(languageBundle != null) {
			try {
				return languageBundle.getValue(key);
			} catch(MissingResourceException ex) {}
		}
		return Thinlet.DEFAULT_ENGLISH_BUNDLE.get(key);
	}

	/**
	 * Return the list of internationalised message for this prefix. <br>
	 * This method tries to get the strings from the current bundle, and if it
	 * does not exist, it looks into the default bundle
	 * 
	 * @param key
	 * @return the list internationalised text, or an empty list if no
	 *         internationalised text could be found
	 */
	public static List<String> getI18nStrings(String key, String ... i18nValues) {
		if(FrontlineUI.currentResourceBundle != null) {
			try {
				List<String> values = FrontlineUI.currentResourceBundle.getValues(key);

				if (i18nValues.length == 0) {
					return values;
				} else {
					List<String> formattedValues = new ArrayList<String>();
					for (String value : values) {
						formattedValues.add(formatString(value, i18nValues));
					}
					
					return formattedValues;
				}
			} catch(MissingResourceException ex) {}
		}
		return LanguageBundle.getValues(Thinlet.DEFAULT_ENGLISH_BUNDLE, key);
	}

	/**
	 * Return an internationalised message for this key. This calls
	 * {@link #getI18nString(String)} and then replaces any instance of
	 * {@link FrontlineSMSConstants#ARG_VALUE} with @param argValues
	 * 
	 * @param key
	 * @param argValues
	 * @return an internationalised string with any substitution variables
	 *         converted
	 */
	public static String getI18nString(String key, String... argValues) {
		String string = getI18nString(key);
		return formatString(string, argValues);
	}

	/**
	 * Return an internationalised message for this key. This calls
	 * {@link #getI18nString(String)} and then replaces any instance of
	 * {@link FrontlineSMSConstants#ARG_VALUE} with @param argValues
	 * 
	 * @param key
	 * @param argValues
	 * @return an internationalised string with any substitution variables
	 *         converted
	 */
	public static String formatString(String string, String... argValues) {
		if (argValues != null) {
			// Iterate backwards through the replacements and replace the
			// arguments with the new values. Need
			// to iterate backwards so e.g. %10 is replaced before %1
			for (int i = argValues.length - 1; i >= 0; --i) {
				String arg = argValues[i];
				if (arg != null) {
					if (LOG.isDebugEnabled())
						LOG.debug("Subbing " + arg + " as "
								+ (FrontlineSMSConstants.ARG_VALUE + i)
								+ " into: " + string);
					string = string.replace(
							FrontlineSMSConstants.ARG_VALUE + i, arg);
				}
			}
		}
		return string;
	}

	/**
	 * Return an internationalised message for this key. This converts the
	 * integer to a {@link String} and then calls
	 * {@link #getI18nString(String, String...)} with this argument.
	 * 
	 * @param key
	 * @param intValue
	 * @return the internationalised string with the supplied integer embedded
	 *         at the appropriate place
	 */
	public static String getI18nString(String key, int intValue) {
		return getI18nString(key, Integer.toString(intValue));
	}

	
	/**
	 * Parses a string representation of an amount of currency to an integer.
	 * This will handle cases where the string has separators, including non
	 * default separators, two or more separators, and different separators
	 * in the same string.
	 *  
	 * @param currencyString
	 * @return the currency amount represented by the supplied string
	 * @throws {@link NumberFormatException}
	 */
	public static final double parseCurrency(String currencyString)	throws NumberFormatException{
		String regexPattern = "\\D";
		Pattern pattern = Pattern.compile(regexPattern);
		Matcher matcher = pattern.matcher(currencyString);
		

		//Execute if currencyString has the specified pattern
		if (matcher.find()) {
			
			String[] splitValues = currencyString.split(regexPattern);
			
			if (splitValues.length == 0) {
				throw new NumberFormatException();
			} else if (splitValues.length == 1) {
				currencyString = splitValues[0];
			} else if (splitValues.length == 2) {
				// Only one separator - assume its for decimal places
				currencyString = splitValues[0] + "." + splitValues[1];
			} else {
				int splitValuesLastBlock = splitValues.length - 1;
				
				String[] separators = new String[splitValuesLastBlock];

				matcher.reset();
				
				// Find all separators and store them
				for(int counter = 0; !matcher.hitEnd(); counter++){
					if(matcher.find()){
						separators [counter] = matcher.group();
					}
				}

				// Check if the last two separators are the same - if true, assume no decimal places are present
				if (separators[separators.length - 1].equals(separators[separators.length - 2])) {
					currencyString = currencyString.replaceAll("\\D", "");
				} else {
					currencyString = "";
					for (int i = 0; i < splitValuesLastBlock; i++) {
						currencyString += splitValues[i];
					}
					currencyString += "." + splitValues[splitValuesLastBlock];
				}
			}
		}

		return Double.parseDouble(currencyString);
	}

	/**
	 * Returns a formatted value according to the defined currency format
	 * @param value
	 * @return formatted value
	 */
	public static final String formatCurrency(double value) {
		if (UiProperties.getInstance().isCurrencyFormatCustom()) {
			String currencyFormat = UiProperties.getInstance().getCustomCurrencyFormat();
			return new CurrencyFormatter(currencyFormat).format(value);
		} else {
			return NumberFormat.getCurrencyInstance(getCurrentLocale()).format(value);
		}
	}

	/**
	 * Returns a formatted value according to the defined currency format
	 * @param value
	 * @return formatted value
	 */
	public static final String formatCurrency(BigDecimal value) {
		if (UiProperties.getInstance().isCurrencyFormatCustom()) {
			String currencyFormat = UiProperties.getInstance().getCustomCurrencyFormat();
			return new CurrencyFormatter(currencyFormat).format(value);
		} else {
			return NumberFormat.getCurrencyInstance(getCurrentLocale()).format(value);
		}
	}

//> LANGUAGE BUNDLE LOADING METHODS
	/**
	 * Loads the default, english {@link LanguageBundle} from the classpath
	 * 
	 * @return the default English {@link LanguageBundle}
	 * @throws IOException
	 *             If there was a problem loading the default language bundle.
	 *             // TODO this should probably throw a runtimeexception of some
	 *             sort
	 */
	public static final LanguageBundle getDefaultLanguageBundle()
			throws IOException {
		return ClasspathLanguageBundle.create(DEFAULT_LANGUAGE_BUNDLE_PATH);
	}

	/**
	 * @return {@link InputStream} to the default translation file on the
	 *         classpath.
	 */
	public static InputStream getDefaultLanguageBundleInputStream() {
		return ClasspathLanguageBundle.class
				.getResourceAsStream(DEFAULT_LANGUAGE_BUNDLE_PATH);
	}

	/**
	 * Loads a {@link LanguageBundle} from a file. All files are encoded with
	 * UTF-8. TODO change this to use {@link Currency}, and put the ISO 4217
	 * currency code in the l10n file.
	 * 
	 * @param file
	 * @return The loaded bundle, or NULL if the bundle could not be loaded.
	 */
	public static final FileLanguageBundle getLanguageBundle(File file) {
		try {
			FileLanguageBundle bundle = FileLanguageBundle.create(file);
			LOG.info("Successfully loaded language bundle from file: "
					+ file.getName());
			LOG.info("Bundle reports filename as: "
					+ bundle.getFile().getAbsolutePath());
			LOG.info("Language Name : " + bundle.getLanguageName());
			LOG.info("Language Code : " + bundle.getLanguageCode());
			LOG.info("Country       : " + bundle.getCountry());
			LOG.info("Right-To-Left : " + bundle.isRightToLeft());
			return bundle;
		} catch (Exception ex) {
			LOG.error("Problem reading language file: " + file.getName(), ex);
			return null;
		}
	}

	/**
	 * @param identifier
	 *            ID used when logging problems while loading the text resource
	 * @param inputStream
	 * @return map containing map of key-value pairs of text resources
	 * @throws IOException
	 */
	public static final Map<String, String> loadTextResources(
			String identifier, InputStream inputStream) throws IOException {
		HashMap<String, String> i18nStrings = new HashMap<String, String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				inputStream, CHARSET_UTF8));
		String line;
		while ((line = in.readLine()) != null) {
			line = line.trim();
			if (line.length() > 0 && line.charAt(0) != '#') {
				int splitChar = line.indexOf('=');
				if (splitChar <= 0) {
					// there's no "key=value" pair on this line, but it does
					// have text on it. That's
					// not strictly legal, so we'll log a warning and carry on.
					LOG.warn("Bad line in language file '" + identifier
							+ "': '" + line + "'");
				} else {
					String key = line.substring(0, splitChar).trim();
					if (i18nStrings.containsKey(key)) {
						// This key has already been read from the language
						// file. Ignore the new value.
						LOG.warn("Duplicate key in language file '': ''");
					} else {
						String value = line.substring(splitChar + 1).trim();
						if (value.length() > 0) {
							i18nStrings.put(key, value);
						}
					}
				}
			}
		}
		return i18nStrings;
	}

	/**
	 * Loads all language bundles from within and without the JAR
	 * 
	 * @return all language bundles from within and without the JAR
	 */
	public static Collection<FileLanguageBundle> getLanguageBundles() {
		ArrayList<FileLanguageBundle> bundles = new ArrayList<FileLanguageBundle>();
		File langDir = new File(getLanguageDirectoryPath());
		if (!langDir.exists() || !langDir.isDirectory())
			throw new IllegalArgumentException(
					"Could not find resources directory: "
							+ langDir.getAbsolutePath());

		for (File file : langDir.listFiles()) {
			FileLanguageBundle bungle = getLanguageBundle(file);
			if (bungle != null) {
				bundles.add(bungle);
			}
		}
		return bundles;
	}

	/** @return path of the directory in which language bundles are located. */
	private static final String getLanguageDirectoryPath() {
		return ResourceUtils.getConfigDirectoryPath()
				+ LANGUAGES_DIRECTORY_NAME + File.separatorChar;
	}

	/** @return path of the directory in which language bundles are located. */
	public static final File getLanguageDirectory() {
		return new File(ResourceUtils.getConfigDirectoryPath(),
				LANGUAGES_DIRECTORY_NAME);
	}

//> DATE FORMAT GETTERS
	/**
	 * N.B. This {@link DateFormat} may be used for parsing user-entered data.
	 * @return date format for displaying and entering year (4 digits), month and day.
	 */
	public static DateFormat getDateFormat() {
		return new SimpleDateFormat(
				getI18nString(FrontlineSMSConstants.DATEFORMAT_YMD));
	}
	
	/**
	 * N.B. This {@link DateFormat} may be used for parsing user-entered data.
	 * @return date format for displaying and entering hours and minutes
	 */
	public static DateFormat getDateFormatHM() {
		return new SimpleDateFormat(
				getI18nString(FrontlineSMSConstants.DATEFORMAT_HM));
	}
	
	/**
	 * Formats date suitable for user's locale including day of month,
	 * month and year.
	 * @param date
	 * @return formatted date
	 */
	public static String formatDate(long date) {
		return getDateFormat().format(new Date(date));
	}
	
	public static String formatTime(long time) {
		return getDateFormatHM().format(new Date(time));
	}

	/**
	 * This is not used for parsing user-entered data.
	 * 
	 * @return date format for displaying date and time.#
	 */
	public static DateFormat getDatetimeFormat() {
		return new SimpleDateFormat(
				getI18nString(FrontlineSMSConstants.DATEFORMAT_YMD_HMS));
	}

	/**
	 * TODO what is this method used for? This value seems completely
	 * nonsensical - why wouldn't you just use the timestamp itself? When do you
	 * ever need the date as an actual string?
	 * 
	 * @return current time as a formatted date string
	 */
	public static String getDefaultStartDate() {
		return getDateFormat().format(new Date());
	}

	/**
	 * Parse the supplied {@link String} into a {@link Date}. This method
	 * assumes that the supplied date is in the same format as
	 * {@link #getDateFormat()}.
	 * @param date A date {@link String} formatted with {@link #getDateFormat()}
	 * @return a java {@link Date} object describing the supplied date
	 * @throws ParseException
	 */
	public static Date parseDate(String date) throws ParseException {
		return getDateFormat().parse(date);
	}
	
	public static Date parseTime(Date date, String time) throws ParseException {
		Calendar forReadingTime = Calendar.getInstance();
		forReadingTime.setTime(getDateFormatHM().parse(time));
		
		Calendar toModify = Calendar.getInstance();
		toModify.setTime(date);
		int hour = forReadingTime.get(Calendar.HOUR_OF_DAY);
		toModify.add(Calendar.HOUR_OF_DAY, hour);
		toModify.set(Calendar.MINUTE, forReadingTime.get(Calendar.MINUTE));
		
		System.out.println("InternationalisationUtils.parseTime() >> " + toModify);
		
		return toModify.getTime();
	}

	/**
	 * <p>
	 * Merges the source map into the destination. Values in the destination
	 * take precedence - they will not be overridden if the same key occurs in
	 * both destination and source.
	 * </p>
	 * <p>
	 * If a <code>null</code> source is provided, this method does nothing; if a
	 * <code>null</code> destination is provided, a {@link NullPointerException}
	 * will be thrown.
	 * 
	 * @param destination
	 * @param source
	 */
	public static void mergeMaps(Map<String, String> destination,
			Map<String, String> source) {
		assert (destination != null) : "You must provide a destination map to merge into.";

		// If there is nothing to merge, just return.
		if (source == null)
			return;

		for (String key : source.keySet()) {
			if (destination.get(key) != null) {
				// key already present in language bundle - ignoring
			} else {
				// this key does not appear in the language bundle, so add it
				// with the value from the map
				destination.put(key, source.get(key));
			}
		}
	}

	/**
	 * Get the status of a {@link Email} as a {@link String}.
	 * 
	 * @param email
	 * @return {@link String} representation of the status.
	 */
	public static final String getEmailStatusAsString(Email email) {
		switch (email.getStatus()) {
		case OUTBOX:
			return getI18nString(COMMON_OUTBOX);
		case PENDING:
			return getI18nString(COMMON_PENDING);
		case SENT:
			return getI18nString(COMMON_SENT);
		case RETRYING:
			return getI18nString(COMMON_RETRYING);
		case FAILED:
			return getI18nString(COMMON_FAILED);
		default:
			return "(unknown)";
		}
	}

	/**
	 * @return the current locale, specified by which language is currently
	 *         selected
	 */
	public static Locale getCurrentLocale() {
		return FrontlineUI.currentResourceBundle != null ? FrontlineUI.currentResourceBundle
				.getLocale()
				: new Locale("en", "gb");
	}

	/** Check if the supplied phone number is a valid international phone number. */
	public static boolean isValidInternationalPhoneNumber(String phoneNumber) {
		return CountryCallingCode.isValidInternationalNumber(phoneNumber);
	}
	
	/** Check if the supplied phone number is a valid local or international phone number. */
	public static boolean isValidLocalPhoneNumber(String phoneNumber) {
		return CountryCallingCode.isValidLocalNumber(phoneNumber, AppProperties.getInstance().getUserCountry());
	}
	
	/** Check if the supplied phone number is a valid local or international phone number. */
	public static boolean isValidPhoneNumber(String phoneNumber) {
		return isValidLocalPhoneNumber(phoneNumber) || isValidInternationalPhoneNumber(phoneNumber);
	}
	
	public static String getInternationalPhoneNumber(String phoneNumber) {
		return CountryCallingCode.format(phoneNumber, AppProperties.getInstance().getUserCountry());
	}
}
