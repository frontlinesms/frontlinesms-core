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
package net.frontlinesms.messaging.sms.properties;

import java.util.*;

import net.frontlinesms.data.StructuredProperties;

/**
 * Defines a checkbox that enbales or disables its fields according to
 * the selection.
 *
 * @author Carlos Eduardo Endler Genz
 * @date 05/02/2009
 */
public class OptionalSection {
	private boolean value;
	private StructuredProperties dependencies;
	
	public OptionalSection() {
		dependencies = new StructuredProperties();
	}
	
	/**
	 * @return the value
	 */
	public boolean getValue() {
		return value;
	}
	/**
	 * @param value the value to set
	 */
	public void setValue(boolean value) {
		this.value = value;
	}
	/**
	 * @return the dependencies
	 */
	public StructuredProperties getDependencies() {
		return dependencies;
	}
	
	public void addDependency(String property, Object value) {
		dependencies.put(property, value);
	}
}
