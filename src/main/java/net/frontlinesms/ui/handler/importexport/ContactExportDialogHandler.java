/**
 * 
 */
package net.frontlinesms.ui.handler.importexport;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.frontlinesms.csv.CsvExporter;
import net.frontlinesms.csv.CsvRowFormat;
import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

/**
 * @author aga
 */
public class ContactExportDialogHandler extends ExportDialogHandler<Contact> {
	/** I18n Text Key: TODO document */
	private static final String MESSAGE_EXPORTING_SELECTED_CONTACTS = "message.exporting.selected.contacts";
	
	public ContactExportDialogHandler(UiGeneratorController ui) {
		super(Contact.class, ui);
	}

	@Override
	protected String getWizardTitleI18nKey() {
		return MESSAGE_EXPORTING_SELECTED_CONTACTS;
	}
	
	@Override
	protected String getOptionsFilePath() {
		return UI_FILE_OPTIONS_PANEL_CONTACT;
	}
	
	@Override
	public void doSpecialExport(String dataPath) throws IOException {
		log.debug("Exporting all contacts..");
		exportContacts(this.contactDao.getAllContacts(), dataPath);
	}

	@Override
	public void doSpecialExport(String dataPath, List<Contact> selected) throws IOException {
		exportContacts(selected, dataPath);
	}
	
	/**
	 * Export the supplied contacts using settings set in {@link #wizardDialog}.
	 * @param contacts The contacts to export
	 * @param filename The file to export the contacts to
	 * @throws IOException 
	 */
	private void exportContacts(List<Contact> contacts, String filename) throws IOException {
		CsvRowFormat rowFormat = getRowFormatForContact();
		
		if (!rowFormat.hasMarkers()) {
			uiController.alert(InternationalisationUtils.getI18nString(MESSAGE_NO_FIELD_SELECTED));
			log.trace("EXIT");
			return;
		}
		
		log.debug("Row Format [" + rowFormat + "]");
		
		CsvExporter.exportContacts(new File(filename), contacts, groupMembershipDao, rowFormat);
		uiController.setStatus(InternationalisationUtils.getI18nString(MESSAGE_EXPORT_TASK_SUCCESSFUL));
		this.uiController.infoMessage(InternationalisationUtils.getI18nString(MESSAGE_EXPORT_TASK_SUCCESSFUL));
	}
}
