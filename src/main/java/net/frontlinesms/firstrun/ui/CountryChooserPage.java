package net.frontlinesms.firstrun.ui;

import net.frontlinesms.AppProperties;
import net.frontlinesms.ui.i18n.Country;

public class CountryChooserPage extends FirstTimeWizardPage {
	public CountryChooserPage(FirstTimeWizard ui) {
		super(ui);
	}

	@Override
	public void initContent() {
		Object countryList = find("lsCountry");
		Object currentCountry = AppProperties.getInstance().getUserCountry();

		int selectedIndex = -1;
		for (int i = 0 ; i < Country.values().length ; ++i) {
			Country enumCountry = Country.values()[i];
			
			Object listItem = ui.createListItem(enumCountry.getEnglishName(),
					enumCountry.getCode().toUpperCase());
			ui.setIcon(listItem, this.ui.getFlagIcon(enumCountry.getCode()));
			
			this.ui.add(countryList, listItem);
			
			if (currentCountry.equals(enumCountry.getCode().toUpperCase())) {
				selectedIndex = i;
			}
		}
		
		this.ui.setSelectedIndex(countryList, selectedIndex);
	}

	@Override
	public String getLayoutFile() {
		return "/ui/wizard/countrySelect.xml";
	}

	public void next() {
		Object selectedItem = ui.getSelectedItem(find("lsCountry"));
		if(selectedItem != null) {
			String country = ui.getAttachedObject(selectedItem, String.class);
			AppProperties.getInstance().setUserCountry(country);
		}
		super.next();
	}
}
