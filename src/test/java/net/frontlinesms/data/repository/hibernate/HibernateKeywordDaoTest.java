/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;


import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.data.repository.KeywordDao;
import net.frontlinesms.junit.HibernateTestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Test class for {@link HibernateKeywordDao}
 * @author Alex
 */
public class HibernateKeywordDaoTest extends HibernateTestCase {
//> STATIC CONSTANTS
	/** The description applied to the blank keyword created in {@link #setDao(KeywordDao)} */
	private static final String BLANK_KEYWORD_DESCRIPTION = "The blank keyword.";

//> PROPERTIES
	/** Logging object */
	private final Log log = LogFactory.getLog(getClass());
	/** Instance of this DAO implementation we are testing. */
	private KeywordDao dao;
	/** The blank keyword that should be saved in {@link #dao} by {@link #setDao(KeywordDao)} */
	private Keyword blankKeyword;
	
//> TEST METHODS
	/**
	 * Test everything all at once!
	 * @throws DuplicateKeyException if there was a problem creating a keyword required by this test
	 */
	public void test() throws DuplicateKeyException {
		// Confirm that the blank keyword exists
		assertEquals(1, dao.getAllKeywords().size());
		
		Keyword simple = new Keyword("simple", "a very simple keyword");
		assertEquals(1, dao.getAllKeywords().size());
		dao.saveKeyword(simple);
		assertEquals(2, dao.getAllKeywords().size());
		assertEquals(dao.getAllKeywords(), dao.getAllKeywords());
		Keyword simpleChild = new Keyword("simple child", "a child of the very simple keyword");
		assertEquals(2, dao.getAllKeywords().size());
		assertEquals(dao.getAllKeywords(), dao.getAllKeywords());
		dao.saveKeyword(simpleChild);
		assertEquals(3, dao.getAllKeywords().size());
		
		dao.deleteKeyword(simpleChild);
		assertEquals(2, dao.getAllKeywords().size());
	}
	
	/**
	 * Test that creation of duplicate keywords fails in the expected manner.
	 * @throws DuplicateKeyException if there was a problem creating a keyword required by this test
	 */
	public void testDuplicates() throws DuplicateKeyException {
		try {
			dao.saveKeyword(new Keyword("", BLANK_KEYWORD_DESCRIPTION));
			fail("Duplicate keyword was successfully saved.  This should not be allowed.");
		} catch(DuplicateKeyException ex) {}
		
		try {
			Keyword newKeyword = new Keyword("", "different description");
			dao.saveKeyword(newKeyword);
			fail("Duplicate keyword ('" + newKeyword.getKeyword() + "':\"" + newKeyword.getDescription() + "\") was successfully saved.  This should not be allowed.");
		} catch(DuplicateKeyException ex) {}
		
		dao.saveKeyword(new Keyword("one", ""));
		try {
			dao.saveKeyword(new Keyword("one", ""));
			fail("Duplicate keyword was successfully saved.  This should not be allowed.");
		} catch(DuplicateKeyException ex) { /* expected */ }
		try {
			dao.saveKeyword(new Keyword("one", "different description"));
			fail("Duplicate keyword was successfully saved.  This should not be allowed.");
		} catch(DuplicateKeyException ex) { /* expected */ }
	}
	
	/**
	 * Tests matching to the BLANK keyword
	 * @throws DuplicateKeyException if there was a problem creating a keyword required by this test
	 */
	public void testBlankKeywordMatching() throws DuplicateKeyException {
		// Set up the test data - a blank keyword to match, and another keyword to avoid
		Keyword blankKeyword = dao.getFromMessageText("");
		// Check we have got the blank keyword successfully
		assertEquals("", blankKeyword.getKeyword());
		assertEquals(BLANK_KEYWORD_DESCRIPTION, blankKeyword.getDescription());
		
		Keyword avoidKeyword = new Keyword("a", "");
		dao.saveKeyword(avoidKeyword);
		
		// Check a blank message matches BLANK
		testKeywordMatching(blankKeyword, "");

		// Check an empty message matches BLANK
		testKeywordMatching(blankKeyword, " ");
		testKeywordMatching(blankKeyword, "\r\n");

		// Check a random message matches BLANK
		testKeywordMatching(blankKeyword, "zxcvb");
		testKeywordMatching(blankKeyword, "zxcvb nm");
		
		// Check some things which should NOT match BLANK
		testKeywordMatching(avoidKeyword, "a");
		testKeywordMatching(avoidKeyword, "a non-blank message");
		testKeywordMatching(blankKeyword, "ablank");
	}
	
	/**
	 * @throws DuplicateKeyException if there was a problem creating a keyword required by this test
	 */
	public void testKeywordMatching() throws DuplicateKeyException {
		Keyword keyword1 = new Keyword("one", "");
		dao.saveKeyword(keyword1);
		
		Keyword keyword2 = new Keyword("two", "");
		dao.saveKeyword(keyword2);
		
		Keyword keyword3 = new Keyword("three", "");
		dao.saveKeyword(keyword3);
		
		Keyword keyword1a = new Keyword("one a", "");
		dao.saveKeyword(keyword1a);
		
		Keyword keyword2a = new Keyword("two a", "");
		dao.saveKeyword(keyword2a);
		
		Keyword keyword3a = new Keyword("three a", "");
		dao.saveKeyword(keyword3a);
		
		Keyword keyword1ax = new Keyword("one a x", "");
		dao.saveKeyword(keyword1ax);
		
		Keyword keyword1b = new Keyword("one b", "");
		dao.saveKeyword(keyword1b);
		
		Keyword keyword1byz = new Keyword("one b y z", "");
		dao.saveKeyword(keyword1byz);

		testKeywordMatching(keyword1, "one");
		testKeywordMatching(keyword1, "one ");
		testKeywordMatching(keyword1, "one is the keyword that we seek");
		testKeywordMatching(keyword1, "one as the keyword that we seek");
		testKeywordMatching(keyword1a, "one a");
		testKeywordMatching(keyword1a, "one a is the keyword that we seek");
		testKeywordMatching(keyword1a, "one a xis the keyword that we seek");
		testKeywordMatching(keyword1ax, "one a x");
		testKeywordMatching(keyword1ax, "one a x is the keyword that we seek");
		
		// Test again, with upper cases
		testKeywordMatching(keyword1, "ONE");
		testKeywordMatching(keyword1, "ONE ");
		testKeywordMatching(keyword1, "ONE IS THE KEYWORD THAT WE SEEK");
		testKeywordMatching(keyword1, "ONE AS THE KEYWORD THAT WE SEEK");
		testKeywordMatching(keyword1a, "ONE A");
		testKeywordMatching(keyword1a, "ONE A IS THE KEYWORD THAT WE SEEK");
		testKeywordMatching(keyword1a, "ONE A XIS THE KEYWORD THAT WE SEEK");
		testKeywordMatching(keyword1ax, "ONE A X");
		testKeywordMatching(keyword1ax, "ONE A X IS THE KEYWORD THAT WE SEEK");

		// Test again with mixed cases
		testKeywordMatching(keyword1, "oNe");
		testKeywordMatching(keyword1, "onE ");
		testKeywordMatching(keyword1, "One is the keyword that we seek");
		testKeywordMatching(keyword1, "oNE as the keyword that we seek");
		testKeywordMatching(keyword1a, "ONE a");
		testKeywordMatching(keyword1a, "one A is the keyword that we seek");
		testKeywordMatching(keyword1a, "one a XIS THE KEYWORD THAT WE SEEK");
		testKeywordMatching(keyword1ax, "one a X");
		testKeywordMatching(keyword1ax, "ONe A x is the keyword that we seek");
		
		// Test no match
		testKeywordMatching(blankKeyword, "my one two three is a four five six");
	}
	
	private void testKeywordMatching(Keyword expectedKeyword, String messageText) {
		Keyword fetchedKeyword = dao.getFromMessageText(messageText);
		assertEquals("Incorrect keyword retrieved for message text: '" + messageText + "'", expectedKeyword, fetchedKeyword);
	}
	
//> TEST SETUP/TEARDOWN
	@Override
	protected void onSetUpInTransaction() throws Exception {
		this.blankKeyword = new Keyword("", BLANK_KEYWORD_DESCRIPTION);
		this.dao.saveKeyword(blankKeyword);
		
		super.onSetUpInTransaction();
	}
	
//> ACCESSORS
	/** @param d The DAO to use for the test. 
	 * @throws DuplicateKeyException */
	@Required
	public void setKeywordDao(KeywordDao d) throws DuplicateKeyException {
		// we can just set the DAO once in the test
		this.dao = d;
	}
}
