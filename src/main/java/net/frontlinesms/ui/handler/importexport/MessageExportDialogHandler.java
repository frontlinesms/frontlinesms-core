/**
 * 
 */
package net.frontlinesms.ui.handler.importexport;

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.frontlinesms.csv.CsvExporter;
import net.frontlinesms.csv.CsvRowFormat;
import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

/**
 * @author aga
 */
public class MessageExportDialogHandler extends ExportDialogHandler<FrontlineMessage> {
	/** I18n Text Key: TODO document */
	private static final String MESSAGE_EXPORTING_SELECTED_MESSAGES = "message.exporting.selected.messages";
	
	public MessageExportDialogHandler(UiGeneratorController ui) {
		super(FrontlineMessage.class, ui);
	}
	
	@Override
	protected String getWizardTitleI18nKey() {
		return MESSAGE_EXPORTING_SELECTED_MESSAGES;
	}
	
	@Override
	protected String getOptionsFilePath() {
		return UI_FILE_OPTIONS_PANEL_MESSAGE;
	}
	
	@Override
	public void doSpecialExport(String dataPath) throws IOException {
		doSpecialExport(dataPath, messageDao.getAllMessages());
	}

	/**
	 * Export the supplied {@link FrontlineMessage}s using settings set in {@link #wizardDialog}.
	 * @param messages The messages to export
	 * @param dataPath The file to export the contacts to
	 * @throws IOException 
	 */
	@Override
	public void doSpecialExport(String dataPath, List<FrontlineMessage> messages) throws IOException {
		CsvRowFormat rowFormat = getRowFormatForMessage();
		if (!rowFormat.hasMarkers()) {
			uiController.alert(InternationalisationUtils.getI18nString(MESSAGE_NO_FIELD_SELECTED));
			log.trace("EXIT");
			return;
		}
		if(log.isDebugEnabled()) log.debug("Row Format: " + rowFormat);
		CsvExporter.exportMessages(new File(dataPath), messages, rowFormat, contactDao);
		uiController.setStatus(InternationalisationUtils.getI18nString(MESSAGE_EXPORT_TASK_SUCCESSFUL));
		this.uiController.infoMessage(InternationalisationUtils.getI18nString(MESSAGE_EXPORT_TASK_SUCCESSFUL));
	}
}
