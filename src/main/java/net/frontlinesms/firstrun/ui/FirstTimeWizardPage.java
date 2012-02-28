package net.frontlinesms.firstrun.ui;

import net.frontlinesms.ui.FrontlineUI;
import net.frontlinesms.ui.ThinletUiEventHandler;

public abstract class FirstTimeWizardPage implements ThinletUiEventHandler {
	final FirstTimeWizard ui;
	Object page;
	
//> CONSTRUCTORS
	public FirstTimeWizardPage(FirstTimeWizard ui) {
		this.ui = ui;
	}
	
	public abstract void initContent();
	
//> ACCESSORS
	public abstract String getLayoutFile();
	
	public synchronized Object getPage() {
		if(page == null) {
			page = ui.loadComponentFromFile(getLayoutFile(), this);
			initContent();
		}
		return page;
	}
	
//> UI EVENT METHODS
	public void showHelpPage(String page) {
		ui.showHelpPage(page);
	}
	
//> UI HELPER METHODS
	public Object find(String componentName) {
		return FrontlineUI.find(page, componentName);
	}
}
