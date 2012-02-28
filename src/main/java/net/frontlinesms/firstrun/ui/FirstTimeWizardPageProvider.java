package net.frontlinesms.firstrun.ui;

import java.util.List;

public interface FirstTimeWizardPageProvider {
	List<FirstTimeWizardPage> getPages();

	void setOwner(FirstTimeWizard firstTimeWizard);
}
