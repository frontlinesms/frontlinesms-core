/**
 * 
 */
package net.frontlinesms.ui.handler.importexport;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.frontlinesms.csv.CsvExporter;
import net.frontlinesms.csv.CsvRowFormat;
import net.frontlinesms.csv.CsvUtils;
import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.data.domain.FrontlineMessage.Type;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

/**
 * @author aga
 */
public class KeywordExportDialogHandler extends ExportDialogHandler<Keyword> {
	/** I18n Text Key: TODO document */
	private static final String MESSAGE_EXPORTING_SELECTED_KEYWORDS = "message.exporting.selected.keywords";
	
	/** Thinlet Component Name: TODO document */
	private static final String COMPONENT_CB_KEYWORD = "cbKeyword";
	/** Thinlet Component Name: TODO document */
	private static final String COMPONENT_CB_DESCRIPTION = "cbDescription";
	/** Thinlet Component Name: TODO document */
	private static final String COMPONENT_CB_CONTACT_NOTES = "cbContactNotes";
	/** Thinlet Component Name: TODO document */
	private static final String COMPONENT_CB_CONTACT_EMAIL = "cbContactEmail";
	/** Thinlet Component Name: TODO document */
	private static final String COMPONENT_CB_CONTACT_OTHER_NUMBER = "cbContactOtherNumber";
	/** Thinlet Component Name: TODO document */
	private static final String COMPONENT_CB_CONTACT_NAME = "cbContactName";
	/** Thinlet Component Name: TODO document */
	private static final String COMPONENT_CB_RECEIVED = "cbReceived";
	/** Thinlet Component Name: TODO document */
	private static final String COMPONENT_CB_SENT = "cbSent";
	
	public KeywordExportDialogHandler(UiGeneratorController ui) {
		super(Keyword.class, ui);
	}
	
	@Override
	protected String getWizardTitleI18nKey() {
		return MESSAGE_EXPORTING_SELECTED_KEYWORDS;
	}

	@Override
	protected String getOptionsFilePath() {
		return UI_FILE_OPTIONS_PANEL_KEYWORD;
	}
	
	@Override
	public void doSpecialExport(String dataPath) throws IOException {
		doSpecialExport(dataPath, keywordDao.getAllKeywords());
	}

	/**
	 * Export the supplied keywords using the settings in {@link #wizardDialog}.
	 * @param keywords keywords to export
	 * @param filename file to save to
	 * @throws IOException 
	 */
	@Override
	public void doSpecialExport(String dataPath, List<Keyword> keywords) throws IOException {
		//KEYWORDS
		log.debug("Exporting all keywords..");
		
		FrontlineMessage.Type messageType = getMessageType();
		CsvRowFormat rowFormat = getRowFormatForKeyword(messageType);
		if (!rowFormat.hasMarkers()) {
			uiController.alert(InternationalisationUtils.getI18nString(MESSAGE_NO_FIELD_SELECTED));
			log.trace("EXIT");
			return;
		}
		log.debug("Row Format [" + rowFormat + "]");
		CsvExporter.exportKeywords(new File(dataPath), keywords, rowFormat, this.contactDao, this.messageDao, messageType);
		uiController.setStatus(InternationalisationUtils.getI18nString(MESSAGE_EXPORT_TASK_SUCCESSFUL));
		this.uiController.infoMessage(InternationalisationUtils.getI18nString(MESSAGE_EXPORT_TASK_SUCCESSFUL));
	}
	
//> UI EVENT METHODS
	
//> PRIVATE HELPER METHODS
	
	/**
	 * Creates an export row format for keywords.
	 * @param type Type of {@link FrontlineMessage} to export, e.g. {@link Type#RECEIVED}
	 * @return The row format for exporting {@link Keyword}s to CSV
	 */
	private CsvRowFormat getRowFormatForKeyword(FrontlineMessage.Type type) {
		CsvRowFormat rowFormat = new CsvRowFormat();
		addMarker(rowFormat, CsvUtils.MARKER_KEYWORD_KEY, COMPONENT_CB_KEYWORD);
		addMarker(rowFormat, CsvUtils.MARKER_KEYWORD_DESCRIPTION, COMPONENT_CB_DESCRIPTION);

		if (type == Type.ALL) {
			rowFormat.addMarker(CsvUtils.MARKER_MESSAGE_TYPE);
		}
		addMarker(rowFormat, CsvUtils.MARKER_MESSAGE_DATE, COMPONENT_CB_DATE);
		addMarker(rowFormat, CsvUtils.MARKER_MESSAGE_CONTENT, COMPONENT_CB_CONTENT);
		addMarker(rowFormat, CsvUtils.MARKER_SENDER_NUMBER, COMPONENT_CB_SENDER);
		addMarker(rowFormat, CsvUtils.MARKER_RECIPIENT_NUMBER, COMPONENT_CB_RECIPIENT);
		addMarker(rowFormat, CsvUtils.MARKER_CONTACT_NAME, COMPONENT_CB_CONTACT_NAME);
		addMarker(rowFormat, CsvUtils.MARKER_CONTACT_OTHER_PHONE, COMPONENT_CB_CONTACT_OTHER_NUMBER);
		addMarker(rowFormat, CsvUtils.MARKER_CONTACT_EMAIL, COMPONENT_CB_CONTACT_EMAIL);
		addMarker(rowFormat, CsvUtils.MARKER_CONTACT_NOTES, COMPONENT_CB_CONTACT_NOTES);
	
		return rowFormat;
	}

	/**
	 * Get the type of {@link FrontlineMessage} that has been selected to export.
	 * @return {@link Type#ALL}, {@link Type#ALL}, {@link Type#ALL} or <code>null</code> if the user would not like any messages.
	 */
	private final FrontlineMessage.Type getMessageType() {
		boolean sent = isChecked(COMPONENT_CB_SENT);
		boolean received = isChecked(COMPONENT_CB_RECEIVED);
		
		if (sent && received) {
			return Type.ALL;
		} else if (sent) {
			return Type.OUTBOUND;
		} else if (received) {
			return Type.RECEIVED;
		} else return null;
	}
}
