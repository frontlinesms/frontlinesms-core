/**
 * 
 */
package net.frontlinesms.data.repository.hibernate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;


import net.frontlinesms.data.DuplicateKeyException;
import net.frontlinesms.data.Order;
import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.domain.FrontlineMessage.Status;
import net.frontlinesms.data.domain.FrontlineMultimediaMessage;
import net.frontlinesms.data.domain.FrontlineMultimediaMessagePart;
import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.data.domain.FrontlineMessage.Type;
import net.frontlinesms.data.repository.ContactDao;
import net.frontlinesms.data.repository.KeywordDao;
import net.frontlinesms.data.repository.MessageDao;
import net.frontlinesms.junit.HibernateTestCase;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test class for {@link HibernateMessageDao}
 * @author Alex
 */
public class HibernateMessageDaoTest extends HibernateTestCase {
//> STATIC CONSTANTS
	private static final String ARTHUR = "+44123456789";
	private static final String BERNADETTE = "+447890123456";
	private static final String TEST_MSISDN = ARTHUR;

	private static final long DATE_1970 = createDate(1970);
	private static final long DATE_1980 = createDate(1980);
	private static final long DATE_1990 = createDate(1990);
	private static final long DATE_2000 = createDate(2000);
	private static final long DATE_2010 = createDate(2010);
	
//> INSTANCE PROPERTIES
	/** Instance of this DAO implementation we are testing. */
	@Autowired private MessageDao dao;
	/** Keyword DAO */
	@Autowired private KeywordDao keywordDao;
	/** Contact DAO */
	@Autowired private ContactDao contactDao;
	
//> TEST METHODS
	public void testSaveBinaryMessageLength() {
		for(int messageLength : new int[]{0, 127, 128, 255, 256, 8*140}) {
			FrontlineMessage m = FrontlineMessage.createBinaryIncomingMessage(DATE_2000, "+1234567890", "+0987654321", 0,
					new byte[messageLength]);
			dao.saveMessage(m);
		}
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
	
	public void testSenderNameBlank() {
		// given
		assertEquals(0, dao.getAllMessages().size());
		dao.saveMessage(FrontlineMessage.createIncomingMessage(DATE_2010, "123", "987", "test"));
		setComplete();
		endTransaction();
		startNewTransaction();
		
		// when
		FrontlineMessage m = dao.getAllMessages().get(0);
		
		// then
		assertNull(m.getSenderName());
		
		// cleanup
		cleanupMessagesAndContactsAndEndTransaction();
	}
	
	public void testSenderNameSet() throws Exception {
		// given
		assertEquals(0, dao.getAllMessages().size());
		dao.saveMessage(FrontlineMessage.createIncomingMessage(DATE_2010, "123", "987", "test"));
		contactDao.saveContact(new Contact("bob", "123", null, null, null, true));
		setComplete();
		endTransaction();
		startNewTransaction();
		
		// when
		FrontlineMessage m = dao.getAllMessages().get(0);
		
		// then
		assertEquals("bob", m.getSenderName());
		
		// cleanup
		cleanupMessagesAndContactsAndEndTransaction();
	}
	
	public void testRecipientNameBlank() {
		// given
		assertEquals(0, dao.getAllMessages().size());
		dao.saveMessage(FrontlineMessage.createOutgoingMessage(DATE_2010, "123", "987", "test"));
		setComplete();
		endTransaction();
		startNewTransaction();
		
		// when
		FrontlineMessage m = dao.getAllMessages().get(0);
		
		// then
		assertNull(m.getRecipientName());
		
		// cleanup
		cleanupMessagesAndContactsAndEndTransaction();
	}
	
	public void testRecipientNameSet() throws Exception {
		// given
		assertEquals(0, dao.getAllMessages().size());
		dao.saveMessage(FrontlineMessage.createOutgoingMessage(DATE_2010, "987", "123", "test"));
		contactDao.saveContact(new Contact("bob", "123", null, null, null, true));
		setComplete();
		endTransaction();
		startNewTransaction();
		
		// when
		FrontlineMessage m = dao.getAllMessages().get(0);
		
		// then
		assertEquals("bob", m.getRecipientName());
		
		// cleanup
		cleanupMessagesAndContactsAndEndTransaction();
	}
	
	public void testGetForStatusUpdateShouldGetNullIfNoMatch() {
		// expect
		assertNull(dao.getMessageForStatusUpdate(TEST_MSISDN, 1));
	}
	
	public void testGetForStatusUpdateShouldGetMessageIfOneMatch() {
		// given
		FrontlineMessage pendingMessage = createMessageWithStatus(Status.PENDING, 1);
		
		// expect
		assertEquals(pendingMessage, dao.getMessageForStatusUpdate(TEST_MSISDN, 1));
	}
	
	public void testGetForStatusUpdateShouldGetSingleMessageIfTwoIdenticalMatches() {
		// given
		createMessageWithStatus(Status.PENDING, 1);
		createMessageWithStatus(Status.PENDING, 1);
		
		// expect
		assertNotNull(dao.getMessageForStatusUpdate(TEST_MSISDN, 1));
	}
	
	public void testGetForStatusUpdateShouldGetMostRecentMessageIfMultipleMatches() {
		// given
		createMessageWithStatus(Status.PENDING, 1, DATE_1970);
		FrontlineMessage mostRecent = createMessageWithStatus(Status.PENDING, 1, DATE_1990);
		createMessageWithStatus(Status.PENDING, 1, DATE_1980);
		
		// expect
		assertEquals(mostRecent, dao.getMessageForStatusUpdate(TEST_MSISDN, 1));
	}
	
	public void testGetForStatusUpdateShouldOnlyGetPendingMessages() {
		// given
		createMessageWithStatus(Status.FAILED, 1);
		createMessageWithStatus(Status.SENT, 2);
		FrontlineMessage pendingMessage = createMessageWithStatus(Status.PENDING, 3);
		
		// expect
		assertNull(dao.getMessageForStatusUpdate(TEST_MSISDN, 1));
		assertNull(dao.getMessageForStatusUpdate(TEST_MSISDN, 2));
		assertEquals(pendingMessage, dao.getMessageForStatusUpdate(TEST_MSISDN, 3));
	}
	
	private FrontlineMessage createMessageWithStatus(FrontlineMessage.Status status, int smscReference) {
		return createMessageWithStatus(status, smscReference, DATE_1990);
	}
	
	private FrontlineMessage createMessageWithStatus(FrontlineMessage.Status status, int smscReference, long dateReceived) {
		FrontlineMessage m = FrontlineMessage.createOutgoingMessage(dateReceived, "000", TEST_MSISDN, "some nonsense");
		m.setSmscReference(smscReference);
		m.setStatus(status);
		dao.saveMessage(m);
		return m;
	}

//> INSTANCE HELPER METHODS
	/**
	 * Check that various methods agree with each other.
	 */
	private void checkSanity() {
		assertEquals(dao.getSMSCount(0l, Long.MAX_VALUE), dao.getAllMessages().size());
	}

	private void cleanupMessagesAndContactsAndEndTransaction() {
		for(FrontlineMessage message : dao.getAllMessages()) {
			dao.deleteMessage(message);
		}
		for(Contact c : contactDao.getAllContacts()) {
			contactDao.deleteContact(c);
		}
		setComplete();
		endTransaction();
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
