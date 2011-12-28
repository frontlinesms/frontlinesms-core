package net.frontlinesms.ui.handler.phones.debug.ussd;

import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;

import org.smslib.handler.ATHandler;
import org.smslib.ussd.*;

public abstract class UssdDialog implements ThinletUiEventHandler {
	protected final String baseRequest;
	private final ATHandler atHandler;
	protected final UiGeneratorController ui;
	protected final Object dialog;
	
	public UssdDialog(String baseRequest, ATHandler atHandler, UiGeneratorController ui, String dialogTitle) {
		this.baseRequest = baseRequest;
		this.atHandler = atHandler;
		this.ui = ui;
		
		this.dialog = ui.createDialog(dialogTitle);
		ui.setColumns(dialog, 1);
		ui.setWidth(dialog, 320);
		ui.setMethod(dialog, "close", "remove", dialog, this);
	}
	
	void add(Object component) {
		ui.add(dialog, component);
	}
	
	void addButton(String text, String action) {
		add(ui.createButton(text, action, dialog, this));
	}
	
	void addText(String text) {
		for(String s : text.split("[\r\n]")) {
			add(ui.createLabel(s));
		}
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
	
	private void processResponse(String request, UssdResponse response) {
		if(response instanceof ActionableUssdResponse) {
			new ActionableUssdDialog(request, atHandler, ui, (ActionableUssdResponse) response).show();
		} else if(response instanceof UssdNotification) {
			ui.alert(((UssdNotification) response).getText());
		} else if(response instanceof UssdOperationNotSupportedResponse) {
			ui.alert("USSD operation not supported.");
		} else {
			throw new RuntimeException("No handling implemented for " + response.getClass());
		}
		remove();
	}
	
	String ensureFrame(String r) {
		if(!r.startsWith("*")) r = "*" + r;
		if(!r.endsWith("#")) r += "#";
		return r;
	}
	
	String removeFrame(String r) {
		if(r.startsWith("*")) r = r.substring(1);
		if(r.endsWith("#")) r = r.substring(0, r.length() - 1);
		return r;
	}
	
	void doRequest(String... req) throws Exception {
		assert(req.length > 0);
//		String fullRequest = removeFrame(req[0]);
		String fullRequest = req[0];
		for(int i=1; i<req.length; ++i) fullRequest += "*" + req[i];
		System.out.println("Full request: " + fullRequest);
//		UssdResponse response = atHandler.ussdRequest(ensureFrame(fullRequest));
		UssdResponse response = atHandler.ussdRequest(fullRequest);
		processResponse(fullRequest, response);
	}
}
