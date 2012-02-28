package net.frontlinesms.firstrun.ui;

import java.util.Comparator;

import net.frontlinesms.resources.ImplementationLoader;

public class FirstTimeWizardPageProviderLoader extends ImplementationLoader<FirstTimeWizardPageProvider> {
	@Override
	protected Class<FirstTimeWizardPageProvider> getEntityClass() {
		return FirstTimeWizardPageProvider.class;
	}

	@Override
	protected Comparator<Class<? extends FirstTimeWizardPageProvider>> getSorter() {
		return new Comparator<Class<? extends FirstTimeWizardPageProvider>>() {
			public int compare(Class<? extends FirstTimeWizardPageProvider> o1,
					Class<? extends FirstTimeWizardPageProvider> o2) {
				return 0;
			}
		};
	}

}
