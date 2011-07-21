/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;

import net.frontlinesms.data.EntityField;
import net.frontlinesms.data.Order;
import net.frontlinesms.data.domain.Email;
import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.data.domain.FrontlineMessage.Field;
import net.frontlinesms.data.domain.FrontlineMessage.Type;
import net.frontlinesms.data.repository.MessageDao;

/**
 * Hibernate implementation of {@link MessageDao}.
 * @author Alex
 */
public class HibernateMessageDao extends BaseHibernateDao<FrontlineMessage> implements MessageDao {
	/** Create instance of this class */
	public HibernateMessageDao() {
		super(FrontlineMessage.class);
	}

	/** @see MessageDao#deleteMessage(FrontlineMessage) */
	public void deleteMessage(FrontlineMessage message) {
		super.delete(message);
	}

	/** @see MessageDao#getAllMessages() */
	public List<FrontlineMessage> getAllMessages() {
		return super.getAll();
	}
	
	public FrontlineMessage getMessage(long id) {
		return super.getUnique(super.getCriterion().add(Restrictions.eq(Field.ID.getFieldName(), id)));
	}

	/** @see MessageDao#getAllMessages(int, Field, Order, Long, Long, int, int) */
	public List<FrontlineMessage> getAllMessages(FrontlineMessage.Type messageType, Field sortBy, Order order, Long start, Long end, int startIndex, int limit) {
		DetachedCriteria criteria = super.getSortCriterion(sortBy, order);
		addTypeCriteria(criteria, messageType);
		addDateCriteria(criteria, start, end);
		return super.getList(criteria, startIndex, limit);
	}

	/** @see MessageDao#getMessageCount(int, FrontlineMessage.Status[]) */
	public int getMessageCount(FrontlineMessage.Type messageType, FrontlineMessage.Status... messageStatuses) {
		DetachedCriteria criteria = DetachedCriteria.forClass(Email.class);
		addStatusCriteria(criteria, messageStatuses);
		addTypeCriteria(criteria, messageType);
		return getCount(criteria);
	}

	/** @see MessageDao#getMessageCount(int, Long, Long) */
	public int getMessageCount(FrontlineMessage.Type messageType, Long start, Long end) {
		DetachedCriteria criteria = super.getCriterion();
		addDateCriteria(criteria, start, end);
		addTypeCriteria(criteria, messageType);
		return getCount(criteria);
	}

	/** @see MessageDao#getMessageCount(int, Keyword, Long, Long) */
	public int getMessageCount(FrontlineMessage.Type messageType, Keyword keyword, Long start, Long end) {
		PartialQuery<FrontlineMessage> q = createQueryStringForKeyword(true, messageType, keyword);
		
		if (start != null) {
			q.appendWhereOrAnd();
			if (end != null) {
				q.append("(message." + FrontlineMessage.Field.DATE.getFieldName() + ">=? AND message." + FrontlineMessage.Field.DATE.getFieldName() + "<=?)", start, end);
			} else {
				q.append("(message." + FrontlineMessage.Field.DATE.getFieldName() + ">=?)", start);	
			}			
		} else if (end != null) {
			q.appendWhereOrAnd();
			q.append("(message." + FrontlineMessage.Field.DATE.getFieldName() + "<=?)", end);
		}
		
		return super.getCount(q.getQueryString(), q.getInsertValues());
	}

	/** @see MessageDao#getMessageCountForMsisdn(int, String, Long, Long) */
	public int getMessageCountForMsisdn(FrontlineMessage.Type messageType, String phoneNumber, Long start, Long end) {
		DetachedCriteria criteria = super.getCriterion();
		addTypeCriteria(criteria, messageType);
		addDateCriteria(criteria, start, end);
		addPhoneNumberMatchCriteria(criteria, phoneNumber, true, true);
		return getCount(criteria);
	}

	/** @see MessageDao#getMessageForStatusUpdate(String, int) */
	public FrontlineMessage getMessageForStatusUpdate(String targetMsisdnSuffix, int smscReference) {
		DetachedCriteria criteria = super.getCriterion();
		criteria.add(Restrictions.eq(Field.RECIPIENT_MSISDN.getFieldName(), targetMsisdnSuffix));
		criteria.add(Restrictions.eq(Field.SMSC_REFERENCE.getFieldName(), smscReference));
		return super.getUnique(criteria);
	}

	/** @see MessageDao#getMessages(int, Field, Order) */
	public List<FrontlineMessage> getMessages(FrontlineMessage.Type messageType, Field sortBy, Order order) {
		DetachedCriteria criteria = super.getSortCriterion(sortBy, order);
		addTypeCriteria(criteria, messageType);
		return getList(criteria);
	}

	/** @see MessageDao#getMessagesForKeyword(int, Keyword, Field, Order, Long, Long, int, int) */
	public List<FrontlineMessage> getMessages(FrontlineMessage.Type messageType, Keyword keyword, Field sortBy, Order order) {
		DetachedCriteria criteria = getSortCriterion(sortBy, order);
		addTypeCriteria(criteria, messageType);
		addKeywordMatchCriteria(criteria, keyword);
		return getList(criteria);
	}

	/** @see MessageDao#getMessages(int, FrontlineMessage.Status[]) */
	public Collection<FrontlineMessage> getMessages(FrontlineMessage.Type messageType, FrontlineMessage.Status... statuses) {
		DetachedCriteria criteria = super.getCriterion();
		addTypeCriteria(criteria, messageType);
		addStatusCriteria(criteria, statuses);
		return getList(criteria);
	}
	
	public int getMessageCount(FrontlineMessage.Type messageType, List<String> phoneNumbers,
			Long messageHistoryStart, Long messageHistoryEnd) {
		return super.getCount(getCriteria(messageType, phoneNumbers,
				messageHistoryStart, messageHistoryEnd));
	}
	
	public List<FrontlineMessage> getMessages(FrontlineMessage.Type messageType,
			List<String> phoneNumbers, Long messageHistoryStart,
			Long messageHistoryEnd) {
		return super.getList(getCriteria(messageType, phoneNumbers,
				messageHistoryStart, messageHistoryEnd));
	}
	
	public List<FrontlineMessage> getMessages(FrontlineMessage.Type messageType,
			List<String> phoneNumbers, Long messageHistoryStart,
			Long messageHistoryEnd, int startIndex, int limit) {
		return super.getList(getCriteria(messageType, phoneNumbers,
				messageHistoryStart, messageHistoryEnd), startIndex, limit);
	}

	private DetachedCriteria getCriteria(FrontlineMessage.Type messageType,
			List<String> phoneNumbers, Long messageHistoryStart,
			Long messageHistoryEnd) {
		DetachedCriteria criteria = super.getCriterion();
		addTypeCriteria(criteria, messageType);
		addDateCriteria(criteria, messageHistoryStart, messageHistoryEnd);
		addPhoneNumberMatchCriteria(criteria, phoneNumbers, true, true);
		return criteria;
	}

	/** @see MessageDao#getMessagesForKeyword(int, Keyword, Field, Order, Long, Long, int, int) */
	public List<FrontlineMessage> getMessagesForKeyword(FrontlineMessage.Type messageType, Keyword keyword, Field sortBy, Order order, Long start, Long end, int startIndex, int limit) {
		PartialQuery<FrontlineMessage> q = createQueryStringForKeyword(false, messageType, keyword);
		
		if (start != null) {
			q.appendWhereOrAnd();
			if (end != null) {
				q.append("(message." + FrontlineMessage.Field.DATE.getFieldName() + ">=? AND message." + FrontlineMessage.Field.DATE.getFieldName() + "<=?)", start, end);
			} else {
				q.append("(message." + FrontlineMessage.Field.DATE.getFieldName() + ">=?)", start);	
			}			
		} else if (end != null) {
			q.appendWhereOrAnd();
			q.append("(message." + FrontlineMessage.Field.DATE.getFieldName() + "<=?)", end);
		}
		
		
		q.addSorting(sortBy, order);
		return super.getList(q.getQueryString(), startIndex, limit, q.getInsertValues());
	}

	/** @see MessageDao#getMessagesForKeyword(int, Keyword) */
	@SuppressWarnings("unchecked")
	public List<FrontlineMessage> getMessagesForKeyword(FrontlineMessage.Type messageType, Keyword keyword) {
		PartialQuery q = createQueryStringForKeyword(false, messageType, keyword);
		return super.getList(q.getQueryString(), q.getInsertValues());
	}
	
	@SuppressWarnings("unchecked")
	List<String> getSimilarKeywords(Keyword keyword) {
		if(keyword.getKeyword().length() == 0) {
			// Get all keywords apart from the blank one
			List<String> allKeywordsExceptBlank = this.getHibernateTemplate().find("SELECT k.keyword FROM Keyword AS k WHERE LENGTH(k.keyword) > 0");
			return allKeywordsExceptBlank;
		} else {
			String likeKeyword = keyword.getKeyword() + " %";
			List<String> similarKeywords = this.getHibernateTemplate().find("SELECT k.keyword FROM Keyword  AS k WHERE k.keyword LIKE ?", likeKeyword);
			return similarKeywords;
		}
	}

	/** @see MessageDao#getMessagesForMsisdn(int, String, Field, Order, Long, Long, int, int) */
	public List<FrontlineMessage> getMessagesForMsisdn(FrontlineMessage.Type messageType, String phoneNumber, Field sortBy, Order order, Long start, Long end, int startIndex, int limit) {
		DetachedCriteria criteria = super.getSortCriterion(sortBy, order);
		addTypeCriteria(criteria, messageType);
		addDateCriteria(criteria, start, end);
		addPhoneNumberMatchCriteria(criteria, phoneNumber, true, true);
		return super.getList(criteria, startIndex, limit);
	}

	/** @see MessageDao#getMessagesForMsisdn(int, String, Field, Order, Long, Long) */
	public List<FrontlineMessage> getMessagesForMsisdn(FrontlineMessage.Type messageType, String phoneNumber, Field sortBy, Order order, Long start, Long end) {
		DetachedCriteria criteria = super.getSortCriterion(sortBy, order);
		addTypeCriteria(criteria, messageType);
		addDateCriteria(criteria, start, end);
		addPhoneNumberMatchCriteria(criteria, phoneNumber, true, true);
		return super.getList(criteria);
	}

	/** @see MessageDao#getMessagesForStati(int, FrontlineMessage.Status[], Field, Order, int, int) */
	public List<FrontlineMessage> getMessagesForStati(FrontlineMessage.Type messageType, FrontlineMessage.Status[] messageStatuses, Field sortBy, Order order, int startIndex, int limit) {
		DetachedCriteria criteria = super.getSortCriterion(sortBy, order);
		addTypeCriteria(criteria, messageType);
		addStatusCriteria(criteria, messageStatuses);
		return super.getList(criteria, startIndex, limit);
	}

	/** @see MessageDao#getSMSCount(Long, Long) */
	public int getSMSCount(Long start, Long end) {
		DetachedCriteria criteria = super.getCriterion();
		addDateCriteria(criteria, start, end);
		return super.getCount(criteria);
	}

	/** @see MessageDao#getSMSCountForKeyword(Keyword, Long, Long) */
	public int getSMSCountForKeyword(Keyword keyword, Long start, Long end) {
		DetachedCriteria criteria = super.getCriterion();
		addDateCriteria(criteria, start, end);
		addKeywordMatchCriteria(criteria, keyword);
		return super.getCount(criteria);
	}

	/** @see MessageDao#getSMSCountForMsisdn(String, Long, Long) */
	public int getSMSCountForMsisdn(String phoneNumber, Long start, Long end) {
		DetachedCriteria criteria = super.getCriterion();
		addDateCriteria(criteria, start, end);
		addPhoneNumberMatchCriteria(criteria, phoneNumber, true, true);
		return super.getCount(criteria);
	}
	
	public List<FrontlineMessage> getMessages(Type messageType, Long messageHistoryStart, Long messageHistoryEnd) {
		DetachedCriteria criteria = super.getCriterion();
		addTypeCriteria(criteria, messageType);
		addDateCriteria(criteria, messageHistoryStart, messageHistoryEnd);
		return super.getList(criteria);
	}
	
	public List<FrontlineMessage> getMessagesForKeyword(FrontlineMessage.Type messageType, Keyword keyword, Long start, Long end) {
		PartialQuery<FrontlineMessage> q = createQueryStringForKeyword(false, messageType, keyword);
		
		if (start != null) {
			q.appendWhereOrAnd();
			if (end != null) {
				q.append("(message." + FrontlineMessage.Field.DATE.getFieldName() + ">=? AND message." + FrontlineMessage.Field.DATE.getFieldName() + "<=?)", start, end);
			} else {
				q.append("(message." + FrontlineMessage.Field.DATE.getFieldName() + ">=?)", start);	
			}			
		} else if (end != null) {
			q.appendWhereOrAnd();
			q.append("(message." + FrontlineMessage.Field.DATE.getFieldName() + "<=?)", end);
		}
		
		return super.getList(q.getQueryString(), q.getInsertValues());
	}

	/** @see MessageDao#saveMessage(FrontlineMessage) */
	public void saveMessage(FrontlineMessage message) {
		super.saveWithoutDuplicateHandling(message);
	}

	/** @see MessageDao#updateMessage(FrontlineMessage) */
	public void updateMessage(FrontlineMessage message) {
		super.updateWithoutDuplicateHandling(message);
	}
	
	/**
	 * Augments the supplied criteria with that required to match a keyword.
	 * @param criteria
	 * @param keyword 
	 */
	private void addKeywordMatchCriteria(DetachedCriteria criteria, Keyword keyword) {
		String keywordString = keyword.getKeyword();
		// FIXME this should be case-insensitive
		Criterion matchKeyword = Restrictions.or(
				Restrictions.ilike(Field.MESSAGE_CONTENT.getFieldName(), keywordString, MatchMode.ANYWHERE), // This should match the keyword exactly, case insensitive
				Restrictions.ilike(Field.MESSAGE_CONTENT.getFieldName(), keywordString + ' ', MatchMode.ANYWHERE));
		criteria.add(matchKeyword);
	}
	
	/**
	 * Augments the supplied criteria with that required to match an msisdn, either for the sender, the receiver or both.
	 * @param criteria
	 * @param phoneNumber 
	 * @param sender 
	 * @param receiver 
	 */
	private void addPhoneNumberMatchCriteria(DetachedCriteria criteria, String phoneNumber, boolean sender, boolean receiver) {
		if(!sender && !receiver) {
			throw new IllegalStateException("This neither sender nor receiver matching is requested.");
		}
		SimpleExpression eqSender = Restrictions.eq(Field.SENDER_MSISDN.getFieldName(), phoneNumber);
		SimpleExpression eqReceiver = Restrictions.eq(Field.RECIPIENT_MSISDN.getFieldName(), phoneNumber);
		if(sender && receiver) {
			criteria.add(Restrictions.or(eqSender, eqReceiver));
		} else if(sender) {
			criteria.add(eqSender);
		} else if(receiver) {
			criteria.add(eqReceiver);
		}
	}
	
	/**
	 * Augments the supplied criteria with that required to match an msisdn, either for the sender, the receiver or both.
	 * @param criteria
	 * @param phoneNumber 
	 * @param sender 
	 * @param receiver 
	 */
	private void addPhoneNumberMatchCriteria(DetachedCriteria criteria, List<String> phoneNumbers, boolean sender, boolean receiver) {
		if(!sender && !receiver) {
			throw new IllegalStateException("This neither sender nor receiver matching is requested.");
		}

		// build multi level OR
		Criterion ors = null;
		for(String phoneNumber : phoneNumbers) {
			if(sender) {
				SimpleExpression eqSender = Restrictions.eq(Field.SENDER_MSISDN.getFieldName(), phoneNumber);
				if(ors == null) ors = eqSender;
				else ors = Restrictions.or(ors, eqSender);
			}

			if(receiver) {
				SimpleExpression eqReceiver = Restrictions.eq(Field.RECIPIENT_MSISDN.getFieldName(), phoneNumber);
				if(ors == null) ors = eqReceiver;
				else ors = Restrictions.or(ors, eqReceiver);
			}
		}
		
		// If we've made any ORs, apply them to the criteria
		if(ors != null) {
			criteria.add(ors);
		}
	}
	
	/**
	 * Augments the supplied criteria with that required to match a date range.
	 * @param criteria
	 * @param start
	 * @param end
	 */
	private void addDateCriteria(DetachedCriteria criteria, Long start, Long end) {
		if(start != null) {
			criteria.add(Restrictions.ge(Field.DATE.getFieldName(), start));
		}
		if(end != null) {
			criteria.add(Restrictions.le(Field.DATE.getFieldName(), end));
		}
	}
	
	/**
	 * Augments the supplied criteria with that required to match a date range.
	 * @param criteria
	 * @param messageType 
	 */
	private void addTypeCriteria(DetachedCriteria criteria, FrontlineMessage.Type messageType) {
		if(messageType != Type.ALL) {
			criteria.add(Restrictions.eq(Field.TYPE.getFieldName(), messageType));
		}
	}

	/**
	 * Augments the supplied criteria with that required to match a date range.
	 * @param criteria 
	 * @param statuses 
	 */
	private void addStatusCriteria(DetachedCriteria criteria, FrontlineMessage.Status... statuses) {
		criteria.add(Restrictions.in(Field.STATUS.getFieldName(), statuses));
	}
	
	/**
	 * 
	 */
	private PartialQuery<FrontlineMessage> createQueryStringForKeyword(boolean isCount, FrontlineMessage.Type messageType, Keyword keyword) {
		PartialQuery<FrontlineMessage> q = new PartialQuery<FrontlineMessage>();
		// Build a list of values to insert into the query string
		String selectString = "message";
		if (isCount)
			selectString = "count(*)";
		
		q.append("SELECT " + selectString + " FROM " + FrontlineMessage.class.getName() + " message");
		
		if(messageType != FrontlineMessage.Type.ALL) {
			q.append("WHERE");
			q.append("message.type=?", messageType);
		}
		
		if(keyword.getKeyword().length() > 0) {
			q.appendWhereOrAnd();
			String likeKeyword = keyword.getKeyword() + " %";
			
			q.append("(UPPER(message." + FrontlineMessage.Field.MESSAGE_CONTENT.getFieldName() + ") LIKE ?", keyword.getKeyword());
			q.append("OR UPPER(message." + FrontlineMessage.Field.MESSAGE_CONTENT.getFieldName() + ") LIKE ?)", likeKeyword);
		}
		
		List<String> similarKeywords = getSimilarKeywords(keyword);
		if(similarKeywords.size() > 0) {
			// Build the query to ignore messages that match similar keywords
			q.appendWhereOrAnd();
			for(int i=0; i<similarKeywords.size(); ++i) {
				if(i > 0) {
					q.append("AND");
				}
				
				String similarKeyword = similarKeywords.get(i);
				String likeSimilarKeyword = similarKeyword + " %";
				
				q.append("NOT (UPPER(message." + FrontlineMessage.Field.MESSAGE_CONTENT.getFieldName() + ") LIKE ?", similarKeyword);
				q.append("OR UPPER(message." + FrontlineMessage.Field.MESSAGE_CONTENT.getFieldName() + ") LIKE ?)", likeSimilarKeyword);
				
			}
		}
		
		return q;
	}
}

class PartialQuery<E> {
	private boolean whereAdded;
	private final StringBuilder queryStringBuilder = new StringBuilder();
	private final LinkedList<Object> insertValues = new LinkedList<Object>();
	
	private EntityField<E> orderBy; 
	private Order order;
	
	boolean isWhereClauseAdded() {
		return this.whereAdded;
	}
	String getQueryString() {
		String queryString = this.queryStringBuilder.toString();
		
		// Add sorting if required
		if(orderBy != null) {
			queryString += "ORDER BY " + orderBy.getFieldName() + " " + order.toHqlString() + " ";
		}
		
		return queryString;
	}
	Object[] getInsertValues() {
		return this.insertValues.toArray(new Object[0]);
	}
	
	public void appendWhereOrAnd() {
		if(!isWhereClauseAdded()) {
			this.append("WHERE");
		} else {
			this.append("AND");
		}
	}
	
	public void append(String s) {
		this.queryStringBuilder.append(s);
		this.queryStringBuilder.append(' ');
		
		if(s.contains("WHERE")) {
			assert(whereAdded == false) : "CANNOT INSERT 2 WHERE CLAUSES INTO ONE QUERY";
			whereAdded = true;
		}
	}
	
	public void append(EntityField<E> field) {
		this.queryStringBuilder.append(field.getFieldName());
	}
	
	public void append(String s, Object... insertValues) {
		this.append(s);
		for(Object insertValue : insertValues) {
			this.insertValues.add(insertValue);
		}
	}

	public void addSorting(EntityField<E> orderBy, Order order) {
		this.orderBy = orderBy;
		this.order = order;
	}
}