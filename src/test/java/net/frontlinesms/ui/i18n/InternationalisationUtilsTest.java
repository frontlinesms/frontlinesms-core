/**
 * 
 */
package net.frontlinesms.ui.i18n;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.junit.BaseTestCase;

/**
 * Test methods for {@link InternationalisationUtils}.
 * @author Alex
 */
public class InternationalisationUtilsTest extends BaseTestCase {
//> STATIC PROPERTIES
	/** Date format which shows years, months and days, e.g. 2009-12-31 */
	private static final DateFormat DATEFORMAT_DATE_ONLY = new SimpleDateFormat("yyyy/MM/dd");
	/** Adds an extra, more intensive date test to {@link #testDateFormats()} */
	private static final boolean EXTENSIVE_DATE_TEST = false;
	
//> TEST DATA
	/**
	 * Test dates in the format {year (4-digit), month (1-indexed), day (1-31; valid for month and year).
	 */
	private static final int[][] TEST_DATES = {
		{2009, 12, 15},
		{1888, 3, 31},
		{2015, 7, 1},
		{1, 1, 1},
		{3000, 12, 31},
	};
	
//> INSTANCE VARIABLES
	/** Logging object */
	private final Logger log = Logger.getLogger(this.getClass());
	
//> TEST METHODS
	/**
	 * This method loads all date formats from each language bundle, and makes sure that they are valid.
	 * This tests {@link InternationalisationUtils#getDateFormat()} vs {@link InternationalisationUtils#parseDate(String)}.
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public void testDateFormats() throws ParseException, IOException {
		log.warn("This test has been disabled as it needs modifications to its implementation.");
		for(LanguageBundle bungle : getLanguageBundles()) {
			log.info("Testing " + bungle.getLanguageName());
			
			String formatString = bungle.getValue(FrontlineSMSConstants.DATEFORMAT_YMD);
			DateFormat dateFormat = new SimpleDateFormat(formatString);
			for(int[] dateDetails : TEST_DATES) {
				// Create a date object and format it as a String.  Reparse the String and make sure that the returned date is
				// within an acceptable margin (< a single day) of the original date.
				Date testDate = getDate(dateDetails);
				String formattedDate = dateFormat.format(testDate);
				Date parsedDate = dateFormat.parse(formattedDate);
				System.err.println(testDate + " --> " + parsedDate);
				
				assertEquals("Parsed date was incorrect for language '" + bungle.getLanguageName() + "' - format='" + formatString + "'",
						DATEFORMAT_DATE_ONLY.format(testDate),
						DATEFORMAT_DATE_ONLY.format(parsedDate));
			}
			
			// For a more extensive test, we can check most of the dates from 1AD to the year 3000
			if(EXTENSIVE_DATE_TEST) {
				for(int year=1; year<=3000; ++year) {
					for(int month=1; month<=12; ++month) {
						for(int day=1; day<=28; ++day) {
							// Create a date object and format it as a String.  Reparse the String and make sure that the returned date is
							// within an acceptable margin (< a single day) of the original date.
							Date testDate = getDate(year, month, day);
							String formattedDate = dateFormat.format(testDate);
							Date parsedDate = dateFormat.parse(formattedDate);
							
							assertEquals("Parsed date was incorrect for language '" + bungle.getLanguageName() + "'",
									DATEFORMAT_DATE_ONLY.format(testDate),
									DATEFORMAT_DATE_ONLY.format(parsedDate));
						}
					}
				}
			}
		}
	}
	
	/** @return {@link LanguageBundle}s loaded from resources dir 
	 * @throws IOException */
	private Collection<FileLanguageBundle> getLanguageBundles() throws IOException {
		Set<FileLanguageBundle> bundles = new HashSet<FileLanguageBundle>();
		for(File bundleFile : new File("src/main/resources/resources/languages").listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(".properties") ;
			}
		})) {
			bundles.add(FileLanguageBundle.create(bundleFile));
		}
		return bundles;
	}

	/**
	 * Test method for {@link InternationalisationUtils#mergeMaps(java.util.Map, java.util.Map)}
	 */
	public void testMergeMaps() {
		HashMap<String, String> destination = new HashMap<String, String>();
		HashMap<String, String> additionalValue = new HashMap<String, String>();
		
		destination.put("i will be there at the end", "still here");
		
		additionalValue.put("i have come from 2", "new arrival");
		
		destination.put("do not replace me", "original");
		additionalValue.put("do not replace me", "new value");
		
		InternationalisationUtils.mergeMaps(destination, additionalValue);
		
		// map contents should be merged into destination, with duplicate keys keeping the values from destination
		assertEquals(destination.get("i will be there at the end"), "still here");
		assertEquals(destination.get("i have come from 2"), "new arrival");
		assertEquals(destination.get("do not replace me"), "original");
	}
	
	public void testCurrencyParsing() throws IOException {
		List<Locale> unparsableCurrencies = Arrays.asList(new Locale[] { new Locale("hi", "in") });

		for (FileLanguageBundle languageBundle : getLanguageBundles()) {
			Locale locale = languageBundle.getLocale();
			NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(locale);
			String currencySymbol = ((DecimalFormat) currencyFormat).getDecimalFormatSymbols().getCurrencySymbol();
			if (!unparsableCurrencies.contains(locale)) {
				try {
					System.err.println("Parsing currency strings for Locale: " + locale.toString());
					assertEquals(1.4, InternationalisationUtils.parseCurrency(currencyFormat, currencySymbol, "1.4"));
					assertEquals(1.4, InternationalisationUtils.parseCurrency(currencyFormat, currencySymbol, "1,4"));
					assertEquals(0.0, InternationalisationUtils.parseCurrency(currencyFormat, currencySymbol, "0"));
					assertEquals(0.5, InternationalisationUtils.parseCurrency(currencyFormat, currencySymbol, "0.5"));
					assertEquals(0.5, InternationalisationUtils.parseCurrency(currencyFormat, currencySymbol, "0,5"));
					
					assertNotSame(1.4, InternationalisationUtils.parseCurrency(currencyFormat, currencySymbol, "1.6"));
					assertNotSame(1.4, InternationalisationUtils.parseCurrency(currencyFormat, currencySymbol, "1,2"));
					assertNotSame(0.1, InternationalisationUtils.parseCurrency(currencyFormat, currencySymbol, "0"));
					assertNotSame(0.5, InternationalisationUtils.parseCurrency(currencyFormat, currencySymbol, "0.6"));
					assertNotSame(0.5, InternationalisationUtils.parseCurrency(currencyFormat, currencySymbol, "0,495"));
				} catch (ParseException e) {
					fail(e.getMessage());
				}
			}
		}
		
		System.err.println("Now testing special currencies...");
	
		Locale locale = new Locale("hi", "in");
		System.err.println("Parsing currency strings for Locale: " + locale.toString());
		NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(locale);
		String currencySymbol = ((DecimalFormat) currencyFormat).getDecimalFormatSymbols().getCurrencySymbol();
		
		try {			
			assertEquals(1.3, InternationalisationUtils.parseCurrency(currencyFormat, currencySymbol, "\u0967.\u0969\u0966"));
			assertEquals(1.4, InternationalisationUtils.parseCurrency(currencyFormat, currencySymbol, "\u0967.\u096a\u0966"));
			assertEquals(0.5, InternationalisationUtils.parseCurrency(currencyFormat, currencySymbol, "\u0966.\u096b\u0966"));
			assertEquals(1.0, InternationalisationUtils.parseCurrency(currencyFormat, currencySymbol, "\u0967.\u0966\u0966"));
			assertEquals(1.0, InternationalisationUtils.parseCurrency(currencyFormat, currencySymbol, "\u0967"));
		} catch (ParseException e) {
			fail(e.getMessage());
		}
	}
	
	public void testGetCurrencyStringWithSymbol () {
			NumberFormat currencyFormat;
			String currencySymbol;
			
			try {
				currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "gb"));
				currencySymbol = ((DecimalFormat) currencyFormat).getDecimalFormatSymbols().getCurrencySymbol();
				assertEquals("\u00a31.4", InternationalisationUtils.getCurrencyStringWithSymbol(currencyFormat, currencySymbol, "1.4"));
				
				currencyFormat = NumberFormat.getCurrencyInstance(new Locale("fr", "fr"));
				currencySymbol = ((DecimalFormat) currencyFormat).getDecimalFormatSymbols().getCurrencySymbol();
				assertEquals("1,4 \u20ac", InternationalisationUtils.getCurrencyStringWithSymbol(currencyFormat, currencySymbol, "1.4"));
				
				currencyFormat = NumberFormat.getCurrencyInstance(new Locale("ru", "ru"));
				currencySymbol = ((DecimalFormat) currencyFormat).getDecimalFormatSymbols().getCurrencySymbol();
				assertEquals("1,4 \u0440\u0443\u0431.", InternationalisationUtils.getCurrencyStringWithSymbol(currencyFormat, currencySymbol, "1.4"));
			} catch (ParseException e) {
				fail(e.getMessage());
			}
	}
	
//> INSTANCE HELPER METHODS
	
//> STATIC HELPER METHODS
	/**
	 * Convert year, month and day of {@link #TEST_DATES} into a java {@link Date} object.
	 * @param dateDetails
	 * @return a {@link Date} object describing the supplied time.
	 */
	private static Date getDate(int[] dateDetails) {
		return getDate(dateDetails[0], dateDetails[1], dateDetails[2]);
	}
	
	/**
	 * Convert year, month and day into a java {@link Date} object.
	 * @param year the year, CE (AD)
	 * @param month 1-indexed year
	 * @param day the day of the month
	 * @return a {@link Date} object describing the supplied time.
	 */
	private static Date getDate(int year, int month, int day) {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.set(year, month - 1, day);
		return cal.getTime();
	}
}
