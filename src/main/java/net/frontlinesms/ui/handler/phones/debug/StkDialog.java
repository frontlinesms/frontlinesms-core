package net.frontlinesms.ui.handler.phones.debug;

import org.smslib.handler.ATHandler;
import org.smslib.stk.StkMenu;
import org.smslib.stk.StkNotification;
import org.smslib.stk.StkResponse;
import org.smslib.stk.StkValuePrompt;

import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;

public abstract class StkDialog implements ThinletUiEventHandler {
	protected final ATHandler atHandler;
	protected final UiGeneratorController ui;
	protected final Object dialog;
	
	public StkDialog(ATHandler atHandler, UiGeneratorController ui, String dialogTitle) {
		this.atHandler = atHandler;
		this.ui = ui;
		
		this.dialog = ui.createDialog(dialogTitle);
		ui.setColspan(dialog, 1);
		ui.setWidth(dialog, 320);
		ui.setMethod(dialog, "close", "remove", dialog, this);
	}
	
	public void show() {
		ui.add(this.dialog);
	}
	
	public void remove() {
		ui.remove(this.dialog);
	}
	
	Object find(String componentName) {
		return ui.find(dialog, componentName);
	}

	void showNewDialog(StkResponse response) {
		if(response instanceof StkMenu) {
			new StkMenuNavigator(atHandler, ui, (StkMenu) response).show();
		} else if(response instanceof StkValuePrompt) {
			new StkValuePromptDialog(atHandler, ui, (StkValuePrompt) response).show();
		} else if(response instanceof StkNotification) {
			ui.alert(((StkNotification) response).getText());
		} else {
			ui.alert("Don't know how to handle response: " + response);
		}
		remove();
	}
}
