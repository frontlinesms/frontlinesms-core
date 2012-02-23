package net.frontlinesms.ui.handler.core;

import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_BT_CONTINUE;
import static net.frontlinesms.ui.UiGeneratorControllerConstants.UI_FILE_CONFIRMATION_DIALOG_FORM;
import net.frontlinesms.ui.FrontlineUI;
import net.frontlinesms.ui.ThinletUiEventHandler;
import thinlet.Thinlet;

import static thinlet.Thinlet.find;

public class ConfirmationDialogHandler implements ThinletUiEventHandler {
	private final FrontlineUI ui;
	private final Object dialog;
	private final String methodToBeCalled;
	private final ThinletUiEventHandler handler;
	
	public ConfirmationDialogHandler(FrontlineUI ui, ThinletUiEventHandler handler, String methodToBeCalled) {
		this.ui = ui;
		dialog = ui.loadComponentFromFile(UI_FILE_CONFIRMATION_DIALOG_FORM, this);
		this.methodToBeCalled = methodToBeCalled;
		this.handler = handler;
		ui.setMethod(find(dialog, COMPONENT_BT_CONTINUE), Thinlet.ATTRIBUTE_ACTION, "doMethodCall", ui.getDesktop(), this);
		ui.add(dialog);
	}
	
	public ConfirmationDialogHandler(FrontlineUI ui, ThinletUiEventHandler handler, String methodToBeCalled, String message) {
		this(ui, handler, methodToBeCalled);
		ui.setText(Thinlet.find(dialog, "lbText"), message);
	}
	
	public void removeDialog(Object dialog) {
		ui.remove(dialog);
	}
	
	public void doMethodCall() {
		Object button = find(dialog, COMPONENT_BT_CONTINUE);
		ui.setMethod(button, Thinlet.ATTRIBUTE_ACTION, methodToBeCalled, ui.getDesktop(), handler);
		ui.invokeAction(button);
		removeDialog(dialog);
	}
}
