package net.frontlinesms.firstrun.ui;

public class StartApplicationPage extends FirstTimeWizardPage {
	public StartApplicationPage(FirstTimeWizard ui) {
		super(ui);
	}

	@Override
	public String getLayoutFile() {
		return "/ui/wizard/startForm.xml";
	}
	
	@Override
	public void initContent() {}
	
	public void startFrontline() throws Throwable {
		ui.startFrontline();
	}
}
