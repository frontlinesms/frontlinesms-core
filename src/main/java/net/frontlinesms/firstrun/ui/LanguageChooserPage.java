package net.frontlinesms.firstrun.ui;

import java.awt.Font;
import java.io.File;

import net.frontlinesms.AppProperties;
import net.frontlinesms.ui.FrontlineUI;
import net.frontlinesms.ui.i18n.FileLanguageBundle;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.i18n.LanguageBundle;

public class LanguageChooserPage extends FirstTimeWizardPage {

//> STATIC CONSTANTS
	/** The default country to use - Great Britain */
	private static final String COUNTRY_GB = "gb";

//> UI COMPONENT NAMES
	private static final String COMPONENT_LANGUAGES_LIST = "languagesList";

//> CONSTRUCTORS/INITIALISERS
	public LanguageChooserPage(FirstTimeWizard ui) {
		super(ui);
	}

	@Override
	public void initContent() {
		Object languagesList = find(COMPONENT_LANGUAGES_LIST);
		for (FileLanguageBundle languageBundle : InternationalisationUtils.getLanguageBundles()) {
			Object item = ui.createListItem(languageBundle.getLanguageName(), languageBundle.getFile().getAbsolutePath());
			ui.setIcon(item, ui.getFlagIcon(languageBundle));
			int index = -1;
			if (languageBundle.getCountry().equals(COUNTRY_GB)) {
				ui.setSelected(item, true);
				index = 0;
			}
			ui.add(languagesList, item, index);
		}
	}

//> ACCESSORS
	@Override
	public String getLayoutFile() {
		return "/ui/wizard/languageSelect.xml";
	}
	
//> UI EVENT METHODS
	/**
	 * Save the selected language
	 * @param list
	 */
	public void setLanguage(Object list) {
		Object sel = ui.getSelectedItem(list);
		AppProperties appProperties = AppProperties.getInstance();
		String filename = ui.getAttachedObject(sel).toString();
		appProperties.setLanguageFilename(filename);
		LanguageBundle languageBundle = InternationalisationUtils.getLanguageBundle(new File(filename));
		FrontlineUI.currentResourceBundle = languageBundle;
		ui.setResourceBundle(languageBundle.getProperties(), languageBundle.isRightToLeft());
		Font font = languageBundle.getFont();
		if(font != null) {
			ui.setFont(new Font(font.getName(), ui.getFont().getStyle(), ui.getFont().getSize()));
		}
		appProperties.saveToDisk();
		ui.setTitle(InternationalisationUtils.getI18nString(FirstTimeWizard.I18N_FIRST_TIME_WIZARD_TITLE));
		ui.gotoNextPage();
	}
}
