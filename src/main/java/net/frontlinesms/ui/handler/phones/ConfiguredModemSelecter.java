package net.frontlinesms.ui.handler.phones;

import thinlet.Thinlet;
import net.frontlinesms.data.domain.SmsModemSettings;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;

public class ConfiguredModemSelecter implements ThinletUiEventHandler {
	private final UiGeneratorController ui;
	private final Object dialog;
	/** textfield to update on completion */
	private final Object updateTextfield;
	
	public ConfiguredModemSelecter(UiGeneratorController ui, Object textfield) {
		this.ui = ui;
		updateTextfield = textfield;
		
		dialog = ui.createDialog("Select connection"); // FIXME i18n
		ui.setWidth(dialog, 320);
		ui.setHeight(dialog, 240);
		ui.setColumns(dialog, 2);
		ui.setCloseAction(dialog, "close", dialog, this);
		
		Object list = Thinlet.create("list");
		ui.setName(list, "lsConnections");
		ui.setPerform(list, "done", dialog, this);
		ui.setColspan(list, 2);
		ui.setWeight(list, 1, 1);
		
		for(SmsModemSettings s : ui.getFrontlineController().getSmsModemSettingsDao().getAll()) {
			ui.add(list, createListItem(s));
		}
		add(list);

		add(ui.createButton("OK", "done", dialog, this));
		add(ui.createButton("Cancel", "close", dialog, this));
	}

	/** Show the selecter */
	public void show() {
		ui.add(dialog);
	}
	
	public void close() {
		ui.remove(dialog);
	}
	
	public void done() {
		Object selected = ui.getSelectedItem(ui.find(dialog, "lsConnections"));
		if(selected != null) {
			SmsModemSettings settings = (SmsModemSettings) ui.getAttachedObject(selected);
			ui.setText(updateTextfield, settings.getSerial());
			close();
		}
	}
	
	private void add(Object component) {
		ui.add(dialog, component);
	}

	private Object createListItem(SmsModemSettings s) {
		return ui.createListItem(getDescription(s), s);
	}

	private String getDescription(SmsModemSettings s) {
		return s.getManufacturer() + " " + s.getModel();
	}
}
