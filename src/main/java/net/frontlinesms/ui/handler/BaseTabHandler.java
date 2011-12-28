/**
 * 
 */
package net.frontlinesms.ui.handler;

import org.apache.log4j.Logger;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.events.EventBus;
import net.frontlinesms.events.EventObserver;
import net.frontlinesms.events.FrontlineEventNotification;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiDestroyEvent;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.events.FrontlineUiUpdateJob;

/**
 * @author Alex Anderson
 */
public abstract class BaseTabHandler implements ThinletUiEventHandler, EventObserver {
	/** Logging object */
	protected final Logger log = FrontlineUtils.getLogger(this.getClass());
	/** The {@link UiGeneratorController} that shows the tab. */
	protected final UiGeneratorController ui;
	protected final EventBus eventBus;
	private final boolean isEventObserver;
	/** The tab component this handler is based around */
	private Object tabComponent;
	
	/**
	 * @param ui
	 * @param isEventObserver if set <code>true</code>, this will be registered with the {@link EventBus}
	 * 	in the {@link #init()} method, and deregistered in the {@link #notify()} method.  Care should be taken
	 * 	to ensure <code>super.notify(...)</code> is called if overridding the {@link #notify()} method in
	 * 	subclasses.
	 */
	protected BaseTabHandler(UiGeneratorController ui, boolean isEventObserver) {
		this.ui = ui;
		this.eventBus = ui.getFrontlineController().getEventBus();
		this.isEventObserver = isEventObserver;
	}
	
	/** Refresh the view. */
	public abstract void refresh();
	
	protected void threadSafeRefresh() {
		new FrontlineUiUpdateJob() {
			public void run() {
				refresh();
			}
		}.execute();
	}
	
	/** Initialise this component, including its tab. */
	public void init() {
		this.tabComponent = initialiseTab();
		if(isEventObserver) eventBus.registerObserver(this);
	}
	
	/**
	 * <p>Initialise the tab.</p>
	 * <p>This method should only be called by {@link #init()}.</p>
	 * @return the newly-initialised tabComponent.
	 */
	protected abstract Object initialiseTab();
	
	/** @return {@link #tabComponent} */
	public final Object getTab() {
		return this.tabComponent;
	}
	
//> EVENT NOTIFICATION HANDLING
	public void notify(FrontlineEventNotification notification) {
		if (isEventObserver && notification instanceof UiDestroyEvent) {
			if(((UiDestroyEvent) notification).isFor(this.ui)) {
				eventBus.unregisterObserver(this);
			}
		}
	}
	
//> UI HELPER METHODS
	/**
	 * Find a UI component within the {@link #tabComponent}.
	 * @param componentName the name of the UI component
	 * @return the ui component, or <code>null</code> if it could not be found
	 */
	protected final Object find(String componentName) {
		return ui.find(this.tabComponent, componentName);
	}
	
//> UI PASS THROUGH METHODS
	/**
	 * Remove the supplied dialog from view.
	 * @param dialog the dialog to remove
	 * @see UiGeneratorController#removeDialog(Object)
	 */
	public void removeDialog(Object dialog) {
		this.ui.removeDialog(dialog);
	}
	/**
	 * Shows an HTML help page in an external web browser.
	 * @param page The file name of the help page
	 * @see UiGeneratorController#showHelpPage(String)
	 */
	public final void showHelpPage(String page) {
		this.ui.showHelpPage(page);
	}
	/**
	 * Shows a general dialog asking the user to confirm his action. 
	 * @param methodToBeCalled The name and optionally the signature of the method to be called 
	 * @see UiGeneratorController#showConfirmationDialog(String) */
	public final void showConfirmationDialog(String methodToBeCalled) {
		this.ui.showConfirmationDialog(methodToBeCalled, this);
	}
	/**
	 * Shows the message history for the selected contact or group.
	 * @param component group list or contact list
	 */
	public final void showMessageHistory(Object component) {
		this.ui.showMessageHistory(component);
	}
}
