package net.frontlinesms.ui.handler.phones.debug.stk;

import net.frontlinesms.ui.UiGeneratorController;

import org.smslib.handler.ATHandler;
import org.smslib.stk.StkConfirmationPrompt;

public class StkConfirmationDialog extends StkDialog {
	private final StkConfirmationPrompt prompt;

	public StkConfirmationDialog(ATHandler atHandler, UiGeneratorController ui, StkConfirmationPrompt prompt) {
		super(atHandler, ui, "Confirm");
		this.prompt = prompt;
		
		ui.add(dialog, ui.createLabel(prompt.getText()));
		ui.add(dialog, ui.createButton("OK", "confirm", dialog, this));
		ui.add(dialog, ui.createButton("Cancel", "remove", dialog, this));
	}
	
	public void confirm() throws Exception {
		showNewDialog(atHandler.stkRequest(prompt.getRequest()));
	}
}
