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
package net.frontlinesms.ui.handler.core;

import java.awt.Color;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

import net.frontlinesms.FrontlineSMSConstants;
import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.ui.FrontlineUI;
import net.frontlinesms.ui.i18n.InternationalisationUtils;
import thinlet.Thinlet;

import static thinlet.Thinlet.find;
import static net.frontlinesms.ui.i18n.InternationalisationUtils.parseDate;

/**
 * @author kadu
 */
public class DateSelecter {
	
//> STATIC CONSTANTS
	/** Index of the first month, January */
	private static final int FIRST_MONTH = 0;
	/** Index of the last month, December */
	private static final int LAST_MONTH = 11;
	private static final String COMPONENT_LB_MONTH = "lbMonth";
	private static final String COMPONENT_BT_NEXT = "btNext";
	private static final String COMPONENT_BT_PREVIOUS = "btPrevious";
	private static final String UI_FILE_DATE_SELECTER_FORM = "/ui/core/util/dgDate.xml";

//> INSTANCE PROPERTIES
	/** Logger for this class */
	private final Logger log = FrontlineUtils.getLogger(getClass());
	/** A calendar object with a poorly-defined role */
	private Calendar current;
	/** A month value with a poorly-defined role */
	private int curMonth;
	/** A year value with a poorly-defined role */
	private int curYear;
	/** The {@link Thinlet} ui controller */
	private FrontlineUI ui;
	/** The textfield that the date will be inserted into when date selection has completed. */
	private Object textField;
	private int dayToHighlight;
	private int monthToHighlight;
	private int yearToHighlight;
	
//> CONSTRUCTORS
	/**
	 * Constructs a new date selecter which will update a particular textfield.
	 * @param ui The thinlet ui controller
	 * @param textField The textfield that the selected date will be entered into
	 */
	public DateSelecter(FrontlineUI ui, Object textField) {
		this.ui = ui;
		this.textField = textField;
	}
	
	/**
	 * Shows the date selecter dialog, showing the previous date or today.
	 * @throws IOException
	 */
	public void showSelecter() throws IOException {
		Object dialog = ui.parse(UI_FILE_DATE_SELECTER_FORM);
		init(dialog);
		showMonth(dialog);
		ui.add(dialog);
	}
	
	/**
	 * Initialises the dialog buttons, set their action method and gets the dte to be shown.
	 * 
	 * @param dialog
	 */
	private void init(Object dialog) {
		log.trace("ENTER");
		Object prev = find(dialog, COMPONENT_BT_PREVIOUS);
		ui.setCloseAction(dialog, "closeDialog(this)", dialog, this);
		ui.setAction(prev, "previousMonth(dateSelecter)", dialog, this);
		Object next = find(dialog, COMPONENT_BT_NEXT);
		ui.setAction(next, "nextMonth(dateSelecter)", dialog, this);
		current = Calendar.getInstance();
		setDayToHighlight();
		current.set(Calendar.DATE, 1);
		if (!ui.getText(textField).equals("")) {
			log.debug("Previous date is [" + ui.getText(textField) + "]");
			try {
				Date d = parseDate(ui.getText(textField));
				current.setTime(d);
				setDayToHighlight();
			} catch (ParseException e) {}
		}
		log.debug("Current date is [" + current.getTime() + "]");
		curMonth = current.get(Calendar.MONTH);
		curYear = current.get(Calendar.YEAR);
		log.trace("EXIT");
	}

//> UI EVENT METHODS
	/**
	 * @param dialog
	 */
	public void closeDialog(Object dialog) {
		ui.remove(dialog);
	}

	/**
	 * Method called when the user clicks the next month button on the dialog.
	 * 
	 * @param dialog
	 */
	public void nextMonth(Object dialog) {
		current.set(Calendar.MONTH, this.curMonth + 1);
		curMonth = current.get(Calendar.MONTH);
		if (curMonth == FIRST_MONTH) {
			current.set(Calendar.YEAR, this.curYear + 1);
		}
		curYear = current.get(Calendar.YEAR);
		showMonth(dialog);
	}
	
	/**
	 * Method called when the user clicks the previous month button on the dialog.
	 * 
	 * @param dialog
	 */
	public void previousMonth(Object dialog) {
		current.set(Calendar.MONTH, this.curMonth - 1);
		curMonth = current.get(Calendar.MONTH);
		if (curMonth == LAST_MONTH) {
			current.set(Calendar.YEAR, this.curYear - 1);
		}
		curYear = current.get(Calendar.YEAR);
		showMonth(dialog);
	}
	
	/**
	 * Fill all the dates in the dialog for the current month.
	 * 
	 * @param dialog
	 */
	private void showMonth(Object dialog) {
		log.trace("ENTER");
		current.set(Calendar.DATE, 1);
		String curMonth = getMonthAsString(this.curMonth) + " " + curYear;
		Object lbMonth = find(dialog, COMPONENT_LB_MONTH);
		log.debug("Current month [" + curMonth + "]");
		ui.setText(lbMonth, curMonth);
		for (int i = 1; i <= 6; i++) {
			fillRow(dialog, "pn" + i);
		}
		log.trace("EXIT");
	}

	public void selectionMade(Object dialog, String day) {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.DATE, Integer.parseInt(day));
		c.set(Calendar.MONTH, this.curMonth);
		c.set(Calendar.YEAR, this.curYear);
		String date = InternationalisationUtils.formatDate(c.getTime());
		ui.setText(textField, date);
		ui.remove(dialog);
		ui.invokeAction(this.textField);
	}
	
//> UI HELPER METHODS
	/** Sets the day to be highlighted */
	private void setDayToHighlight() {
		dayToHighlight = current.get(Calendar.DATE);
		monthToHighlight = current.get(Calendar.MONTH);
		yearToHighlight = current.get(Calendar.YEAR);
	}
	/**
	 * Sets the button texts for the supplied week.
	 * 
	 * @param dialog
	 * @param pnName
	 */
	private void fillRow(Object dialog, String pnName) {
		Object panel = find(dialog, pnName);
		Object buttons[] = ui.getItems(panel);
		cleanButtons(buttons);
		
		int dayOfWeek;
		while (current.get(Calendar.MONTH) == this.curMonth) {
			dayOfWeek = current.get(Calendar.DAY_OF_WEEK);
			Object button = buttons[dayOfWeek - 1];
			ui.setEnabled(button, true);
			ui.setText(button, String.valueOf(current.get(Calendar.DATE)));
			ui.setAction(button, "selectionMade(dateSelecter, this.text)", dialog, this);
			if (isDayToHighlight()) {
				ui.setColor(button, Thinlet.FOREGROUND, Color.RED);
			}
			current.set(Calendar.DATE, current.get(Calendar.DATE) + 1);
			if (dayOfWeek == Calendar.SATURDAY) {
				//we've reached the end of the week, so we break;
				break;
			}
		}
		if (current.get(Calendar.YEAR) != curYear) {
			current.set(Calendar.YEAR, curYear);
		}
	}
	private void cleanButtons(Object[] buttons) {
		for (Object but : buttons) {
			ui.setText(but, "");
			ui.setEnabled(but, false);
			ui.setColor(but, Thinlet.FOREGROUND, Color.BLUE);
		}
	}
	private boolean isDayToHighlight() {
		return (current.get(Calendar.DATE) == dayToHighlight 
				&& current.get(Calendar.MONTH) == monthToHighlight
				&& current.get(Calendar.YEAR) == yearToHighlight);
	}
	/**
	 * Gets the name of the month with the specified index
	 * @param i 0-based index of the month: 0=jan, 11=dec
	 * @return Month name
	 */
	private String getMonthAsString(int i) {
		return InternationalisationUtils.getI18nString(FrontlineSMSConstants.MONTH_KEYS[i]);
	}
}
