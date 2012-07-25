/**
 * 
 */
package net.frontlinesms.messaging;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.mockito.Mock;
import org.smslib.CIncomingMessage;

import net.frontlinesms.FrontlineSMS;
import net.frontlinesms.data.domain.Contact;
import net.frontlinesms.data.domain.Group;
import net.frontlinesms.data.domain.Keyword;
import net.frontlinesms.data.domain.KeywordAction;
import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.data.repository.ContactDao;
import net.frontlinesms.data.repository.KeywordActionDao;
import net.frontlinesms.data.repository.KeywordDao;
import net.frontlinesms.data.repository.MessageDao;
import net.frontlinesms.junit.BaseTestCase;
import net.frontlinesms.listener.UIListener;
import net.frontlinesms.messaging.IncomingMessageProcessor;
import net.frontlinesms.messaging.sms.SmsService;

/**
 * Tests for the {@link IncomingMessageProcessor} class.
 * @author Alex Anderson <alex@frontlinesms.com>
 */
public class IncomingMessageProcessorTest extends BaseTestCase {
	/** A phone number used for initialising {@link CIncomingMessage}s */
	private static final String TEST_ORIGINATOR = "+123456789";
	@Mock private FrontlineSMS frontline;
	@Mock private ContactDao contactDao;
	@Mock private MessageDao messageDao;
	@Mock private KeywordDao keywordDao;
	@Mock private KeywordActionDao keywordActionDao;
	
	private IncomingMessageProcessor imp;
	private BlockingIncomingMessageEventListener bimel;
	
//> TEST META METHODS
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	
		// Set up the Frontline controller
		when(frontline.getContactDao()).thenReturn(contactDao);
		when(frontline.getMessageDao()).thenReturn(messageDao);
		when(frontline.getKeywordDao()).thenReturn(keywordDao);
		when(frontline.getKeywordActionDao()).thenReturn(keywordActionDao);
		
		imp = new IncomingMessageProcessor(frontline);
		bimel = new BlockingIncomingMessageEventListener();
		imp.setUiListener(bimel);
		imp.start();
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		
		imp.die();
		imp = null;
		frontline = null;
	}

//> TESTS
	public void testActionProcessing() {
		FrontlineMessage mockMessage = mock(FrontlineMessage.class);
		Keyword mockKeyword = mock(Keyword.class);
		
		when(keywordDao.getFromMessageText(anyString())).thenReturn(mockKeyword);

		UIListener uiListener = mock(UIListener.class);
		imp.setUiListener(uiListener);

		KeywordAction goodAction1 = mockKeywordAction(true);
		KeywordAction goodAction2 = mockKeywordAction(true);
		KeywordAction deadAction = mockKeywordAction(false);
		
		KeywordAction badAction = mockKeywordAction(true);
		when(badAction.getType()).thenReturn(KeywordAction.Type.REPLY);
		
		when(keywordActionDao.getActions(mockKeyword)).thenReturn(Arrays.asList(goodAction1, deadAction, badAction, goodAction2));
		
		imp.handleMessage(mockMessage);
		verify(keywordActionDao).incrementCounter(goodAction1);
		verify(uiListener).keywordActionExecuted(goodAction1);
		
		verify(keywordActionDao).incrementCounter(goodAction2);
		verify(uiListener).keywordActionExecuted(goodAction2);
		
		verify(keywordActionDao, never()).incrementCounter(deadAction);
		verify(uiListener, never()).keywordActionExecuted(deadAction);
		
		verify(keywordActionDao, never()).incrementCounter(badAction);
		verify(uiListener, never()).keywordActionExecuted(badAction);
	}
	
	private KeywordAction mockKeywordAction(boolean isAlive) {
		KeywordAction action = mock(KeywordAction.class);
		when(action.isAlive(anyLong())).thenReturn(isAlive);
		when(action.getType()).thenReturn(KeywordAction.Type.NO_ACTION);
		return action;
	}
	
	/**
	 * Verify that new message objects are created and saved for messages which have no
	 * keywords linked to them.
	 */
	public void testSimpleTextMessages() {
		testTextMessage("");
		testTextMessage("A short message.");
		testTextMessage("\ni have a newline\nin me and one at the start.");
	}
	
	/**
	 * Test the basic processing of a text message, including whether it is persisted, that keyword matching is
	 * attempted, and that the created {@link FrontlineMessage} object contained the expected text.
	 * @param messageText
	 * @return
	 */
	private FrontlineMessage testTextMessage(String messageText) {
		// Create and queue the message
		CIncomingMessage message = new CIncomingMessage(TEST_ORIGINATOR, messageText);
		
		FrontlineMessage mess = testMessageReceive(message);
		verify(keywordDao).getFromMessageText(messageText);
		
		assertEquals("Created " + FrontlineMessage.class + " message had unexpected text content.", messageText, mess.getTextContent());
		assertNull(mess.getBinaryContent());
		
		return mess;
	}
	
	public void testSimpleBinaryMessages() {
		// Test empty message
		testBinaryMessage(new byte[0]);
		
		// Test a message with some data
		byte[] bytes1 = new byte[128];
		for(int i=0; i<bytes1.length; ++i) bytes1[i] = (byte) i;
		testBinaryMessage(bytes1);
		
		// Test a message with lots of data
		byte[] bytes2 = new byte[512];
		for(int i=0; i<bytes2.length; ++i) bytes2[i] = (byte) i;
		testBinaryMessage(bytes2);
	}
	
	/**
	 * Test the basic processing of an incoming binary message.
	 * @param data
	 * @return
	 */
	private FrontlineMessage testBinaryMessage(byte[] data) {
		CIncomingMessage message = new CIncomingMessage(TEST_ORIGINATOR, data);
		FrontlineMessage mess = testMessageReceive(message);
		assertEquals("Message contains incorrect data.", data, message.getBinary());
		return mess;
	}
	
	/**
	 * Receive a {@link CIncomingMessage}, and return the corresponding {@link FrontlineMessage} 
	 * @param message
	 * @return
	 */
	private FrontlineMessage testMessageReceive(CIncomingMessage message) {
		SmsService receiver = mock(SmsService.class);
		imp.queue(receiver, message);
		
		// Wait for the message to be processed, and then check that the expected steps were taken
		FrontlineMessage mess = bimel.getIncomingMessage();
		verify(messageDao).saveMessage(mess);
		
		return mess;
	}
}

/**
 * This class is used as the {@link UIListener} for the {@link IncomingMessageProcessor} that we are testing.
 * Using the method {@link BlockingIncomingMessageEventListener#getIncomingMessage()} we can wait until the
 * {@link IncomingMessageProcessor} has finished processing a message before checking the state of the test
 * objects.
 * @author aga
 */
class BlockingIncomingMessageEventListener implements UIListener {
	private final BlockingQueue<FrontlineMessage> incomingMessages = new LinkedBlockingQueue<FrontlineMessage>();
	
	public void contactAddedToGroup(Contact contact, Group group) { /* ignore */ }
	public void contactRemovedFromGroup(Contact contact, Group group) { /* ignore */ }
	public void keywordActionExecuted(KeywordAction action) { /* ignore */ }
	public void outgoingMessageEvent(FrontlineMessage message) { /* ignore */ }

	public void incomingMessageEvent(FrontlineMessage message) {
		incomingMessages.add(message);
	}

	/**
	 * Gets the next message from {@link #incomingMessages}, blocking until there is a message
	 * available.
	 * @return The head of {@link #incomingMessages}.
	 */
	public FrontlineMessage getIncomingMessage() {
		try {
			return incomingMessages.take();
		} catch (InterruptedException ex) {
			throw new RuntimeException("Unexpected interruption while queuing for message.", ex);
		}
	}
}