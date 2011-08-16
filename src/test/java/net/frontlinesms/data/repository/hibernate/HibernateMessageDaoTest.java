/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import net.frontlinesms.junit.HibernateTestCase;

import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.Order;
import net.frontlinesms.data.domain.FrontlineMultimediaMessage;
import net.frontlinesms.data.domain.FrontlineMultimediaMessagePart;
import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.data.domain.FrontlineMessage.Type;
import net.frontlinesms.data.repository.KeywordDao;
import net.frontlinesms.data.repository.MessageDao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Test class for {@link HibernateMessageDao}
 * @author Alex
 */
public class HibernateMessageDaoTest extends HibernateTestCase {
//> STATIC CONSTANTS
	private static final String ARTHUR = "+44123456789";
	private static final String BERNADETTE = "+447890123456";

	private static final long DATE_1970 = createDate(1970);
	private static final long DATE_1980 = createDate(1980);
	private static final long DATE_1990 = createDate(1990);
	private static final long DATE_2000 = createDate(2000);
	private static final long DATE_2010 = createDate(2010);
	
//> INSTANCE PROPERTIES
	/** Logging object */
	private final Log log = LogFactory.getLog(getClass());
	/** Instance of this DAO implementation we are testing. */
	private MessageDao dao;
	/** Keyword DAO */
	private KeywordDao keywordDao;
	
//> TEST METHODS
	public void testGetMessagesForPhoneNumberList_empty() {
		checkSanity();
		createOutgoingMessages(ARTHUR,
				"hey buddy, you watching the game tonite?",
				"fabulous, u bet i will");
		createIncomingMessages(ARTHUR,
				"u mean the breakerz? meet me down caesars");
		
		createOutgoingMessages(BERNADETTE,
				"hello",
				"nice to see you on tuesday",
				"wanna go see iron man 2 tomorrow?",
				"please???");
		createIncomingMessages(BERNADETTE,
				"heya",
				"yeah you too, what a night ;)",
				"Loser.");

		testGetMessagesForPhoneNumberList_populated(0, Type.ALL);
		testGetMessagesForPhoneNumberList_populated(0, Type.RECEIVED);
		testGetMessagesForPhoneNumberList_populated(0, Type.OUTBOUND);
		
	}
	
	public void testGetMessagesForPhoneNumberList_populated() {
		checkSanity();
		createOutgoingMessages(ARTHUR,
				"hey buddy, you watching the game tonite?",
				"fabulous, u bet i will");
		createIncomingMessages(ARTHUR,
				"u mean the breakerz? meet me down caesars");
		
		createOutgoingMessages(BERNADETTE,
				"hello",
				"nice to see you on tuesday",
				"wanna go see iron man 2 tomorrow?",
				"please???");
		createIncomingMessages(BERNADETTE,
				"heya",
				"yeah you too, what a night ;)",
				"Loser.");

		testGetMessagesForPhoneNumberList_populated(3, Type.ALL, ARTHUR);
		testGetMessagesForPhoneNumberList_populated(7, Type.ALL, BERNADETTE);
		testGetMessagesForPhoneNumberList_populated(10, Type.ALL, ARTHUR, BERNADETTE);

		testGetMessagesForPhoneNumberList_populated(2, Type.OUTBOUND, ARTHUR);
		testGetMessagesForPhoneNumberList_populated(4, Type.OUTBOUND, BERNADETTE);
		testGetMessagesForPhoneNumberList_populated(6, Type.OUTBOUND, ARTHUR, BERNADETTE);

		testGetMessagesForPhoneNumberList_populated(1, Type.RECEIVED, ARTHUR);
		testGetMessagesForPhoneNumberList_populated(3, Type.RECEIVED, BERNADETTE);
		testGetMessagesForPhoneNumberList_populated(4, Type.RECEIVED, ARTHUR, BERNADETTE);
	}
	
	private void testGetMessagesForPhoneNumberList_populated(int expectedCount, Type messageType, String... phoneNumbers) {
		assertEquals(expectedCount, 
					dao.getMessages(messageType, Arrays.asList(phoneNumbers), FrontlineMessage.Field.DATE, Order.ASCENDING, null, null, 0, 100).size());
	}
	
	public void testGetMessageForStatusUpdate_noMessage() {
		checkSanity();
		assertNull(dao.getMessageForStatusUpdate("3456789654345678987654", 0));
	}
	
	public void testGetMessageForStatusUpdate_multipleMessages() {
		checkSanity();

		FrontlineMessage oldMessage1 = FrontlineMessage.createOutgoingMessage(4000, ARTHUR, BERNADETTE, "asdf");
		oldMessage1.setSmscReference(123);
		dao.saveMessage(oldMessage1);
		
		FrontlineMessage recentMessage = FrontlineMessage.createOutgoingMessage(5000, ARTHUR, BERNADETTE, "asdf");
		recentMessage.setSmscReference(123);
		dao.saveMessage(recentMessage);
		
		FrontlineMessage oldMessage2 = FrontlineMessage.createOutgoingMessage(4000, ARTHUR, BERNADETTE, "asdf");
		oldMessage2.setSmscReference(123);
		dao.saveMessage(oldMessage2);
		
		assertEquals(recentMessage, dao.getMessageForStatusUpdate(BERNADETTE, 123));
	}
	
	public void testGetMessageForStatusUpdate_onlyReceivedMessages() {
		FrontlineMessage irrelevantMessage = FrontlineMessage.createIncomingMessage(1000, ARTHUR, BERNADETTE, "hello");
		irrelevantMessage.setSmscReference(123);
		dao.saveMessage(irrelevantMessage);
		
		assertNull(dao.getMessageForStatusUpdate(BERNADETTE, 123));
	}

	/**
	 * Test everything all at once!
	 */
	public void testSaveDeleteSimple() {
		checkSanity();
		
		long startTime = System.currentTimeMillis();
		FrontlineMessage m = FrontlineMessage.createIncomingMessage(startTime + 1000, ARTHUR, BERNADETTE, "Hello mate.");
		dao.saveMessage(m);
	
		checkSanity();
		assertEquals(1, dao.getSMSCount(0l, Long.MAX_VALUE));
		assertEquals(1, dao.getSMSCountForMsisdn(ARTHUR, 0l, Long.MAX_VALUE));
		assertEquals(1, dao.getSMSCountForMsisdn(BERNADETTE, 0l, Long.MAX_VALUE));
		assertEquals(0, dao.getSMSCountForMsisdn("whatever i am invented", 0l, Long.MAX_VALUE));
		assertEquals(0, dao.getMessageCount(Type.OUTBOUND, 0l, Long.MAX_VALUE));
		assertEquals(1, dao.getMessageCount(Type.RECEIVED, 0l, Long.MAX_VALUE));
		
		dao.deleteMessage(m);

		checkSanity();
		assertEquals(0, dao.getSMSCount(startTime, Long.MAX_VALUE));
	}
	
	public void testGetSimilarKeywords() throws DuplicateKeyException {
		// Create a number of keywords and messages, and perform queries over them
		createKeywords("", "te", "test", "test complex", "test other complex", "test complex again", "distraction", "another distraction");
		testGetSimilarKeywords("", "te", "test", "test complex", "test other complex", "test complex again", "distraction", "another distraction");
		testGetSimilarKeywords("te");
		testGetSimilarKeywords("test", "test complex", "test other complex", "test complex again");
		testGetSimilarKeywords("test complex", "test complex again");
		testGetSimilarKeywords("test other complex");
		testGetSimilarKeywords("test complex again");
		testGetSimilarKeywords("distraction");
		testGetSimilarKeywords("another distraction");
	}
	
	private void testGetSimilarKeywords(String keyword, String... expectedMatches) {
		HibernateMessageDao dao = (HibernateMessageDao) this.dao;
		
		// Convert expectedMathches to upper case
		for (int i = 0; i < expectedMatches.length; i++) {
			expectedMatches[i] = expectedMatches[i].toUpperCase();
		}
		
		List<String> actualMatches = dao.getSimilarKeywords(new Keyword(keyword, "test keyword: trying to get similar."));
		assertEquals("Unexpected results for keyword '" + keyword + "'",
				expectedMatches.length, actualMatches.size());
		
		assertEqualsIgnoreOrder(keyword, expectedMatches, actualMatches);
	}
	
	public void testGetMessagesForKeywordWithParameters() throws DuplicateKeyException {
		createKeywords("", "test", "test complex", "distraction");
		createMessagesWithParameters("", "test", "test complex", "distraction");
		testGetMessagesForKeywordWithParameters("", "test", "test complex", "distraction");
	}
	
	private void testGetMessagesForKeywordWithParameters(String... keywords) {
		for(String keyword : keywords) {
			// Test sorting messages by date ASCENDING
			// 1. get all messages
			testGetMessagesForKeywordWithParameters(keyword, 20, Long.MIN_VALUE, Long.MAX_VALUE);
			testGetMessagesForKeywordWithParameters(keyword, 20, null, null);
			testGetMessagesForKeywordWithParameters(keyword, 20, Long.MIN_VALUE, null);
			testGetMessagesForKeywordWithParameters(keyword, 20, null, Long.MAX_VALUE);
			testGetMessagesForKeywordWithParameters(keyword, 20, DATE_1970, DATE_2010);
			
			// 2. get ranges of messages
			testGetMessagesForKeywordWithParameters(keyword, 12, Long.MIN_VALUE, DATE_1990);
			testGetMessagesForKeywordWithParameters(keyword, 12, null, DATE_1990);
			testGetMessagesForKeywordWithParameters(keyword, 8, DATE_2000, Long.MAX_VALUE);
			testGetMessagesForKeywordWithParameters(keyword, 8, DATE_2000, null);
			testGetMessagesForKeywordWithParameters(keyword, 8, DATE_1980, DATE_1990);
		}
	}
	
	private void testGetMessagesForKeywordWithParameters(String keyword, int totalMessageCount, Long startDate, Long endDate) {
		testGetMessagesForKeywordWithParameters(keyword, totalMessageCount, startDate, endDate, 0, 100);
		testGetMessagesForKeywordWithParameters(keyword, totalMessageCount, startDate, endDate, 0, totalMessageCount);
		testGetMessagesForKeywordWithParameters(keyword, totalMessageCount, startDate, endDate, 0, totalMessageCount/2);
		testGetMessagesForKeywordWithParameters(keyword, totalMessageCount, startDate, endDate, 1, 2);
		testGetMessagesForKeywordWithParameters(keyword, totalMessageCount, startDate, endDate, totalMessageCount, 100);
	}
	
	private void testGetMessagesForKeywordWithParameters(String keyword, int totalMessageCount, Long startDate, Long endDate, int startIndex, int limit) {
		int actualMessageCount = this.dao.getMessageCount(FrontlineMessage.Type.ALL, new Keyword(keyword, ""), startDate, endDate);
		assertTrue("Wrong message count. Expected <" + totalMessageCount + ">, but was <" + actualMessageCount + ">", totalMessageCount == actualMessageCount);
		
		// Adjust the expected message count to take into account the paging
		int expectedMessageCount = Math.min(totalMessageCount-startIndex, limit);
		
		// Test ascending
		{
			List<FrontlineMessage> dateAscMessages = this.dao.getMessagesForKeyword(FrontlineMessage.Type.ALL, new Keyword(keyword, ""),
					FrontlineMessage.Field.DATE, Order.ASCENDING, startDate, endDate, startIndex, limit);
			assertEquals("Messages for keyword '" + keyword + "' " +
					"start=" + startIndex + ";limit=" + limit,
					expectedMessageCount, dateAscMessages.size());
			
			Long lastDate = null;
			if (startDate != null) {
				lastDate = startDate;
			}
			
			for(FrontlineMessage m : dateAscMessages) {
				if (lastDate == null) {
					lastDate = m.getDate();
				}
				assertTrue(m.getTextContent().startsWith(keyword));
				assertTrue(lastDate + " is supposed to be lower than " + m.getDate(), lastDate <= m.getDate());
				lastDate = m.getDate();
			}
		}
			
		// Test descending
		{
			List<FrontlineMessage> dateDescMessages = this.dao.getMessagesForKeyword(FrontlineMessage.Type.ALL, new Keyword(keyword, ""),
					FrontlineMessage.Field.DATE, Order.DESCENDING, startDate, endDate, startIndex, limit);
			assertEquals("Messages for keyword '" + keyword + "' " +
					"start=" + startIndex + ";limit=" + limit,
					expectedMessageCount, dateDescMessages.size());
			Long lastDate = null;
			if (endDate != null) {
				lastDate = endDate;
			}
			
			for(FrontlineMessage m : dateDescMessages) {
				if (lastDate == null) {
					lastDate = m.getDate();
				}
				assertTrue(m.getTextContent().startsWith(keyword));
				assertTrue(lastDate + " is supposed to be greater than " + m.getDate(), lastDate >= m.getDate());
				lastDate = m.getDate();
			}
		}
		System.out.println();
	}
	
	private void createMessagesWithParameters(String... keywords) {
		for(String keyword : keywords) {
			String prefix = keyword.length() > 0 ? keyword + " " : "";
			
			createMessages(prefix + "A 1970", DATE_1970);
			createMessages(prefix + "B 1970", DATE_1970);
			createMessages(prefix + "A 1980", DATE_1980);
			createMessages(prefix + "B 1980", DATE_1980);
			createMessages(prefix + "A 1990", DATE_1990);
			createMessages(prefix + "B 1990", DATE_1990);
			createMessages(prefix + "A 2000", DATE_2000);
			createMessages(prefix + "B 2000", DATE_2000);
			createMessages(prefix + "A 2010", DATE_2010);
			createMessages(prefix + "B 2010", DATE_2010);
		}
	}
	
	/**
	 * Create an incoming and outgoing message sent in a particular year.
	 * @param textContent
	 * @param date
	 */
	private void createMessages(String messageContent, long date) {
		final String senderMsisdn = "test sender";
		final String recipientMsisdn = "test recipient";
		
		FrontlineMessage incomingMessage = FrontlineMessage.createIncomingMessage(date, senderMsisdn, recipientMsisdn, messageContent);
		this.dao.saveMessage(incomingMessage);
		
		FrontlineMessage outgoingMessage = FrontlineMessage.createOutgoingMessage(date, senderMsisdn, recipientMsisdn, messageContent);
		this.dao.saveMessage(outgoingMessage);
	}
	
	/**
	 * Test {@link MessageDao#getMessagesForKeyword(int, net.frontlinesms.data.domain.Keyword)}.
	 * @throws DuplicateKeyException 
	 */
	public void testGetMessagesForKeyword() throws DuplicateKeyException {
		// Create a number of keywords and messages, and perform queries over them
		createKeywords("", "te", "test", "test complex", "test other complex", "test complex again", "distraction", "another distraction");
		
		createMessages(
				"",															// -> ""
				"te",														// -> "te"
				"test",														// -> "test"
				"test complex",												// -> "test complex"
				"Here is a message that should show as blank.",				// -> ""
				"Test the test keyword with this message",					// -> "test"
				"Test Complex keyword behaviour with this message",			// -> "test complex"
				"test test test",											// -> "test"
				"don't test me"												// -> ""
				);

		testGetMessagesForKeyword("", 3);
		testGetMessagesForKeyword("te", 1);
		testGetMessagesForKeyword("test", 3);
		testGetMessagesForKeyword("test complex", 2);
		testGetMessagesForKeyword("test other complex", 0);
		testGetMessagesForKeyword("distraction", 0);
		testGetMessagesForKeyword("another distraction", 0);
	}
	
	/**
	 * Test individual values for {@link #testGetMessagesForKeyword()}
	 * @param keywordString the keyword string to match
	 * @param expectedMessageCount the expected number of incoming and outgoing messages.  Total messages should be twice this.
	 */
	private void testGetMessagesForKeyword(String keywordString, int expectedMessageCount) {
		Keyword keyword = new Keyword(keywordString, "Test keyword.");
		List<FrontlineMessage> allMessagesForBlankKeyword = dao.getMessagesForKeyword(FrontlineMessage.Type.ALL, keyword);
		List<FrontlineMessage> incomingMessagesForBlankKeyword = dao.getMessagesForKeyword(FrontlineMessage.Type.RECEIVED, keyword);
		List<FrontlineMessage> outgoingMessagesForBlankKeyword = dao.getMessagesForKeyword(FrontlineMessage.Type.OUTBOUND, keyword);
		
		int allMessageCount = allMessagesForBlankKeyword.size();
		int incomingMessageCount = incomingMessagesForBlankKeyword.size();
		int outgoingMessageCount = outgoingMessagesForBlankKeyword.size();
		assertTrue("Message count mismatch for keyword: '" + keywordString + "'", incomingMessageCount == outgoingMessageCount);
		assertTrue("Message count mismatch for keyword: '" + keywordString + "'", allMessageCount == 2 * incomingMessageCount);
		assertEquals("Unexpected message count for keyword: '" + keywordString + "'", expectedMessageCount, incomingMessageCount);	
	}
	
	/** TODO may not be necessary to create keywords. */
	private void createKeywords(String... keywordStrings) throws DuplicateKeyException {
		for(String keywordString : keywordStrings) {
			createKeyword(keywordString);
		}
	}
	
	private void createKeyword(String keywordString) throws DuplicateKeyException {
		Keyword k = new Keyword(keywordString, "generated for test in " + this.getClass().getName());
		this.keywordDao.saveKeyword(k);
	}
	
	private void createIncomingMessages(String from, String... messageContents) {
		for(String c : messageContents) {
			dao.saveMessage(FrontlineMessage.createIncomingMessage(0, from, "000", c));
		}
	}
	
	private void createOutgoingMessages(String to, String... messageContents) {
		for(String c : messageContents) {
			dao.saveMessage(FrontlineMessage.createOutgoingMessage(0, "000", to, c));
		}
	}
	
	private void createMessages(String... messageContents) {
		for(String messageContent : messageContents) {
			createIncomingMessage(messageContent);
			createOutgoingMessage(messageContent);
		}
	}

	private void createOutgoingMessage(String messageContent) {
		FrontlineMessage m = FrontlineMessage.createOutgoingMessage(0, "testSender", "testRecipient", messageContent);
		this.dao.saveMessage(m);
	}

	private void createIncomingMessage(String messageContent) {
		FrontlineMessage m = FrontlineMessage.createIncomingMessage(0, "testSender", "testRecipient", messageContent);
		this.dao.saveMessage(m);
	}
	
	public void testMultimediaMessageRetrieval() {
		// Text message
		{
			FrontlineMultimediaMessage mms = new FrontlineMultimediaMessage(
					FrontlineMessage.Type.RECEIVED, "Subject 1", "summary here", Arrays.asList(new FrontlineMultimediaMessagePart[]{
							FrontlineMultimediaMessagePart.createTextPart("Hullo")
					}));
			this.dao.saveMessage(mms);
			
			List<FrontlineMessage> messages = this.dao.getAllMessages();
			assertEquals(1, messages.size());
			
			FrontlineMessage actualMessage = messages.get(0);
			assertEquals(FrontlineMultimediaMessage.class, actualMessage.getClass());
			assertEquals(1, (((FrontlineMultimediaMessage) actualMessage).getMultimediaParts().size()));
		}
		
		// Binary message
		{
			FrontlineMultimediaMessage mms = new FrontlineMultimediaMessage(
					FrontlineMessage.Type.RECEIVED, "Subject 1", "summary here", Arrays.asList(new FrontlineMultimediaMessagePart[]{
							FrontlineMultimediaMessagePart.createBinaryPart("/somewhere/something.wot")
					}));
			this.dao.saveMessage(mms);
			
			List<FrontlineMessage> messages = this.dao.getAllMessages();
			assertEquals(2, messages.size());
			
			FrontlineMessage actualMessage = messages.get(1);
			assertEquals(FrontlineMultimediaMessage.class, actualMessage.getClass());
			assertEquals(1, (((FrontlineMultimediaMessage) actualMessage).getMultimediaParts().size()));
		}
		
		// Mixed message
		{
			FrontlineMultimediaMessage mms = new FrontlineMultimediaMessage(
					FrontlineMessage.Type.RECEIVED, "Subject 1", "summary here", Arrays.asList(new FrontlineMultimediaMessagePart[]{
							FrontlineMultimediaMessagePart.createTextPart("another message"),
							FrontlineMultimediaMessagePart.createBinaryPart("/somewhereElse/somethingElse.who"),
							FrontlineMultimediaMessagePart.createTextPart("The End."),
					}));
			this.dao.saveMessage(mms);
			
			List<FrontlineMessage> messages = this.dao.getAllMessages();
			assertEquals(3, messages.size());
			
			FrontlineMessage actualMessage = messages.get(2);
			assertEquals(FrontlineMultimediaMessage.class, actualMessage.getClass());
			assertEquals(3, (((FrontlineMultimediaMessage) actualMessage).getMultimediaParts().size()));
		}
		
		// Delete messages
	}

//> INSTANCE HELPER METHODS
	/**
	 * Check that various methods agree with each other.
	 */
	private void checkSanity() {
		assertEquals(dao.getSMSCount(0l, Long.MAX_VALUE), dao.getAllMessages().size());
	}
//> ACCESSORS
	/** @param d The DAO to use for the test. */
	@Required
	public void setMessageDao(MessageDao d) {
		this.dao = d;
	}
	
	@Required
	public void setKeywordDao(KeywordDao keywordDao) {
		this.keywordDao = keywordDao;
	}
	
//> STATIC HELPER METHODS
	/** Create a date in millis */
	private static long createDate(int year) {
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		c.setTimeInMillis(0);
		c.set(Calendar.YEAR, year);
		return c.getTimeInMillis();
	}
	
	static final <T> void assertEqualsIgnoreOrder(String keyword, T[] expected, List<T> actual) {
		List<T> tempActual = new ArrayList<T>(actual);
		assertEquals("Incorrect object count for " + keyword, expected.length, actual.size());
		for (int i = 0; i < expected.length; i++) {
			assertTrue("Unexpected keyword match for '" + keyword + "' at index: " + i, tempActual.remove(expected[i]));
		}
	}
}
