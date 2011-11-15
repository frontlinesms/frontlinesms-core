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
package net.frontlinesms.ui;

/**
 * This represents an event instance that will appear on the latest events table in the Home tab.
 * @author Carlos Eduardo Endler Genz
 * @date 19/02/2009
 */
public class HomeTabEvent {
	public enum Type {
		INCOMING_MESSAGE(Icon.SMS_RECEIVE),
		OUTGOING_MESSAGE(Icon.SMS_SEND),
		OUTGOING_MESSAGE_FAILED(Icon.SMS_SEND_FAILURE),
		OUTGOING_EMAIL(Icon.EMAIL_SEND),
		PHONE_CONNECTED(Icon.PHONE_CONNECTED), 
		SMS_INTERNET_SERVICE_CONNECTED(Icon.SMS_INTERNET_SERVICE_CONNECTED),
		SMS_INTERNET_SERVICE_RECEIVING_FAILED(Icon.SMS_INTERNET_SERVICE_RECEIVING_FAILED),
		INCOMING_MMS(Icon.MMS_RECEIVE),
		/** Some generic events for plugins to use. */
		GREEN(Icon.LED_GREEN),
		AMBER(Icon.LED_AMBER),
		RED(Icon.LED_RED);
		
		private final String icon;
		
		Type(String icon) {
			this.icon = icon;
		}
		
		String getIcon() {
			return icon;
		}
	}
	
	private Type type;
	private String description;
	private long time;
	
	
	public HomeTabEvent(Type type, String description) {
		this.type = type;
		this.description = description;
		this.time = System.currentTimeMillis();
	}

	public Type getType() {
		return type;
	}

	public String getDescription() {
		return description;
	}

	public long getTime() {
		return time;
	}
	
	public String getIcon() {
		return type.getIcon();
	}
}
