package net.frontlinesms.ui.handler.phones.debug.ussd;

import net.frontlinesms.ui.UiGeneratorController;

import org.smslib.handler.ATHandler;

public class UssdInitialRequestDialog extends UssdDialog {
	public UssdInitialRequestDialog(ATHandler atHandler, UiGeneratorController ui) {
		super(null, atHandler, ui, "New USSD request");
		
		Object panel = ui.createPanel("");
		ui.add(panel, ui.createLabel("*"));
		ui.add(panel, ui.createTextfield("tfWhatever", "544"));
		ui.add(panel, ui.createLabel("#"));
		add(panel);
		addButton("SEND", "send");
	}
	
	public void send() throws Exception {
		String toSend = ui.getText(find("tfWhatever"));
//		toSend = ensureFrame(toSend);
		System.out.println("TO SEND: " + toSend);
		doRequest(toSend);
	}
}
