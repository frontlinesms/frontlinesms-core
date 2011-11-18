//package net.frontlinesms.serviceconfig.ui;
//
//import java.util.List;
//
//import net.frontlinesms.data.domain.PersistableSettings;
//import net.frontlinesms.data.repository.ConfigurableServiceSettingsDao;
//import net.frontlinesms.serviceconfig.ConfigurableService;
//import net.frontlinesms.settings.BaseSectionHandler;
//import net.frontlinesms.settings.FrontlineValidationMessage;
//import net.frontlinesms.ui.ThinletUiEventHandler;
//import net.frontlinesms.ui.UiGeneratorController;
//import net.frontlinesms.ui.events.FrontlineUiUpdateJob;
//import net.frontlinesms.ui.settings.UiSettingsSectionHandler;
//
//public abstract class BaseServiceSettingsSectionHandler<T extends ConfigurableService> extends BaseSectionHandler
//		implements UiSettingsSectionHandler, ThinletUiEventHandler {
//	
//	private final ConfigurableServiceSettingsDao<T> dao;
//	
//	public BaseServiceSettingsSectionHandler(UiGeneratorController ui, ConfigurableServiceSettingsDao<T> dao) {
//		super(ui);
//		this.dao = dao;
//	}
//
//	@Override
//	protected final void init() {
//	}
//	
//
//	public final void save() {}
//
//	public final List<FrontlineValidationMessage> validateFields() {
//		return null;
//	}
//}
