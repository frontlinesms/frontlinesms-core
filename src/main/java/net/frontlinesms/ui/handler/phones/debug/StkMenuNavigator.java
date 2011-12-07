package net.frontlinesms.ui.handler.phones.debug;

import java.util.List;

import net.frontlinesms.ui.UiGeneratorController;

import org.smslib.handler.ATHandler;
import org.smslib.stk.StkMenu;
import org.smslib.stk.StkMenuItem;
import org.smslib.stk.StkResponse;
import org.smslib.stk.StkValuePrompt;

public class StkMenuNavigator extends StkDialog {
	private final StkMenu menu;
	
	public StkMenuNavigator(ATHandler atHandler, UiGeneratorController ui, StkMenu menu) {
		super(atHandler, ui, "STK Menu: " + menu.getTitle());
		this.menu = menu;

		if(menu.getItemCount() == 0) {
			ui.add(dialog, ui.createLabel(menu.getTitle()));
			ui.add(dialog, ui.createButton("OK", "remove", dialog, this));
		} else {
			List<StkMenuItem> items = menu.getItems();
			for(int i=0; i<items.size(); ++i) {
				StkMenuItem item = items.get(i);
				ui.add(dialog, ui.createButton(item.getText(), "triggerMenuItem(" + i + ")", dialog, this));
			}
		}
	}
	
//> UI EVENT METHODS
	public void triggerMenuItem(int i) throws Exception {
		StkMenuItem item = menu.getItems().get(i);
		StkResponse response = atHandler.stkRequest(item.getRequest());
		showNewDialog(response);
	}
}
