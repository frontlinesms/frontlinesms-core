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
package net.frontlinesms.data;

/**
 * Exception thrown when there is an attempt to create a data object with a unique field
 * that is already used by another object of that class. 
 * @author Alex
 */
@SuppressWarnings("serial")
public class DuplicateKeyException extends Exception {
	/** Creates a new duplicate key exception. */
	public DuplicateKeyException() {
		super();
	}
	
	/**
	 * Creates a new duplicate key exception wrapping another exception.
	 * @param cause the cause of the {@link DuplicateKeyException}
	 */
	public DuplicateKeyException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * Creates a new duplicate key exception wrapping another exception.
	 *  @param message the cause of the {@link DuplicateKeyException}
	 */
	public DuplicateKeyException(String message) {
		super(message);
	}
}
