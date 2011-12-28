/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;

import java.util.List;


import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.data.domain.KeywordAction;
import net.frontlinesms.data.repository.KeywordActionDao;
import net.frontlinesms.data.repository.KeywordDao;
import net.frontlinesms.junit.HibernateTestCase;

import org.springframework.beans.factory.annotation.Required;

/**
 * Test class for {@link HibernateKeywordActionDao}
 * @author Alex Anderson <alex@frontlinesms.com>
 * @author Morgan Belkadi <morgan@frontlinesms.com>
 */
public class HibernateKeywordActionDaoTest extends HibernateTestCase {
//> PROPERTIES
	/** {@link KeywordActionDao} instance to test against. */
	private KeywordActionDao keywordActionDao; // TODO should this explicitly be a Hibernate*Dao?
	/** {@link KeywordDao} instance to test against. */
	private KeywordDao keywordDao;
	
	private Keyword testKeyword;
	private Keyword testKeyword2;

//> TEST METHODS
	public void test() {
		KeywordAction action = KeywordAction.createReplyAction(this.testKeyword, "some reply text", 14343274L, 21340345L);
		this.keywordActionDao.saveKeywordAction(action);
		
		assertEquals(action, this.keywordActionDao.getAction(testKeyword, KeywordAction.Type.REPLY));
		List<KeywordAction> retrievedActionList = this.keywordActionDao.getActions(testKeyword);
		assertEquals(1, retrievedActionList.size());
		assertEquals(action, retrievedActionList.get(0));
		
		this.keywordActionDao.deleteKeywordAction(action);
		
		assertEquals(0, this.keywordActionDao.getActions(testKeyword).size());
		assertNull(this.keywordActionDao.getAction(testKeyword, KeywordAction.Type.REPLY));
	}
	
	public void testKeywordActionsCount () {
		final long startDate = 14343274L;
		final long endDate = 21340345L;
		KeywordAction action = KeywordAction.createReplyAction(this.testKeyword, "some reply text", startDate, endDate);
		KeywordAction action2 = KeywordAction.createReplyAction(this.testKeyword, "some reply text 2", startDate, endDate);
		KeywordAction action3 = KeywordAction.createReplyAction(this.testKeyword2, "some reply text for keyword 2", startDate, endDate);
		KeywordAction action4;
		
		this.keywordActionDao.saveKeywordAction(action);
		this.keywordActionDao.saveKeywordAction(action2);
		this.keywordActionDao.saveKeywordAction(action3);
		
		assertEquals(3, this.keywordActionDao.getCount());
		
		this.keywordActionDao.deleteKeywordAction(action2);
		assertEquals(2, this.keywordActionDao.getCount());
		
		this.keywordActionDao.deleteKeywordAction(action3);
		assertEquals(1, this.keywordActionDao.getCount());
		
		this.keywordActionDao.deleteKeywordAction(action);
		assertEquals(0, this.keywordActionDao.getCount());

		action4 = KeywordAction.createEmailAction(this.testKeyword2, "Reply Text", null, "", "", startDate, endDate);
		this.keywordActionDao.saveKeywordAction(action4);
		assertEquals(1, this.keywordActionDao.getCount());
	}
	
	public void testKeywordActionsIncrementCount() {
		final long startDate = 14343274L;
		final long endDate = 21340345L;
		KeywordAction action = KeywordAction.createReplyAction(this.testKeyword, "some reply text", startDate, endDate);
		
		this.keywordActionDao.saveKeywordAction(action);
		assertEquals(0, action.getCounter());
		assertEquals(0, this.keywordActionDao.getActions(this.testKeyword).get(0).getCounter());
		
		this.keywordActionDao.incrementCounter(action);
		assertEquals(1, action.getCounter());
		assertEquals(1, this.keywordActionDao.getActions(this.testKeyword).get(0).getCounter());
		
		int randomIncrements = (int)(Math.random() * 10);
		for (int i = 0 ; i < randomIncrements ; ++i) {
			this.keywordActionDao.incrementCounter(action);
		}
		
		assertEquals(randomIncrements + 1, action.getCounter());
		assertEquals(randomIncrements + 1, this.keywordActionDao.getActions(this.testKeyword).get(0).getCounter());
	}
	
//> INIT METHODS
	@Override
	protected void onSetUp() throws Exception {
		super.onSetUp();
		
		this.testKeyword = new Keyword("test", "test keyword");
		this.testKeyword2 = new Keyword("test2", "test keyword 2");
		
		this.keywordDao.saveKeyword(this.testKeyword);
		this.keywordDao.saveKeyword(this.testKeyword2);
	}
	
//> ACCESSORS
	/** @param d The DAO to use for the test. */
	@Required
	public void setKeywordActionDao(KeywordActionDao d) {
		this.keywordActionDao = d;
	}
	/** @param d The DAO to use for the test. */
	@Required
	public void setKeywordDao(KeywordDao d) {
		this.keywordDao = d;
	}
}
