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

import static net.frontlinesms.FrontlineSMSConstants.COMMON_UNDEFINED;
import static net.frontlinesms.FrontlineSMSConstants.DEFAULT_END_DATE;

import java.awt.Desktop;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;

import net.frontlinesms.data.domain.*;
import net.frontlinesms.email.EmailException;
import net.frontlinesms.email.smtp.SmtpEmailSender;
import net.frontlinesms.encoding.Base64Utils;
import net.frontlinesms.resources.ResourceUtils;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Class containing general helper methods that have nowhere better to live.
 * @author Alex Anderson alex@frontlinesms.com
 */
public class FrontlineUtils {
	
//> CONSTANTS
	/** Logging object */
	private static Logger LOG = FrontlineUtils.getLogger(FrontlineUtils.class);
	/** Date formatter used in logs. */
	private static final SimpleDateFormat LOG_DATE_FORMATTER = new SimpleDateFormat();

	static {
		loadLogConfiguration();
	}

	/**
	 * Reloads the log configuration.
	 */
	static void loadLogConfiguration() {
		File f = new File(ResourceUtils.getConfigDirectoryPath() + ResourceUtils.PROPERTIES_DIRECTORY_NAME + File.separatorChar + "log4j.properties");
		if (f.exists()) {
			PropertyConfigurator.configure(f.getAbsolutePath());
		} else {
			PropertyConfigurator.configure(FrontlineUtils.class.getResource("/log4j.properties"));
		}

	}

	/**
	 * Gets the logging object for a {@link Class}, making sure the expected configuration is used.
	 * Using this method rather than {@link Logger#getLogger(Class)} directly ensures that {@link #loadLogConfiguration()} has been run.
	 * TODO This probably isn't the best way of ensuring the log config is loaded.  It seems to work okay though.
	 * @param clazz
	 * @return logging object for the supplied class
	 */
	public static Logger getLogger(Class<? extends Object> clazz) {
		return Logger.getLogger(clazz);
	}
	
	/**
	 * Gets the date passed in arguments as a long format 
	 * @param dateFieldString The string basically input in a text field
	 * @param isStartDate A boolean saying whether we're looking for a start or end date
	 * @return
	 * @throws ParseException
	 */
	public static long getLongDateFromStringDate(String dateFieldString, boolean isStartDate) throws ParseException {
		if (dateFieldString.length() == 0
				|| (!isStartDate && dateFieldString.equals(InternationalisationUtils.getI18nString(COMMON_UNDEFINED)))) {
			return (isStartDate ? System.currentTimeMillis() : DEFAULT_END_DATE); // FIXME Should we take the TimeZone into account?
		} else {
			Date ds = InternationalisationUtils.parseDate(dateFieldString);
			Calendar c = Calendar.getInstance();
			c.setTime(ds);
			
			// If we are looking for a conversion of a start date, then no worries, because the first milliseconds of
			// the given day is returned
			if (!isStartDate && !dateFieldString.contains(" ")) { // We just do that if the format is a simple format (without time)
				// Otherwise, we're looking for the last milliseconds of the given day
				// So, we seek the first millisecond of the day after
				c.add(Calendar.DATE, 1);
				// And then substitute one millisecond
				c.setTimeInMillis(c.getTimeInMillis() - 1);
			}
			return c.getTime().getTime();
		}
	}
	
	/**
	 * Converts a device manufacturer and model into a human-readable string, with extraneous information removed.
	 * @param manufacturer
	 * @param model
	 * @return string containing manufacturer and model information
	 */
	public static String getManufacturerAndModel(String manufacturer, String model) {
		if (model.startsWith(manufacturer)) model = model.substring(model.indexOf(manufacturer) + manufacturer.length()).trim();
		return manufacturer + ' ' + model;
	}

	/**
	 * Make the current thread sleep; ignore InterruptedExceptions.
	 * @param millis number of milliseconds to sleep the thread for.
	 */
	public static void sleep_ignoreInterrupts(long millis) {
		try {
			Thread.sleep(millis);
		} catch(InterruptedException ex) {
			LOG.debug("", ex);
		}
	}

	/**
	 * Returns a string with all contact groups.
	 * @param contact
	 * @param groups_delimiter
	 * @return the groups this contact is a member of represented as a string with vales separated by the requested delimiter
	 */
	public static String contactGroupsAsString(Collection<Group> groupCollection, String groups_delimiter) {
		String groups = "";
		for (Group g : groupCollection) {
			groups += g.getPath() + groups_delimiter;
		}
		if (groups.endsWith(groups_delimiter)) {
			groups = groups.substring(0, groups.length() - groups_delimiter.length());
		}
		return groups;
	}

	/**
	 * This method makes a http request and returns the response according to the supplied parameter.
	 * @param url URL to connect.
	 * @param waitForResponse <code>true</code> if this method should block and return the http response body; <code>false</code> otherwise
	 * @return the body of the http response, or empty string if the response is not requested
	 * @throws IOException
	 */
	public static String makeHttpRequest(String url, boolean waitForResponse) throws IOException {
		LOG.trace("ENTER");
		String str = "";
		URL hp = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) hp.openConnection();
		int rc = conn.getResponseCode();
		LOG.debug("RC = " + rc);
		if (rc == HttpURLConnection.HTTP_OK) {
			InputStream input = null;
			try {
				input = conn.getInputStream();
				LOG.debug("Wait for response [" + waitForResponse + "]");
				if (waitForResponse) {
					// Don't check the MIME type here - we don't want to confuse anybody
					// Get response data.
					BufferedReader inputData = new BufferedReader(new InputStreamReader(input));
					StringBuilder sb = new StringBuilder();
					while (null != (str = inputData.readLine())) {
						sb.append(str + "\n");
					}
					str = sb.toString();
				}
			} finally {
				if(input != null) try { input.close(); } catch(IOException ex) { LOG.warn("Exception closing HTTP input stream.", ex); }
			}
		}
		LOG.trace("EXIT");
		return str;
	}

	/**
	 * This method makes a http request and returns the input stream.
	 * @param url URL to connect.
	 * @return
	 * @throws IOException
	 */
	public static InputStream makeHttpRequest(String url) throws IOException {
		LOG.trace("ENTER");
		URL hp = new URL(url);
		URLConnection conn = hp.openConnection();
		conn.connect();
		LOG.trace("EXIT");
		return conn.getInputStream();
	}

	/**
	 * This method executes a external command and returns the input stream.
	 * @param cmd Command to be executed.
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static InputStream executeExternalProgram(String cmd) throws IOException, InterruptedException {
		LOG.trace("ENTER");
		Process p = Runtime.getRuntime().exec(cmd);
		p.waitFor();
		LOG.trace("EXIT");
		return p.getInputStream();
	}

	/**
	 * This method executes a external command and returns the response according to the supplied parameter.
	 * @param cmd Command to be executed.
	 * @param waitForResponse <code>true</code> if the command's response should be returned.  If <code>true</code>, this method blocks.
	 * @return empty string if waitForResponse is <code>false</code> or the response was an error, or the standard output text of the command if waitForResponse was <code>true</code>
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static String executeExternalProgram(String cmd, boolean waitForResponse) throws IOException, InterruptedException {
		LOG.trace("ENTER");
		String str = "";
		Process p = Runtime.getRuntime().exec(cmd);
		LOG.debug("Wait for response [" + waitForResponse + "]");
		if (waitForResponse) {
			int exit = p.waitFor();
			LOG.debug("Process exit value [" + exit + "]");
			if (exit == 0) {
				InputStream inputStream = null;
				try {
					inputStream = p.getInputStream();
					BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
					StringBuilder sb = new StringBuilder();
					while (null != ((str = br.readLine()))) {
						sb.append(str + "\n");
					}
					str = sb.toString();
				} finally {
					if(inputStream != null) try { inputStream.close(); } catch(IOException ex) { LOG.warn("Error closing external program input stream.", ex); }
				}
			}
		}
		LOG.trace("EXIT");
		return str;
	}

	/**
	 * Encodes the supplied string into Base64.
	 * @param password the string to encode
	 * @return base64-encoded string
	 */
	public static String encodeBase64(String password) {
		try {
			return Base64Utils.encode(password.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException ex) {
			return Base64Utils.encode(password.getBytes());
		}
	}

	/**
	 * Decodes the supplied string from Base64.
	 * @param passwordEncoded
	 * @return decoded value of the supplied base64 string
	 */
	public static String decodeBase64(String passwordEncoded) {
		byte[] data = Base64Utils.decode(passwordEncoded);
		String decoded;
		try {
			decoded = new String(data, "UTF-8");
		} catch(UnsupportedEncodingException ex) {
			return new String(data);
		}
		return decoded;
	}

	/**
	 * This class compares files and directories, giving higher priority to directories.
	 * 
	 * @author Carlos Eduardo Genz
	 */
	public static class FileComparator implements Comparator<File> {
		/**
		 * compares files and directories, giving higher priority to directories.
		 */
		public int compare(File arg0, File arg1) {
			if (arg0.isDirectory() && arg1.isDirectory()) {
				return 0;
			} else if (arg0.isDirectory() && !arg1.isDirectory()) {
				return -1;
			} else return 1;
		}
	}

	/** prioritised guess of users' browser preference */
	private static final String[] BROWSERS = {"epiphany", "firefox", "mozilla", "konqueror", "netscape", "opera", "links", "lynx"};
	
	/** Number of milliseconds in a day */
	private static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000;
	
	/**
	 * <p>This method assumes that any URLs starting with something other than http:// are
	 * links to the FrontlineSMS help manual.  On Linux and Windows machines, this is
	 * assumed to be local.  On Mac OSX, we link to a website.</p>
	 * <p>This code was adapted from http://www.centerkey.com/java/browser/.  The original code
	 * is in the public domain.</p>
	 * @param url
	 */
	public static void openExternalBrowser(String url) {
		Runtime rt = Runtime.getRuntime();
	
		try {
			if (isWindowsOS()) {
				LOG.info("Attempting to open URL with Windows-specific code");
				String[] cmd = new String[4];
				cmd[0] = "cmd.exe";
				cmd[1] = "/C";
				cmd[2] = "start";
				cmd[3] = url;
				rt.exec(cmd);
			} else if (isMacOS()) {
				LOG.info("Attempting to open URL with Mac-specific code");

				// TODO here, we are trying to launch the browser twice.  This looks to only open
				// one browser instance, so I'd guess one of them was failing.  If it's the same
				// one every time, we can get rid of the other method.
				LOG.debug("Trying to open with rt.exec....");
				rt.exec("open " + url);

				LOG.debug("Trying to open with FileManager...");
				Class<?> fileManager = Class.forName("com.apple.eio.FileManager");
				Method openURL = fileManager.getDeclaredMethod("openURL", String.class);
				openURL.invoke(null, new Object[] {url});
			} else {
				LOG.info("Attempting to open URL with default code");
				StringBuilder cmd = new StringBuilder();
				for (int i=0; i < BROWSERS.length; i++)
					cmd.append( (i==0  ? "" : " || " ) + BROWSERS[i] +" \"" + url + "\" ");

				rt.exec(new String[] { "sh", "-c", cmd.toString() });
			}
		} catch (Throwable t) {
			LOG.warn("Could not open browser (" + url + ")", t);
		}
	}
	
	/**
	 * Builds the URL of the web page depending on the OS
	 * @param page The help page
	 */
	public static void openHelpPageInBrowser(String page) {
		// TODO Try to resolve permission problem for mac and then open local help files
		// It seems like we don't have permission to open a file inside a .app package
		// on OSX.  While we are packaging the mac version as a .app, we access help on
		// the FrontlineSMS website.
		String url = "help/" + page;
		if (!new File(url).exists()) {
			if (!page.toLowerCase().startsWith("http")) {
				url = getOnlineHelpUrl(page);
			} else {
				url = page;
			}
		}
		
		openExternalBrowser(url);
	}
	
	/**
	 * Opens an email editor in the default email client
	 * @param uri
	 * @throws IOException 
	 */
	public static void openDefaultMailClient(URI uri) throws IOException {
		Desktop.getDesktop().mail(uri.resolve(uri.toString().replace("#", "")));
	}
	
	private final static String getOnlineHelpUrl(String page) {
		return "http://help.frontlinesms.com/manuals/" + BuildProperties.getInstance().getVersion() + "/" + page;
	}

	/**
	 * Checks if the User's OS is a Mac OS
	 * @return <code>true</code> if the OS is Mac OS, <code>false</code> otherwise.
	 */
	public static boolean isMacOS () {
		String os = System.getProperty("os.name").toLowerCase();
		return os.startsWith("mac");		
	}
	
	/**
	 * Checks if the User's OS is a Windows OS
	 * @return <code>true</code> if the OS is Windows, <code>false</code> otherwise.
	 */
	public static boolean isWindowsOS () {
		String os = System.getProperty("os.name").toLowerCase();
		return os.startsWith("win");		
	}

	/**
	 * Creates an image from the specified resource.
	 * To speed up loading the same images use a cache (a simple hashtable).
	 * And flush the resources being used by an image when you won't use it henceforward
	 *
	 * @param path is relative or the classpath, or an URL
	 * @param clazz TODO
	 * @return the loaded image or null
	 */
	public static Image getImage(String path, Class<?> clazz) {
		if ((path == null) || (path.length() == 0)) {
			return null;
		}
		Image image = null; //(Image) imagepool.get(path);
		try {
			URL url = clazz.getResource(path); //ClassLoader.getSystemResource(path)
			if (url != null) { // contributed by Stefan Matthias Aust
				image = Toolkit.getDefaultToolkit().getImage(url);
			}
		} catch (Throwable e) {}
		if (image == null) {
			try {
				InputStream is = clazz.getResourceAsStream(path);
				//InputStream is = ClassLoader.getSystemResourceAsStream(path);
				if (is != null) {
					byte[] data = new byte[is.available()];
					is.read(data, 0, data.length);
					image = Toolkit.getDefaultToolkit().createImage(data);
					is.close();
				}
				else { // contributed by Wolf Paulus
					image = Toolkit.getDefaultToolkit().getImage(new URL(path));
				}
			} catch (Throwable e) {}
		}
		return image;
	}

	/**
	 * Formats a date for use in logging - should use default SimpleDateFormat
	 * style rather than a localised format.  For this reason, this should not
	 * be used for dates that are to be displayed to the user.
	 * @param date the date to be formatted
	 * @return date string represetnation of a date, suitably formatted for use in logs
	 */
	public static String log_formatDate(long date) {
		return LOG_DATE_FORMATTER.format(new Date(date));
	}

	/**
	 * Calls {@link URLEncoder#encode(String, String)} using UTF-8 as the encoding.  If somehow
	 * an {@link UnsupportedEncodingException} is thrown, this method will just return the original
	 * {@link String} supplied.  This method will also ignore <code>null</code> inputs, rather than
	 * throwing a {@link NullPointerException}.
	 * @param string
	 * @return url-encoded string
	 */
	public static String urlEncode(String string) {
		if(string == null) return null;
		try {
			string = URLEncoder.encode(string, "UTF-8");
		} catch (UnsupportedEncodingException e) { /* This will never happen - UTF-8 should always be supported by every JVM. */ }
		return string;
	}

	/**
	 * Calls {@link URLDecoder#decode(String, String)} using UTF-8 as the encoding.  If somehow
	 * an {@link UnsupportedEncodingException} is thrown, this method will just return the original
	 * {@link String} supplied.  This method will also ignore <code>null</code> inputs, rather than
	 * throwing a {@link NullPointerException}.
	 * @param string
	 * @return url-decoded string
	 */	
	public static String urlDecode(String string) {
		if(string == null) return null;
		try {
			string = URLDecoder.decode(string, "UTF-8").trim();
		} catch (UnsupportedEncodingException e) { /* This will never happen - UTF-8 should always be supported by every JVM. */ }
		return string;
	}

	/**
	 * Gets the name of the supplied file without the extension.
	 * @param file
	 */
	public static String getFilenameWithoutFinalExtension(File file) {
		return getFilenameWithoutFinalExtension(file.getName());
	}
	public static String getFilenameWithoutFinalExtension(String filename) {
		int dotIndex = filename.lastIndexOf('.');
		if(dotIndex > -1) filename = filename.substring(0, dotIndex);
		return filename;
	}

	/**
	 * Gets the name of the supplied file without any extension.
	 * @param file
	 */
	public static String getFilenameWithoutAnyExtension(File file) {
		return getFilenameWithoutAnyExtension(file.getName());
	}
	public static String getFilenameWithoutAnyExtension(String filename) {
		int dotIndex = filename.indexOf('.');
		if(dotIndex > -1) filename = filename.substring(0, dotIndex);
		return filename;
	}

	public static String getFinalFileExtension(File file) {
		return getFinalFileExtension(file.getName());
	}
	/** return the extension of a file */
	public static String getFinalFileExtension(String filename) {
		int dotIndex = filename.lastIndexOf('.');
		if(dotIndex == -1) return "";
		else return filename.substring(dotIndex+1);
	}

	public static String getWholeFileExtension(File file) {
		return getWholeFileExtension(file.getName());
	}
	/** return the extension of a file */
	public static String getWholeFileExtension(String filename) {
		int dotIndex = filename.indexOf('.');
		if(dotIndex == -1) return "";
		else return filename.substring(dotIndex+1);
	}
	
	/**
	 * Send an E-Mail to the FrontlineSMS Support email account.
	 * TODO smtp sending should be refactored into email.smtp.SmtpMessageSender
	 * @param fromName
	 * @param fromEmailAddress
	 * @param attachment
	 * @throws MessagingException
	 */
	public static void sendToFrontlineSupport(String fromName, String fromEmailAddress, String subject, String textContent, String attachment) throws EmailException {
		sendEmail(FrontlineSMSConstants.FRONTLINE_SUPPORT_EMAIL, fromName, fromEmailAddress, subject, textContent, attachment);
	}
	
	/**
	 * Send an E-Mail to the given e-mail address.
	 * TODO smtp sending should be refactored into email.smtp.SmtpMessageSender
	 * @param fromName
	 * @param fromEmailAddress
	 * @param attachment
	 * @throws MessagingException
	 */
	public static void sendEmail(String recipientEmailAddress, String fromName, String fromEmailAddress, String subject, String textContent, String attachment) throws EmailException {
		SmtpEmailSender emailSender = new SmtpEmailSender(FrontlineSMSConstants.FRONTLINE_SUPPORT_EMAIL_SERVER);
	
	    emailSender.sendEmail(recipientEmailAddress,
							  emailSender.getLocalEmailAddress(fromEmailAddress, fromName),
							  subject,
							  textContent,
							  new File(attachment));
	}

	/**
	 * Used to calculate the exact last moment a date should 
	 * @param date
	 * @return
	 */
	public static Long getFirstMillisecondOfNextDay(Date date) {
		return date.getTime() + MILLIS_PER_DAY;
	}

	public static boolean isSimpleFormat(Date date) {
		Calendar cal = new GregorianCalendar();
		cal.setTime(date);
		
		return cal.get(Calendar.HOUR_OF_DAY) == 0 
			&& cal.get(Calendar.MINUTE) == 0
			&& cal.get(Calendar.MILLISECOND) == 0;
	}
}
