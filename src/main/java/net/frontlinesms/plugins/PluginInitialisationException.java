package net.frontlinesms.plugins;

/**
 * Exception thrown when initialising a FrontlineSMS plugin.
 * @author Alex
 */
@SuppressWarnings("serial")
public class PluginInitialisationException extends Exception {
	/**
	 * Create a new {@link PluginInitialisationException} with a particular cause.
	 * @param cause
	 */
	public PluginInitialisationException(Throwable cause) {
		super(cause);
	}
}
