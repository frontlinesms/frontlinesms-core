package net.frontlinesms.ui.handler.settings;

import java.util.List;

import net.frontlinesms.settings.BaseSectionHandler;
import net.frontlinesms.settings.FrontlineValidationMessage;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.settings.UiSettingsSectionHandler;

public class SettingsEmptySectionHandler extends BaseSectionHandler implements UiSettingsSectionHandler, ThinletUiEventHandler {
	private static final String UI_EMPTY_SECTION = "/ui/core/settings/generic/pnEmptySettings.xml";
	
	private String sectionTitle;
	
	public SettingsEmptySectionHandler (UiGeneratorController ui, String sectionTitle) {
		super(ui);
		this.sectionTitle = sectionTitle;
	}
	
	protected void init() {
		this.panel = ui.loadComponentFromFile(UI_EMPTY_SECTION, this);
	}
	
	public void save() {
	}
	
	public List<FrontlineValidationMessage> validateFields() {
		return null;
	}

	public String getTitle() {
		return this.sectionTitle;
	}
}