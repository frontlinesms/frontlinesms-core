/**
 * 
 */
package net.frontlinesms.ui;

import net.frontlinesms.events.FrontlineEventNotification;

/**
 * Event fired when {@link UiGeneratorController} is being destroyed.  This should
 * allow children (e.g. tab controllers, dialog handlers) to also de-register
 * themselves with the event bus.
 * @author Alex Anderson <alex@frontlinesms.com>
 */
public class UiDestroyEvent implements FrontlineEventNotification {
	private final FrontlineUI ui;
	
	public UiDestroyEvent(FrontlineUI ui) {
		assert ui != null;
		this.ui = ui;
	}
	
	public boolean isFor(FrontlineUI ui) {
		return this.ui == ui; // N.B. this is checking reference, not .equals() deliberately
	}
}
