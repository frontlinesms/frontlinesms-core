package net.frontlinesms.ui.debug;

import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;

public class SystemPropertyDebugDialog implements ThinletUiEventHandler {
	private final UiGeneratorController ui;
	private final Object dialog;
	
	public SystemPropertyDebugDialog(UiGeneratorController ui) {
		this.ui = ui;
		
		dialog = ui.createDialog("System Property Debugger");
		ui.setWidth(dialog, 640);
		ui.setWidth(dialog, 480);
		ui.setCloseAction(dialog, "remove", dialog, this);
		ui.setColumns(dialog, 1);

		Object tfProperty = ui.createTextfield("tfProperty", "java.library.path");
		ui.setWeight(tfProperty, 1, 0);
		add(tfProperty);
		
		Object tfVal = ui.createTextfield("tfValue", "");
		ui.setWeight(tfVal, 1, 0);
		add(tfVal);
		
		addButton("show value", "showValue");
	}
	
	public void show() {
		ui.add(dialog);
	}
	
	public void remove() {
		ui.remove(dialog);
	}
	
	public void showValue() {
		String p = ui.getText(find("tfProperty"));
		ui.setText(find("tfValue"), System.getProperty(p));
	}
	
	private void add(Object component) {
		ui.add(dialog, component);
	}
	
	private Object find(String componentName) {
		return ui.find(dialog, componentName);
	}
	
	private void addButton(String text, String action) {
		add(ui.createButton(text, action, dialog, this));
	}
}
