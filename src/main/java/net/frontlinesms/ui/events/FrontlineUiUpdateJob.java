package net.frontlinesms.ui.events;

import java.awt.EventQueue;

public abstract class FrontlineUiUpdateJob implements Runnable {
	public void execute() {
		EventQueue.invokeLater(this);
	}
}
