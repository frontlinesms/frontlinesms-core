package net.frontlinesms.ui.handler.phones.debug.ussd;

import net.frontlinesms.ui.UiGeneratorController;

import org.smslib.handler.ATHandler;
import org.smslib.ussd.ActionableUssdResponse;

public class ActionableUssdDialog extends UssdDialog {
	public ActionableUssdDialog(String request, ATHandler atHandler, UiGeneratorController ui, ActionableUssdResponse response) {
		super(request, atHandler, ui, "USSD");
		
		addText(response.getText());
		add(ui.createTextfield("tfWhatever", ""));
		addButton("Send", "send");
	}
	
	public void send() throws Exception {
		String requestString = ui.getText(find("tfWhatever"));
		doRequest(requestString);
	}
}
