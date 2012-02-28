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
package net.frontlinesms.data.repository;

import java.util.Collection;
import java.util.List;

import net.frontlinesms.data.domain.*;

/**
 * Factory for the creation of KeywordActions.
 * @author Alex Anderson <alex@frontlinesms.com>
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public interface KeywordActionDao {
	/**
	 * Gets all keyword actions of TYPE_REPLY.
	 * @return all {@link KeywordAction} of type {@link KeywordAction#REPLY}
	 */
	public Collection<KeywordAction> getReplyActions();

	/**
	 * Deletes a {@link KeywordAction}.
	 * @param action action to delete
	 */
	public void deleteKeywordAction(KeywordAction action);
	
	/**
	 * Saves a {@link KeywordAction}
	 * @param action action to save
	 */
	public void saveKeywordAction(KeywordAction action);
	
	/**
	 * Updates a {@link KeywordAction}
	 * @param action action to update
	 */
	public void updateKeywordAction(KeywordAction action);
	
	/**
	 * Gets a list of all {@link KeywordAction}s for the supplied keyword.
	 * @param keyword
	 * @return all {@link KeywordAction}s attached to the supplied {@link Keyword}
	 */
	public List<KeywordAction> getActions(Keyword keyword);
	
	/**
	 * Gets a count of all {@link KeywordAction}s.
	 * @return Total number of {@link KeywordAction}s.
	 */
	public int getCount();
	
	/**
	 * Gets a {@link KeywordAction} of a specified type for a particular {@link Keyword}.
	 * @param keyword The keyword whose action we want to fetch
	 * @param actionType the type of the action to fetch
	 * @return The action of the specified type, or <code>null</code> if there is none available
	 */
	public KeywordAction getAction(Keyword keyword, KeywordAction.Type actionType);
	
	/**
	 * Increments the counter for this action
	 * @param action
	 */
	public void incrementCounter(KeywordAction action);

	public void deleteForEmailAccount(EmailAccount email);
}
