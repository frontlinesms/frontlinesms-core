/**
 * 
 */
package net.frontlinesms.data.importexport;

import java.io.File;
import java.io.IOException;

import net.frontlinesms.csv.CsvImportReport;
import net.frontlinesms.csv.CsvImporter;
import net.frontlinesms.csv.CsvParseException;
import net.frontlinesms.csv.CsvRowFormat;
import net.frontlinesms.csv.CsvUtils;
import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.domain.Group;
import net.frontlinesms.data.repository.ContactDao;
import net.frontlinesms.data.repository.GroupDao;
import net.frontlinesms.data.repository.GroupMembershipDao;

/**
 * @author Carlos Eduardo Genz <kadu@masabi.com>
 * @author Alex Anderson <alex@frontlinesms.com>
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public class ContactCsvImporter extends CsvImporter {
	/** The delimiter to use between group names when they are exported. */
	protected static final String GROUPS_DELIMITER = "\\\\"; 
	
//> INSTANCE PROPERTIES
	
//> CONSTRUCTORS
	public ContactCsvImporter(File importFile) throws CsvParseException {
		super(importFile);
	}

//> IMPORT METHODS
	/**
	 * Import contacts from a CSV file.
	 * @param importFile the file to import from
	 * @param contactDao
	 * @param rowFormat 
	 * @throws IOException If there was a problem accessing the file
	 * @throws CsvParseException If there was a problem with the format of the file
	 */
	public CsvImportReport importContacts(ContactDao contactDao, GroupMembershipDao groupMembershipDao, GroupDao groupDao, CsvRowFormat rowFormat) {
		log.trace("ENTER");
		
		for(String[] lineValues : this.getRawValues()) {
			String name = rowFormat.getOptionalValue(lineValues, CsvUtils.MARKER_CONTACT_NAME);
			String number = rowFormat.getOptionalValue(lineValues, CsvUtils.MARKER_CONTACT_PHONE);
			String email = rowFormat.getOptionalValue(lineValues, CsvUtils.MARKER_CONTACT_EMAIL);
			String notes = rowFormat.getOptionalValue(lineValues, CsvUtils.MARKER_CONTACT_NOTES);
			String otherPhoneNumber = rowFormat.getOptionalValue(lineValues, CsvUtils.MARKER_CONTACT_OTHER_PHONE);
			String groups = rowFormat.getOptionalValue(lineValues, CsvUtils.MARKER_CONTACT_GROUPS);
			
			String statusString = rowFormat.getOptionalValue(lineValues, CsvUtils.MARKER_CONTACT_STATUS).toLowerCase();
			boolean active = !"false".equals(statusString) && !"dormant".equals(statusString);
			
			Contact c = new Contact(name, number, otherPhoneNumber, email, notes, active);						
			try {
				contactDao.saveContact(c);
			} catch (DuplicateKeyException e) {
				// FIXME should actually pass details of this back to the user.
				log.debug("Contact already exist with this number [" + number + "]", e);
				// If the contact already existed, let's reach the existing one to fill the groupMembership
				c = contactDao.getFromMsisdn(number);
			}
			
			// We make the contact joins its groups
			String[] pathList = groups.split(GROUPS_DELIMITER);
			for (String path : pathList) {
				if (path.length() == 0) continue;
				
				if (!path.startsWith(String.valueOf(Group.PATH_SEPARATOR))) {
					path = Group.PATH_SEPARATOR + path;
				}
				
				Group group = createGroups(groupDao, path);
				groupMembershipDao.addMember(group, c);
			}
		}
		
		log.trace("EXIT");
		return new CsvImportReport();
	}
	
//> STATIC HELPER METHODS
	/**
	 * Creates the group and all parent groups for a supplied path.
	 * The behaviour of this method is undefined if a group is deleted externally while this method
	 * is executing.
	 * @param groupDao
	 * @param path
	 * @return
	 */
	static Group createGroups(GroupDao groupDao, String path) {
		if (path.length() == 0) {
			return new Group(null, null);
		} else {
			int pos = path.lastIndexOf(Group.PATH_SEPARATOR);
			if (pos == -1) pos = 0;
			
			Group parent = createGroups(groupDao, path.substring(0, pos));
			path = path.substring(pos, path.length());
			if (path.startsWith(String.valueOf(Group.PATH_SEPARATOR))) {
				path = path.substring(1, path.length());
			}
			
			Group group = new Group(parent, path);
			try {
				groupDao.saveGroup(group);
			} catch (DuplicateKeyException ex) {
				// It's not a problem if this group already exists
			}
			
			return group;
		}
	}
}
