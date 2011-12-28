package net.frontlinesms.ui.handler.phones.debug.ussd;

import net.frontlinesms.ui.UiGeneratorController;

import org.smslib.handler.ATHandler;

public class UssdInitialRequestDialog extends UssdDialog {
	/** Set to <code>true</code> if initial request should always be wrapped with *...# */
	private static final boolean ENSURE_FRAME = false;
	
	public UssdInitialRequestDialog(ATHandler atHandler, UiGeneratorController ui) {
		super(null, atHandler, ui, "New USSD request");

		if(ENSURE_FRAME) {
			Object panel = ui.createPanel("");
			ui.add(panel, ui.createLabel("*"));
			ui.add(panel, ui.createTextfield("tfWhatever", "544"));
			ui.add(panel, ui.createLabel("#"));
			add(panel);
		} else {
			add(ui.createTextfield("tfWhatever", "544"));
		}
		addButton("SEND", "send");
	}
	
	public void send() throws Exception {
		String toSend = ui.getText(find("tfWhatever"));
		if(ENSURE_FRAME) toSend = ensureFrame(toSend);
		System.out.println("TO SEND: " + toSend);
		doRequest(toSend);
	}
}
