/**
 * 
 */
package net.frontlinesms.ui.handler;

import org.apache.log4j.Logger;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.ui.FrontlineUI;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;

/**
 * Base event handler for a UI dialog component.
 * @author aga
 */
public abstract class BasePanelHandler implements ThinletUiEventHandler {
	/** Logging object */
	protected final Logger log = FrontlineUtils.getLogger(this.getClass());
	/** The {@link UiGeneratorController} that shows the tab. */
	protected final FrontlineUI ui;
	
	/** The Thinlet UI component which methods should be invoked upon. */
	private Object panelComponent;

//> INITIALISATION METHODS
	/**
	 * Create a new base dialog event handler tied to a specific {@link UiGeneratorController} instance.
	 * @param ui
	 */
	protected BasePanelHandler(FrontlineUI ui) {
		this.ui = ui;
	}

	/**
	 * Load the dialog from the specified file, and sets the value of {@link #panelComponent}.
	 * @param uiLayoutFilePath The classpath path of the layout file to use.
	 */
	protected void loadPanel(String uiLayoutFilePath) {
		// Create a new dialog, add the desired panel and add a close() method to remove the dialog
		this.panelComponent = ui.loadComponentFromFile(uiLayoutFilePath, this);
	}
	
//> ACCESSORS
	/** @return {@link #panelComponent} */
	public Object getPanelComponent() {
		return this.panelComponent;
	}
	
//> UI HELPER METHODS
	/**
	 * Find a ui component within the dialog.
	 * @param componentName
	 * @return
	 */
	protected Object find(String componentName) {
		return ui.find(this.panelComponent, componentName);
	}
}
