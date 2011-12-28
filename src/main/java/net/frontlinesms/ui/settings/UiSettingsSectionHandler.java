package net.frontlinesms.ui.settings;

import java.util.List;

import net.frontlinesms.settings.FrontlineValidationMessage;

public interface UiSettingsSectionHandler {
	  /** 
	   * @param section 
	   * @return The Thinlet panel for this section 
	   **/
	  public Object getPanel();
	  
	  /** Prepare this handler for discarding. */
	  public void deinit();
	
	  /** Called for each {@link UiSettingsSectionHandler} when the settings are saved */
	  public void save();
	  
	  /**
	   * @return <code>null</code> if every field in the current panel has been validated,
	   * otherwise an internationalized validation message.
	   */
	  public List<FrontlineValidationMessage> validateFields();

	  /** @return The title of the section */
	  public String getTitle();
	  
	  public Object getSectionNode();
}
