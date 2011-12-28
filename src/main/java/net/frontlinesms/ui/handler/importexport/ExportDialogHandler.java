/**
 * 
 */
package net.frontlinesms.ui.handler.importexport;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import net.frontlinesms.csv.CsvExporter;
import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

/**
 * @author aga
 */
public abstract class ExportDialogHandler<T> extends ImportExportDialogHandler {
	/** UI XML File Path: This is the outline for the dialog for EXPORTING */
	private static final String UI_FILE_EXPORT_WIZARD_FORM = "/ui/core/importexport/exportWizardForm.xml";
	
	/** i18n Text Key: "A file with this name already exists.  Would you like to overwrite it?" */
	private static final String MESSAGE_CONFIRM_FILE_OVERWRITE = "message.file.overwrite.confirm";
	/** i18n Text Key: "The directory you entered doesn't exist" */
	private static final String MESSAGE_BAD_DIRECTORY = "message.bad.directory";
	/** I18n Text Key: TODO document */
	private static final String MESSAGE_EXPORT_TASK_FAILED = "message.export.failed";
	/** I18n Text Key: TODO document */
	protected static final String MESSAGE_NO_FIELD_SELECTED = "message.no.field.selected";
	/** I18n Text Key: TODO document */
	protected static final String MESSAGE_EXPORT_TASK_SUCCESSFUL = "message.export.successful";
	
//> INSTANCE PROPERTIES
	private Class<T> exportClass;
	/** The objects we are exporting - a selection of thinlet components with attached {@link Contact}s, {@link Keyword}s or {@link FrontlineMessage}s */
	protected Object attachedObject;
	/** Used to store the confirmation dialog while it is being displayed, so that we can remove it later. */
	private Object confirmationDialog;
	
//> CONSTRUCTORS
	public ExportDialogHandler(Class<T> exportClass, UiGeneratorController ui) {
		super(ui);
		this.exportClass = exportClass;
	}

//> ACCESSORS
	@Override
	protected String getDialogFile() {
		return UI_FILE_EXPORT_WIZARD_FORM;
	}
	
//> PUBLIC METHODS
	/**
	 * Shows the export wizard dialog, according to the supplied type.
	 * @param export 
	 * @param list The list to get selected items from.
	 */
	public void showWizard(Object list){
		Object[] selected = uiController.getSelectedItems(list);
		if (selected.length == 0) {
			// If there are no highlighted items to export, don't do anything
			return;
		}

		this.attachedObject = selected;
		
		super._showWizard();
		
		uiController.setAttachedObject(this.wizardDialog, attachedObject);
	}
	
//> UI EVENT METHODS
	/**
	 * Executes the export action.
	 * @param dataPath The path to the file to export data to.
	 */
	public void handleDoExport(String dataPath) {
		log.trace("ENTER");
		
		// Check if the file already exists.  If it does, show a warning.
		if (!dataPath.contains(File.separator) || !(new File(dataPath.substring(0, dataPath.lastIndexOf(File.separator))).isDirectory())) {
			this.uiController.alert(InternationalisationUtils.getI18nString(MESSAGE_BAD_DIRECTORY));
		} else if (dataPath.substring(dataPath.lastIndexOf(File.separator), dataPath.length()).equals(File.separator)) {
			this.uiController.alert(InternationalisationUtils.getI18nString(MESSAGE_NO_FILENAME));
		} else {
			log.debug("Filename is [" + dataPath + "] before [" + CsvExporter.CSV_EXTENSION + "] check.");
			if (!dataPath.endsWith(CsvExporter.CSV_EXTENSION)) {
				dataPath += CsvExporter.CSV_EXTENSION;
			}
			log.debug("Filename is [" + dataPath + "] after [" + CsvExporter.CSV_EXTENSION + "] check.");
			
			File csvFile = new File(dataPath);
			if(csvFile.exists() && csvFile.isFile()) {
				// show confirmation dialog
				this.confirmationDialog = uiController.showConfirmationDialog("doExport('" + dataPath + "')",
						this, MESSAGE_CONFIRM_FILE_OVERWRITE);
			} else {
				doExport(dataPath);
			}
		}
	}
	
	public void doExport(String dataPath) {
		if(this.confirmationDialog != null) {
			uiController.remove(this.confirmationDialog);
			this.confirmationDialog = null;
		}
		
		try {
			if (this.attachedObject != null) {
				log.debug("Exporting selected objects...");
				doSpecialExport(dataPath,
						getSelected(this.exportClass, ((Object[])this.attachedObject)));
			} else doSpecialExport(dataPath);
			uiController.removeDialog(wizardDialog);
		} catch(IOException ex) {
			log.debug(InternationalisationUtils.getI18nString(MESSAGE_EXPORT_TASK_FAILED), ex);
			uiController.alert(InternationalisationUtils.getI18nString(MESSAGE_EXPORT_TASK_FAILED) + ": " + ex.getMessage());
		} finally {
			log.trace("EXIT");
		}
	}
	
	public final void columnCheckboxChanged() {
		// FIXME If possible, remove this empty method!
	}

	protected abstract void doSpecialExport(String dataPath) throws IOException;
	protected abstract void doSpecialExport(String dataPath, List<T> selected) throws IOException;
	
	/**
	 * Gets the objects attached to the selected Thinlet components.
	 * @param <T> Class of the selected objects
	 * @param selectedClass Class of the selected Objects
	 * @param selected Array of selected Thinlet components
	 * @return List of the attached objects from the selected components
	 */
	@SuppressWarnings("unchecked")
	private List<T> getSelected(Class<T> selectedClass, Object[] selected) {
		List<T> objects = new LinkedList<T>();
		for (Object o : selected) {
			objects.add((T) this.uiController.getAttachedObject(o));
		}
		return objects;
	}
}
