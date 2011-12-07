package net.frontlinesms.ui.handler.phones.debug;

import org.smslib.handler.ATHandler;
import org.smslib.stk.StkResponse;
import org.smslib.stk.StkValuePrompt;

import net.frontlinesms.ui.UiGeneratorController;

public class StkValuePromptDialog extends StkDialog {
	private static final String TF_PROMPTED_VALUE = "tfPromptedValue";
	
	private final StkValuePrompt prompt;

	public StkValuePromptDialog(ATHandler atHandler, UiGeneratorController ui, StkValuePrompt prompt) {
		super(atHandler, ui, "Enter value: " + prompt.getPromptText());
		this.prompt = prompt;

		ui.add(dialog, ui.createLabel(prompt.getPromptText()));
		ui.add(dialog, ui.createTextfield(TF_PROMPTED_VALUE, ""));
		ui.add(dialog, ui.createButton("Submit", "submit", dialog, this));
	}
	
//> UI EVENT METHODS
	public void submit() throws Exception {
		StkResponse response = atHandler.stkRequest(prompt.getRequest(), ui.getText(find(TF_PROMPTED_VALUE)));
		showNewDialog(response);
	}
}
