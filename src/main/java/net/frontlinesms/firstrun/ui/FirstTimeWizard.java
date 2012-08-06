/*
 * FrontlineSMS <http://www.frontlinesms.com>
 * Copyright 2007, 2008 kiwanja
 * 
 * This file is part of FrontlineSMS.
 * 
 * FrontlineSMS is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * FrontlineSMS is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FrontlineSMS. If not, see <http://www.gnu.org/licenses/>.
 */
package net.frontlinesms.firstrun.ui;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

import net.frontlinesms.AppProperties;
import net.frontlinesms.ui.FrontlineUI;
import net.frontlinesms.ui.Icon;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import net.frontlinesms.ui.i18n.TextResourceKeyOwner;
import thinlet.FrameLauncher;

/**
 * This class is responsible for showing the first time wizard and handling its events.
 * 
 * @author Carlos Eduardo Genz kadu(at)masabi(dot)com
 * @author Alex Anderson
 */
@SuppressWarnings("serial")
@TextResourceKeyOwner(prefix={"MESSAGE_", "I18N"})
public class FirstTimeWizard extends FrontlineUI {	

//> i18N KEYS
	/** [i18n key] The title of the {@link FirstTimeWizard}'s {@link Frame}. */
	public static final String I18N_FIRST_TIME_WIZARD_TITLE = "common.first.time.wizard";
	
//> INSTANCE VARIABLES
	/** Root item in the UI */
	private Object root;
	/** The page from {@link #pages} that we are currently viewing.  This is used for searching. */
	private Object currentPage;
	/** Index in {@link #pages} that the {@link #currentPage} is located at. */
	private int currentPageIndex;
	/** List of pages to be displayed by the wizard.  These pages are in order. */
	private List<FirstTimeWizardPage> pages = new ArrayList<FirstTimeWizardPage>();
	/** Thinlet/AWT {@link FrameLauncher} used for displaying the {@link FirstTimeWizard} user interface. */
	private FrameLauncher frameLauncher;
	/** Callback listener for when the wizard is finished. */
	private FirstTimeWizardListener listener;
	
//> CONSTRUCTORS
	/**
	 * 
	 * @param frontline 
	 */
	public FirstTimeWizard(FirstTimeWizardListener listener) {
		this.listener = listener;
		
		frameLauncher = new FrameLauncher(InternationalisationUtils.getI18nString(I18N_FIRST_TIME_WIZARD_TITLE), this, 524, 380, getIcon(Icon.FRONTLINE_ICON));
		root = createPanel("pnRoot");
		add(root);
		
		pages.add(new LanguageChooserPage(this));
		pages.add(new CountryChooserPage(this));
		
		for(Class<? extends FirstTimeWizardPageProvider> s : new FirstTimeWizardPageProviderLoader().getAll()) {
			try {
				FirstTimeWizardPageProvider newInstance = s.newInstance();
				newInstance.setOwner(this);
				pages.addAll(newInstance.getPages());
			} catch(Exception ex) {
				log.error("Cannot load first time wizard pages for " + s.getClass(), ex);
			}
		}
		
		pages.add(new StartApplicationPage(this));
		
		showPage(0);
	}

//> UI METHODS
	/** Go to the previous page. */
	public void goBack() {
		showPage(currentPageIndex - 1);
	}
	
	/**
	 * Start the FrontlineSMS user interface. 
	 * @throws Throwable if {@link Throwable} was thrown by {@link UiGeneratorController}'s constructor.
	 */
	public void startFrontline() throws Throwable {
		AppProperties appProperties = AppProperties.getInstance();
		appProperties.setShowWizard(false);
		appProperties.saveToDisk();
		
		frameLauncher.dispose();
		listener.handleCompleted();
	}
	
	public void setTitle(String title) {
		this.frameLauncher.setTitle(title);
	}

//> INSTANCE HELPER METHODS
	/** Proceed to the page in {@link #pages} */
	public void gotoNextPage() {
		showPage(this.currentPageIndex + 1);
	}

	/**
	 * Show a particular page.
	 * The page is selected by index from {@link #pages}.
	 * @param newPageIndex the index into {@link #pages} of the next page to show. */
	private void showPage(int newPageIndex) {
		remove(currentPage);
		currentPageIndex = newPageIndex;
		currentPage = pages.get(currentPageIndex).getPage();
		add(root, currentPage);
	}
}
