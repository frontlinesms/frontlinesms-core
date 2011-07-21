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

import net.frontlinesms.data.Order;
import net.frontlinesms.data.domain.*;
import net.frontlinesms.data.domain.FrontlineMessage.Field;

/**
 * Factory for creating instances of net.frontlinesms.data.Message
 * @author Alex
 */
public interface MessageDao {
	/**
	 * Gets all messages for the specified number. 
	 * @param type 
	 * @param number
	 * @param sortBy Message Field to sort the results by
	 * @param order direction to order results in
	 * @param start TODO
	 * @param end TODO
	 * @param startIndex 
	 * @param limit the maximum number of messages to recover
	 * @param index the result index of the messages to recover
	 * @return
	 */
	public List<FrontlineMessage> getMessagesForMsisdn(FrontlineMessage.Type type, String number, Field sortBy, Order order, Long start, Long end, int startIndex, int limit);

	/**
	 * Gets all messages for the specified number. 
	 * @param number
	 * @param sortBy Message Field to sort the results by
	 * @param order direction to order results in
	 * @param start TODO
	 * @param end TODO
	 * @param limit the maximum number of messages to recover
	 * @param index the result index of the messages to recover
	 * @return
	 */
	public List<FrontlineMessage> getMessagesForMsisdn(FrontlineMessage.Type type, String number, Field sortBy, Order order, Long start, Long end);
	
	/**
	 * Gets message count for the specified number. 
	 * @param number
	 * @param start TODO
	 * @param end TODO
	 * @return
	 */
	public int getMessageCountForMsisdn(FrontlineMessage.Type type, String number, Long start, Long end);
	
	/**
	 * Gets count of SMS sent for the specified number. 
	 * @param number
	 * @param start TODO
	 * @param end TODO
	 * @return
	 */
	public int getSMSCountForMsisdn(String number, Long start, Long end);
	
	/**
	 * Gets count of SMS sent. 
	 * @param start TODO
	 * @param end TODO
	 * @return
	 */
	public int getSMSCount(Long start, Long end);
	
	/**
	 * Gets count of SMS sent for the specified keyword. 
	 * @param keyword
	 * @param start TODO
	 * @param end TODO
	 * @return
	 */
	public int getSMSCountForKeyword(Keyword keyword, Long start, Long end);
	
	/**
	 * Gets all messages of a particular type (SENT, RECEIVED, ALL) which begin with the specified keyword.  If
	 * the supplied keyword is NULL, it will be ignored (i.e. all messages of requested type will be returned).
	 * @param messageType message type(s) to be retrieved, or Message.TYPE_ALL for all messages
	 * @param keyword word messages should start with, or NULL to retrieve all messages
	 * @param sortBy Message Field to sort the results by
	 * @param order direction to order results in
	 * @param start TODO
	 * @param end TODO
	 * @param limit the maximum number of messages to recover
	 * @param index the result index of the messages to recover
	 * @return
	 * FIXME keyword should never be null for this method, and messageType should be understood to
	 * be TYPE_RECEIVED always.  If other functionality is required, the method should be renamed
	 * or new methods created.
	 */
	public List<FrontlineMessage> getMessagesForKeyword(FrontlineMessage.Type messageType, Keyword keyword, Field sortBy, Order order, Long start, Long end, int startIndex, int limit);
	
	/**
	 * Gets all messages of a particular type (SENT, RECEIVED, ALL).
	 * @param messageType message type(s) to be retrieved, or Message.TYPE_ALL for all messages
	 * @param sortBy Message Field to sort the results by
	 * @param order direction to order results in
	 * @return
	 */
	public List<FrontlineMessage> getMessages(FrontlineMessage.Type messageType, Field sortBy, Order order);
	
	/**
	 * Gets all messages of a particular type (SENT, RECEIVED, ALL) which begin with the specified keyword.
	 * @param messageType message type(s) to be retrieved, or Message.TYPE_ALL for all messages
	 * @param keyword word messages should start with
	 * @return
	 */
	public List<FrontlineMessage> getMessagesForKeyword(FrontlineMessage.Type messageType, Keyword keyword);
	
	public List<FrontlineMessage> getMessagesForKeyword(FrontlineMessage.Type messageType, Keyword keyword, Long start, Long end);

	public List<FrontlineMessage> getMessagesForStati(FrontlineMessage.Type messageType, FrontlineMessage.Status[] messageStatuses, Field sortBy, Order order, int startIndex, int limit);
	
	/**
	 * Get the total number of messages with the supplied statuses.
	 * @param messageType
	 * @param messageStati
	 * @return
	 */
	public int getMessageCount(FrontlineMessage.Type messageType, FrontlineMessage.Status... messageStatuses);

	/**
	 * Gets all messages of a particular type (SENT, RECEIVED, ALL) which begin with the specified keyword.  If
	 * the supplied keyword is NULL, it will be ignored (i.e. all messages of requested type will be returned).
	 * 
	 * @param messageType message type(s) to be retrieved, or Message.TYPE_ALL for all messages
	 * @param keyword word messages should start with, or NULL to retrieve all messages
	 * @param sortBy Message Field to sort the results by
	 * @param order direction to order results in
	 * @param index the result index of the messages to recover
	 * @param limit the maximum number of messages to recover
	 * @return
	 */
	public List<FrontlineMessage> getMessages(FrontlineMessage.Type messageType, Keyword keyword, Field sortBy, Order order);
	
	/**
	 * Gets all messages.
	 * @return all messages in the system 
	 */
	public List<FrontlineMessage> getAllMessages();
	
	/**
	 * Gets a page of messages.
	 * @param type the type of the message
	 * @param field the field to sort by
	 * @param order the order to sort by
	 * @param start the start date for the messages
	 * @param end the end date for the messages
	 * @param startIndex the index of the first message to get
	 * @param limit the maximum number of messages to get
	 * @return list of all messages conforming to the specified constraints and sorted in a particular way.
	 *
	 */
	public List<FrontlineMessage> getAllMessages(FrontlineMessage.Type type, Field field, Order order, Long start, Long end, int startIndex, int limit);
	
	/**
	 * Gets the number of messages of a specific type from between the specified dates
	 * @param type
	 * @param start the start date as a java timestamp, or <code>null</code> for no start date restriction
	 * @param end the end date as a java timestamp, or <code>null</code> for no start date restriction
	 * @return count of messages
	 */
	public int getMessageCount(FrontlineMessage.Type type, Long start, Long end);
	
	/**
	 * Gets all messages with the supplied status and type.
	 * @param type
	 * @param status
	 * @return 
	 */
	public Collection<FrontlineMessage> getMessages(FrontlineMessage.Type type, FrontlineMessage.Status... status);
	
	/**
	 * Gets the number of messagesthere are of the given type for the given keyword.
	 * @param messageType
	 * @param keyword
	 * @param start
	 * @param end
	 * @return
	 */
	public int getMessageCount(FrontlineMessage.Type messageType, Keyword keyword, Long start, Long end);
	
	/**
	 * Gets the outgoing message with the matching SMSC Reference Number sent to
	 * a number ending with the supplied msisdn suffix.
	 * @param targetMsisdnSuffix last N digits of the target's msisdn
	 * @param smscReference
	 * @return
	 */
	public FrontlineMessage getMessageForStatusUpdate(String targetMsisdnSuffix, int smscReference);
	
	/** @return the number of messages sent to the specified phone numbers within the specified dates */
	public int getMessageCount(FrontlineMessage.Type messageType, List<String> phoneNumbers, Long messageHistoryStart, Long messageHistoryEnd);

	/** @return the messages sent or received to/from the specified phone numbers within the specified dates */
	public List<FrontlineMessage> getMessages(FrontlineMessage.Type messageType, List<String> phoneNumbers, Long messageHistoryStart, Long messageHistoryEnd);

	/** @return the messages sent or received to/from the specified phone numbers within the specified dates */
	public List<FrontlineMessage> getMessages(FrontlineMessage.Type messageType, List<String> phoneNumbers, Long messageHistoryStart, Long messageHistoryEnd, int startIndex, int limit);

	/** @return all messages sent or received within the specified dates */
	public List<FrontlineMessage> getMessages(FrontlineMessage.Type messageType, Long messageHistoryStart, Long messageHistoryEnd);

	/**
	 * Delete the supplied message to the data source.
	 * @param message the message to be deleted
	 */
	public void deleteMessage(FrontlineMessage message);

	/**
	 * Save the supplied message to the data source.
	 * @param message the message to be saved
	 */
	public void saveMessage(FrontlineMessage message);

	/**
	 * Update the supplied message in the data source.
	 * @param message the message to be updated
	 */
	public void updateMessage(FrontlineMessage message);
	
	public FrontlineMessage getMessage(long id);
}
