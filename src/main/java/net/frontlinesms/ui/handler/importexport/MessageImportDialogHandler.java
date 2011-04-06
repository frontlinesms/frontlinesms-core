package net.frontlinesms.ui.handler.importexport;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.frontlinesms.csv.CsvImporter;
import net.frontlinesms.csv.CsvParseException;
import net.frontlinesms.csv.CsvRowFormat;
import net.frontlinesms.data.importexport.MessageCsvImporter;
import net.frontlinesms.ui.Icon;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.i18n.LanguageBundle;

public class MessageImportDialogHandler extends ImportDialogHandler {
	/** I18n Text Key: TODO document */
	private static final String MESSAGE_IMPORTING_SELECTED_MESSAGES = "message.importing.messages";
	private static final String I18N_MULTIMEDIA_MESSAGES_IMPORT_SUCCESSFUL = "importexport.import.multimedia.messages.successful";
	
//> INSTANCE PROPERTIES
	private LanguageBundle usedLanguageBundle;
	private MessageCsvImporter importer;
	private int messageTypeColumnIndex;
	private int columnCount;
	
	public MessageImportDialogHandler(UiGeneratorController ui) {
		super(ui);
	}
	
	@Override
	protected String getWizardTitleI18nKey() {
		return MESSAGE_IMPORTING_SELECTED_MESSAGES;
	}
	
	@Override
	protected String getOptionsFilePath() {
		return UI_FILE_OPTIONS_PANEL_MESSAGE;
	}
	
	@Override
	protected CsvImporter getImporter() {
		return this.importer;
	}
	
	@Override
	protected void setImporter(String filename) throws CsvParseException {
		this.importer = new MessageCsvImporter(new File(filename));
	}
	
	@Override
	protected void doSpecialImport(String dataPath) throws CsvParseException {
		CsvRowFormat rowFormat = getRowFormatForMessage();
		int multimediaMessagesCount = importer.importMessages(this.messageDao, rowFormat).getMultimediaMessageCount(); // FIXME importer should be of known type depending on the handler we are in
		
		if (multimediaMessagesCount == 0) {
			this.uiController.infoMessage(InternationalisationUtils.getI18nString(I18N_IMPORT_SUCCESSFUL));
		} else {
			this.uiController.infoMessage(InternationalisationUtils.getI18nStrings(I18N_MULTIMEDIA_MESSAGES_IMPORT_SUCCESSFUL, String.valueOf(multimediaMessagesCount)).toArray(new String[0]));
		}
	}
	
	@Override
	protected void appendPreviewHeaderItems(Object header) {
		int columnCount = 0;
		for (Object checkbox : getCheckboxes()) {
			if (this.uiController.isSelected(checkbox)) {
				String attributeName = this.uiController.getText(checkbox);
				if (this.uiController.getName(checkbox).equals(COMPONENT_CB_TYPE)) {
					this.messageTypeColumnIndex = columnCount;
				}
				this.uiController.add(header, this.uiController.createColumn(attributeName, attributeName));
				++columnCount;
			}
		}
		this.columnCount = columnCount;
	}
	
	@Override
	protected Object[] getPreviewRows() {
		List<String[]> lines = this.importer.getRawValues();
		ArrayList<Object> previewRows = new ArrayList<Object>(lines.size());
		for (String[] lineValues : lines) {
			previewRows.add(getRow(lineValues));
		}
		return previewRows.toArray();
	}
	
	private Object getRow(String[] lineValues) {
		Object row = this.uiController.createTableRow();
		
		String rowIcon = getIconFromI18nString(lineValues);
		Object iconCell = this.uiController.createTableCell("");
		this.uiController.setIcon(iconCell, rowIcon);
		this.uiController.add(row, iconCell);

		for (int i = 0; i < this.columnCount && i < lineValues.length; ++i) {
			String cellValue = lineValues[i];
			this.uiController.add(row, this.uiController
					.createTableCell(cellValue));
		}

		return row;
	}
	
	private List<Object> getCheckboxes() {
		List<Object> allCheckboxes = new ArrayList<Object>();
		
		Object pnCheckboxes = this.uiController.find(this.wizardDialog, COMPONENT_PN_CHECKBOXES);
		allCheckboxes.addAll(Arrays.asList(this.uiController.getItems(pnCheckboxes)));
		
		Object pnCheckboxes2 = this.uiController.find(this.wizardDialog, COMPONENT_PN_CHECKBOXES_2);
		allCheckboxes.addAll(Arrays.asList(this.uiController.getItems(pnCheckboxes2)));
		
		return allCheckboxes;
	}

	private String getIconFromI18nString(String[] lineValues) {
		if(messageTypeColumnIndex != -1
				&& usedLanguageBundle != null) {
			switch (MessageCsvImporter.getTypeFromString(lineValues[messageTypeColumnIndex], usedLanguageBundle)) {
				case OUTBOUND :
					return Icon.SMS_SEND;
				case RECEIVED :
					return Icon.SMS_RECEIVE;
			}
		}
		return Icon.SMS;
	}
}
