/**
 * 
 */
package net.frontlinesms.ui.handler;

import static net.frontlinesms.ui.UiGeneratorControllerConstants.COMPONENT_EVENTS_LIST;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.frontlinesms.AppProperties;
import net.frontlinesms.events.AppPropertiesEventNotification;
import net.frontlinesms.events.EventBus;
import net.frontlinesms.events.EventObserver;
import net.frontlinesms.events.FrontlineEventNotification;
import net.frontlinesms.ui.HomeTabEventNotification;
import net.frontlinesms.ui.FrontlineUI;
import net.frontlinesms.ui.FrontlineUiUtils;
import net.frontlinesms.ui.UiDestroyEvent;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.UiGeneratorControllerConstants;
import net.frontlinesms.ui.UiProperties;
import net.frontlinesms.ui.events.FrontlineUiUpateJob;
import net.frontlinesms.ui.handler.message.MessagePanelHandler;
import net.frontlinesms.ui.i18n.FileLanguageBundle;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.settings.HomeTabLogoChangedNotification;

/**
 * Event handler for the Home tab and associated dialogs
 * @author Alex Anderson <alex@frontlinesms.com>
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public class HomeTabHandler extends BaseTabHandler implements EventObserver {
//> STATIC CONSTANTS
	/** Limit of the number of events to be displayed on the home screen */
	final int EVENTS_LIMIT = UiProperties.getInstance().getHomeTabEventLimit();
	
	/** UI XML File Path: the Home Tab itself */
	protected static final String UI_FILE_HOME_TAB = "/ui/core/home/homeTab.xml";
	/** UI XML File Path: settings dialog for the home tab logo */
	private static final String UI_FILE_HOME_TAB_SETTINGS = "/ui/core/home/dgHomeTabSettings.xml";
	/** UI XML File Path: statistics dialog */
	private static final String UI_FILE_STATS_DIALOG = "/ui/core/home/dgStatistics.xml";
	/** Thinlet Component Name: Home Tab: logo */
	private static final String COMPONENT_LB_HOME_TAB_LOGO = "lbHomeTabLogo";
	/** Thinlet Component Name: Settings dialog: radio indicating if the logo is visible */
	private static final String COMPONENT_CB_HOME_TAB_LOGO_INVISIBLE = "cbHomeTabLogoInvisible";
	/** Thinlet Component Name: Settings dialog: radio used to choose the default logo */
	private static final String COMPONENT_CB_HOME_TAB_USE_DEFAULT_LOGO = "cbHomeTabLogoDefault";
	/** Thinlet Component Name: Settings dialog: radio used to choose a custom logo */
	private static final String COMPONENT_CB_HOME_TAB_USE_CUSTOM_LOGO = "cbHomeTabLogoCustom";
	/** Thinlet Component Name: Settings dialog: checkbox used to choose a custom logo */
	private static final String COMPONENT_CB_HOME_TAB_LOGO_KEEP_ORIGINAL_SIZE = "cbHomeTabLogoKeepOriginalSize";
	/** Thinlet Component Name: Settings dialog: panel grouping the path of the image file for the logo */
	private static final String COMPONENT_PN_CUSTOM_IMAGE = "pnCustomImage";
	/** Thinlet Component Name: Settings dialog: textfield inidicating the path of the image file for the logo */
	private static final String COMPONENT_TF_IMAGE_SOURCE = "tfImageSource";

	/** Default FrontlineSMS home logo */
	private static final String FRONTLINE_LOGO = "/icons/frontlineSMS_logo.png";
	/** Max FrontlineSMS home logo width */
	private static final double FRONTLINE_LOGO_MAX_WIDTH = 484.0;
	/** Max FrontlineSMS home logo height */
	private static final double FRONTLINE_LOGO_MAX_HEIGHT = 300.0;

	private EventBus eventBus;

	private MessagePanelHandler messagePanel;


//> INSTANCE PROPERTIES

//> CONSTRUCTORS
	/**
	 * Create a new instance of this class.
	 * @param ui value for {@link #ui}
	 */
	public HomeTabHandler(UiGeneratorController ui) {
		super(ui);
		this.eventBus = ui.getFrontlineController().getEventBus();
		
		this.eventBus.registerObserver(this);
	}

//> UI METHODS
	/** Show the settings dialog for the home tab. */
	public void showHomeTabSettings() {
		log.trace("ENTER");
		Object homeTabSettings = ui.loadComponentFromFile(UI_FILE_HOME_TAB_SETTINGS, this);
		UiProperties uiProperties = UiProperties.getInstance();
		boolean visible 			= uiProperties.isHometabLogoVisible();
		boolean isCustomLogo 		= uiProperties.isHometabCustomLogo();
		boolean isOriginalSizeKept 	= uiProperties.isHometabLogoOriginalSizeKept();
		
		String imageLocation = uiProperties.getHometabLogoPath();
		log.debug("Visible? " + visible);
		log.debug("Logo: " + (isCustomLogo ? "custom" : "default"));
		if (isCustomLogo)
			log.debug("Keep original size: " + isOriginalSizeKept);
		log.debug("Image location [" + imageLocation + "]");
		
		String radioButtonName;
		if(!visible) {
			radioButtonName = COMPONENT_CB_HOME_TAB_LOGO_INVISIBLE;
		} else if(isCustomLogo) {
			radioButtonName = COMPONENT_CB_HOME_TAB_USE_CUSTOM_LOGO;
		} else {
			radioButtonName = COMPONENT_CB_HOME_TAB_USE_DEFAULT_LOGO;
		}
		
		ui.setSelected(ui.find(homeTabSettings, radioButtonName), true);
		ui.setSelected(ui.find(homeTabSettings, COMPONENT_CB_HOME_TAB_LOGO_KEEP_ORIGINAL_SIZE), isOriginalSizeKept);
		
		setHomeTabCustomLogo(ui.find(homeTabSettings, COMPONENT_PN_CUSTOM_IMAGE), isCustomLogo && visible);
		
		if (imageLocation != null && imageLocation.length() > 0) {
			ui.setText(ui.find(homeTabSettings, COMPONENT_TF_IMAGE_SOURCE), imageLocation);
		}
		
		ui.add(homeTabSettings);
		log.trace("EXIT");
	}
	
	/** Show the settings dialog for the home tab. */
	public void showStatsDialog() {
		log.trace("ENTER");
		Object homeTabSettings = ui.loadComponentFromFile(UI_FILE_STATS_DIALOG, this);
		ui.add(homeTabSettings);
		log.trace("EXIT");
	}
	
	/**
	 * Save the home tab settings from the settings dialog, and remove the dialog.
	 * @param panel
	 */
	public void saveHomeTabSettings(Object panel) {
		log.trace("ENTER");
		boolean invisible 			= ui.isSelected(ui.find(panel, COMPONENT_CB_HOME_TAB_LOGO_INVISIBLE));
		boolean isCustomLogo 		= ui.isSelected(ui.find(panel, COMPONENT_CB_HOME_TAB_USE_CUSTOM_LOGO));
		boolean isOriginalSizeKept 	= ui.isSelected(ui.find(panel, COMPONENT_CB_HOME_TAB_LOGO_KEEP_ORIGINAL_SIZE));
		
		String imgSource = ui.getText(ui.find(panel, COMPONENT_TF_IMAGE_SOURCE));
		log.debug("Hidden? " + invisible);
		log.debug("Logo: " + (isCustomLogo ? "default" : "custom"));
		log.debug("Image location [" + imgSource + "]");
		UiProperties uiProperties = UiProperties.getInstance();
		uiProperties.setHometabLogoVisible(!invisible);
		uiProperties.setHometabCustomLogo(isCustomLogo);
		uiProperties.setHometabLogoOriginalSizeKept(isOriginalSizeKept);
		uiProperties.setHometabLogoPath(imgSource);
		uiProperties.saveToDisk();

		// Update visibility of logo
		refreshLogoVisibility(getTab());
		
		ui.remove(panel);
		log.trace("EXIT");
	}
	
	/**
	 * Enable or disable the bottom panel whether the logo is custom or not.
	 * @param panel
	 * @param isCustom <code>true</code> if the logo is a custom logo; <code>false</code> otherwise.
	 */
	public void setHomeTabCustomLogo(Object panel, boolean isCustom) {
		ui.setEnabled(panel, isCustom);
		for (Object obj : ui.getItems(panel)) {
			ui.setEnabled(obj, isCustom);
		}
	}

	
//> UI PASSTHRU METHODS TO UiGC
	/**
	 * @param component Component whose contents are to be removed
	 * @see UiGeneratorController#removeAll()
	 */
	public void removeAll(Object component) {
		this.ui.removeAll(component);
	}
	
	/**
	 * @param component
	 * @see UiGeneratorController#showOpenModeFileChooser(Object)
	 */
	public void showFileChooser(Object component) {
		this.ui.showFileChooser(component);
	}
	
//> INSTANCE HELPER METHODS	
	/**
	 * Refresh the contents of the tab.
	 */
	protected Object initialiseTab() {
		Object tabComponent = ui.loadComponentFromFile(UI_FILE_HOME_TAB, this);
		
		Object pnSend = ui.find(tabComponent, UiGeneratorControllerConstants.COMPONENT_PN_SEND);
		
		final boolean shouldDisplayRecipientField = true;
		final boolean shouldCheckMaxMessageLength = false;
		final int numberOfRecipients = 1;
		
		messagePanel = MessagePanelHandler.create(this.ui, shouldDisplayRecipientField, shouldCheckMaxMessageLength, numberOfRecipients);
		ui.add(pnSend, messagePanel.getPanel());
		
		refreshLogoVisibility(tabComponent);
		
		Object fastLanguageSwitch = ui.find(tabComponent, "fastLanguageSwitch");
		for (FileLanguageBundle languageBundle : InternationalisationUtils.getLanguageBundles()) {
			// Don't show the flag for the current language
			if(languageBundle.equals(FrontlineUI.currentResourceBundle)) continue;
			
			Object button = ui.createLink("", "changeLanguage(this)", tabComponent);
			ui.setIcon(button, ui.getFlagIcon(languageBundle));
			ui.setString(button, "tooltip", languageBundle.getLanguageName());
			ui.setWeight(button, 1, 0);
			ui.setAttachedObject(button, languageBundle.getFile().getAbsolutePath());
			ui.add(fastLanguageSwitch, button);
		}
		
		return tabComponent;
	}
	
	public void refresh() { /* No refresh required */ }
	
	/**
	 * Update the visibility of the logo.
	 * @param tabComponent The tab component.  This is passed in, as this method can be called form {@link #initialiseTab()}, in which case {@link #getTab()} will return null.
	 */
	private void refreshLogoVisibility(Object tabComponent) {
		Object lbLogo = ui.find(tabComponent, COMPONENT_LB_HOME_TAB_LOGO);
		if (!UiProperties.getInstance().isHometabLogoVisible()) {
			Image noIcon = null;
			ui.setIcon(lbLogo, noIcon);
		} else {
			String imageLocation = UiProperties.getInstance().getHometabLogoPath();
			boolean useDefault = true;
			if (UiProperties.getInstance().isHometabCustomLogo() && imageLocation != null && imageLocation.length() > 0) {
				// Absolute or relative path provided
				try {
					BufferedImage homeTabLogoImage = ImageIO.read(new File(imageLocation));
					
					// If the "Keep original size" box is unchecked, we resize the image
					if (!UiProperties.getInstance().isHometabLogoOriginalSizeKept())
					{
						ui.setIcon(lbLogo, 
								FrontlineUiUtils.getLimitedSizeImage(homeTabLogoImage,
										FRONTLINE_LOGO_MAX_WIDTH, FRONTLINE_LOGO_MAX_HEIGHT));
					}
					else
						ui.setIcon(lbLogo, homeTabLogoImage);

					
					useDefault = false;
				} catch (IOException e) {
					// We are unable to find the specified image, using the default
					log.warn("We are unable to find the specified image [" + imageLocation + "], using the default one.", e);
				}
			}
			if (useDefault) {
				// We go for the default one, inside the package
				ui.setIcon(lbLogo, ui.getIcon(FRONTLINE_LOGO));
			}
		}
	}

//> UI HELPER METHODS
	private Object getRow(HomeTabEventNotification newEvent) {
		Object row = ui.createTableRow(newEvent);
		Object cell = ui.createTableCell("");
		ui.setIcon(cell, newEvent.getIcon());
		ui.add(row, cell);
		ui.add(row, ui.createTableCell(newEvent.getDescription()));
		ui.add(row, ui.createTableCell(InternationalisationUtils.getDatetimeFormat().format(newEvent.getTime())));
		return row;
	}

//> LISTENER EVENT METHODS
	public void notify(final FrontlineEventNotification notification) {
		if (notification instanceof HomeTabLogoChangedNotification) {
			this.refreshLogoVisibility(getTab());
		} else if (notification instanceof AppPropertiesEventNotification) {
			String property = ((AppPropertiesEventNotification) notification).getProperty();
			if (property.equals(AppProperties.KEY_SMS_COST_SENT_MESSAGES)
					|| property.equals(UiProperties.CURRENCY_FORMAT)
					|| property.equals(UiProperties.CURRENCY_FORMAT_IS_CUSTOM)) {
				this.messagePanel.updateCost();
			}
		} else if (notification instanceof HomeTabEventNotification) {
			new FrontlineUiUpateJob() {
				public void run() {
					Object eventListComponent = find(COMPONENT_EVENTS_LIST);
					if(eventListComponent != null) {
						while(ui.getItems(eventListComponent).length >= EVENTS_LIMIT) {
							ui.remove(ui.getItem(eventListComponent, EVENTS_LIMIT-1));
						}
						ui.add(eventListComponent, getRow((HomeTabEventNotification) notification), 0);
					}		
				}
			}.execute();
		} else if (notification instanceof UiDestroyEvent) {
			if(((UiDestroyEvent) notification).isFor(this.ui)) {
				this.ui.getFrontlineController().getEventBus().unregisterObserver(this);
			}
		}
	}

//> STATIC FACTORIES

//> STATIC HELPER METHODS
}
