/**
 * 
 */
package net.frontlinesms.ui.handler.importexport;

import java.io.File;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.log4j.Logger;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.csv.CsvExporter;
import net.frontlinesms.csv.CsvRowFormat;
import net.frontlinesms.csv.CsvUtils;
import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.data.repository.ContactDao;
import net.frontlinesms.data.repository.GroupDao;
import net.frontlinesms.data.repository.GroupMembershipDao;
import net.frontlinesms.data.repository.KeywordDao;
import net.frontlinesms.data.repository.MessageDao;
import net.frontlinesms.ui.FrontlineUI;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.handler.core.FileChooser;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.i18n.TextResourceKeyOwner;

/**
 * UI Methods for Importing and Exporting data from FrontlineSMS.
 * @author Alex Anderson <alex@frontlinesms.com>
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 * @author Ian Onesmus Mukewa <ian@credit.frontlinesms.com>
 */
@TextResourceKeyOwner(prefix="MESSAGE_")
public abstract class ImportExportDialogHandler implements ThinletUiEventHandler {
//> STATIC CONSTANTS

//> I18N KEYS
	/** I18n Text Key: TODO document */
	protected static final String MESSAGE_NO_FILENAME = "message.filename.blank";

//> THINLET LAYOUT DEFINITION FILES
	/** UI XML File Path: TODO document */
	protected static final String UI_FILE_OPTIONS_PANEL_CONTACT = "/ui/core/importexport/pnContactDetails.xml";
	/** UI XML File Path: TODO document */
	protected static final String UI_FILE_OPTIONS_PANEL_MESSAGE = "/ui/core/importexport/pnMessageDetails.xml";
	/** UI XML File Path: TODO document */
	protected static final String UI_FILE_OPTIONS_PANEL_KEYWORD = "/ui/core/importexport/pnKeywordDetails.xml";

//> THINLET COMPONENT NAMES
	/** Thinlet Component Name: Checkbox to indicate whether a contact's "notes" field should be exported. */
	private static final String COMPONENT_CB_NOTES = "cbContactNotes";
	/** Thinlet Component Name: TODO document */
	private static final String COMPONENT_CB_EMAIL = "cbContactEmail";
	/** Thinlet Component Name: TODO document */
	public static final String COMPONENT_CB_GROUPS = "cbGroups";
	/** Thinlet Component Name: TODO document */
	private static final String COMPONENT_CB_OTHER_PHONE = "cbOtherPhone";
	/** Thinlet Component Name: TODO document */
	private static final String COMPONENT_CB_PHONE = "cbPhone";
	/** Thinlet Component Name: TODO document */
	private static final String COMPONENT_CB_NAME = "cbName";
	/** Thinlet Component Name: TODO document */
	protected static final String COMPONENT_CB_STATUS = "cbStatus";
	/** Thinlet Component Name: TODO document */
	protected static final String COMPONENT_CB_TYPE = "cbType";
	/** Thinlet Component Name: TODO document */
	protected static final String COMPONENT_CB_CONTENT = "cbContent";
	/** Thinlet Component Name: TODO document */
	protected static final String COMPONENT_CB_RECIPIENT = "cbRecipient";
	/** Thinlet Component Name: TODO document */
	protected static final String COMPONENT_CB_SENDER = "cbSender";
	/** Thinlet Component Name: TODO document */
	protected static final String COMPONENT_CB_DATE = "cbDate";
	/** Thinlet component name: button for executing EXPORT action */
	private static final String COMPONENT_BT_DO_EXPORT = "btDoExport";
	/** Thinlet component name: list displaying values from the CSV file */
	private static final String COMPONENT_PN_DETAILS = "pnDetails";

//> INSTANCE PROPERTIES
	/** Logging object */
	protected final Logger log = FrontlineUtils.getLogger(this.getClass());
	/** Data access object for {@link Contact}s */
	protected final ContactDao contactDao;
	/** Data access object for determining group memberships */
	protected final GroupMembershipDao groupMembershipDao;
	/** Data access object for {@link Group}s */
	protected final GroupDao groupDao;
	/** Data access object for {@link FrontlineMessage}s */
	protected final MessageDao messageDao;
	/** Data access object for {@link Keyword}s */
	protected final KeywordDao keywordDao;
	/** The {@link UiGeneratorController} that shows the tab. */
	protected final UiGeneratorController uiController;

	/** Dialog for gathering details of the export or import */
	protected Object wizardDialog;

	/** Main panel for the Details */
	protected Object pnDetails;

	/** Main panel holding The Options */
	protected Object optionsPanel;

//> CONSTRUCTORS
	/**
	 * Create a new instance of this controller.
	 * @param uiController 
	 */
	public ImportExportDialogHandler(UiGeneratorController uiController) {
		this.uiController = uiController;
		this.contactDao = uiController.getFrontlineController().getContactDao();
		this.groupMembershipDao = uiController.getFrontlineController().getGroupMembershipDao();
		this.messageDao = uiController.getFrontlineController().getMessageDao();
		this.keywordDao = uiController.getFrontlineController().getKeywordDao();
		this.groupDao = uiController.getFrontlineController().getGroupDao();
		
		init();
	}

	private void init() {
		// Load the import/export wizard, and save it to the class reference
		this.wizardDialog = uiController.loadComponentFromFile(getDialogFile(),
				this);

		String titleI18nKey = getWizardTitleI18nKey();
		uiController.setText(this.wizardDialog,
				InternationalisationUtils.getI18nString(titleI18nKey));

		this.pnDetails = this.uiController.find(this.wizardDialog,
				COMPONENT_PN_DETAILS);

		this.optionsPanel = uiController.loadComponentFromFile(getOptionsFilePath(), this);

		if (pnDetails == null) {
			uiController.add(this.wizardDialog, optionsPanel, 2);
		} else {
			uiController.add(pnDetails, optionsPanel);
		}

	}

//> ACCESSORS

//> UI SHOW METHODS
	/**
	 * Shows the export wizard dialog, according to the supplied type.
	 * @param export 
	 */
	public void showWizard() {
		_showWizard();
	}

//> PUBLIC UI METHODS
	public void filenameModified(String text) {
		boolean enableExport = FrontlineUtils.getFilenameWithoutFinalExtension(new File(text)).length() > 0;
		uiController.setEnabled(uiController.find(this.wizardDialog, COMPONENT_BT_DO_EXPORT), enableExport);
	}

//> INSTANCE HELPER METHODS
	/**
	 * Gets the title to use for the title of Export wizard
	 * @return i18n key for fetching the title of the wizard
	 */
	protected abstract String getWizardTitleI18nKey();

	protected abstract String getOptionsFilePath();

	protected abstract String getDialogFile();

	/** Show the wizard for importing or exporting a particular type of entity. */
	protected void _showWizard() {
		//The Init and Show are separate to allow manipulation
		// Add the wizard to the Thinlet controller
		uiController.add(this.wizardDialog);
	}

	/**
	 * Checks if a Thinlet checkbox component is checked.
	 * @param checkboxComponentName The name of the checkbox component.
	 * @return <code>true</code> if the checkbox is checked
	 */
	protected boolean isChecked(String checkboxComponentName) {
		assert (this.wizardDialog != null) : "The exportDialog property is currently null.  Should be set when the dialog is displayed.";
		Object cbComponent = uiController.find(wizardDialog, checkboxComponentName);
		assert (cbComponent != null) : "The checkbox component could not be found with name: " + checkboxComponentName;
		return this.uiController.isSelected(cbComponent);
	}

	/**
	 * Adds a marker to the {@link CsvRowFormat} iff the checkbox is checked.
	 * @param rowFormat
	 * @param marker
	 * @param checkboxComponentName
	 */
	protected void addMarker(CsvRowFormat rowFormat, String marker, String checkboxComponentName) {
		if(isChecked(checkboxComponentName)) {
			rowFormat.addMarker(marker);
		}
	}

	/**
	 * Creates an export row format for messages.
	 * @return {@link CsvRowFormat} for message, reflecting the settings in {@link #wizardDialog}
	 */
	protected CsvRowFormat getRowFormatForMessage() {
		CsvRowFormat rowFormat = new CsvRowFormat();
		addMarker(rowFormat, CsvUtils.MARKER_MESSAGE_TYPE, COMPONENT_CB_TYPE);
		addMarker(rowFormat, CsvUtils.MARKER_MESSAGE_STATUS, COMPONENT_CB_STATUS);
		addMarker(rowFormat, CsvUtils.MARKER_MESSAGE_DATE, COMPONENT_CB_DATE);
		addMarker(rowFormat, CsvUtils.MARKER_MESSAGE_CONTENT, COMPONENT_CB_CONTENT);
		addMarker(rowFormat, CsvUtils.MARKER_SENDER_NUMBER, COMPONENT_CB_SENDER);
		addMarker(rowFormat, CsvUtils.MARKER_RECIPIENT_NUMBER, COMPONENT_CB_RECIPIENT);
		return rowFormat;
	}

	/**
	 * Creates an export row format for {@link Contact}s.
	 * @return {@link CsvRowFormat} for contacts, reflecting the settings in {@link #wizardDialog}
	 */
	protected CsvRowFormat getRowFormatForContact() {
		CsvRowFormat rowFormat = new CsvRowFormat();
		addMarker(rowFormat, CsvUtils.MARKER_CONTACT_NAME, COMPONENT_CB_NAME);
		addMarker(rowFormat, CsvUtils.MARKER_CONTACT_PHONE, COMPONENT_CB_PHONE);
		addMarker(rowFormat, CsvUtils.MARKER_CONTACT_OTHER_PHONE, COMPONENT_CB_OTHER_PHONE);
		addMarker(rowFormat, CsvUtils.MARKER_CONTACT_EMAIL, COMPONENT_CB_EMAIL);
		addMarker(rowFormat, CsvUtils.MARKER_CONTACT_STATUS, COMPONENT_CB_STATUS);
		addMarker(rowFormat, CsvUtils.MARKER_CONTACT_NOTES, COMPONENT_CB_NOTES);
		addMarker(rowFormat, CsvUtils.MARKER_CONTACT_GROUPS, COMPONENT_CB_GROUPS);
		return rowFormat;
	}

//> UI PASS-THRU METHODS
	/** @param dialog the dialog to remove
	 * @see UiGeneratorController#remove(Object) */
	public void removeDialog(Object dialog) {
		this.uiController.remove(dialog);
	}
	
	/** @param textFieldToBeSet Thinlet textfield whose value will be set with the selected file
	 * @see FrontlineUI#showOpenModeFileChooser(Object) */
	public void showOpenModeFileChooser() {
		FileChooser fc = FileChooser.createFileChooser(this.uiController, this, "openChooseComplete");
		fc.setFileFilter(new FileNameExtensionFilter("FrontlineSMS Exported Data (" + CsvExporter.CSV_EXTENSION + ")", CsvExporter.CSV_FORMAT));
		fc.show();
	}
	
	/** @param textFieldToBeSet Thinlet textfield whose value will be set with the selected file
	 * @see FrontlineUI#showOpenModeFileChooser(Object) */
	public void showSaveModeFileChooser(Object textFieldToBeSet) {
		FileChooser fc = FileChooser.createFileChooser(this.uiController, this, "saveChooseComplete");
		fc.show();
	}
	
	public void saveChooseComplete(String filename) {
		uiController.setText(uiController.find(this.wizardDialog, "tfFilename"), filename);
		filenameModified(filename);
	}

//> STATIC FACTORIES

//> STATIC HELPER METHODS
}
