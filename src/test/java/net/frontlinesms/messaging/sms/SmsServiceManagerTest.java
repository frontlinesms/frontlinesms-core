/**
 * 
 */
package net.frontlinesms.messaging.sms;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.mockito.internal.verification.NoMoreInteractions;
import org.mockito.internal.verification.Times;
import org.smslib.CIncomingMessage;

import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.data.domain.FrontlineMessage.Status;
import net.frontlinesms.events.EventBus;
import net.frontlinesms.junit.BaseTestCase;
import net.frontlinesms.messaging.sms.MessageType;
import net.frontlinesms.messaging.sms.SmsServiceManager;
import net.frontlinesms.messaging.sms.events.SmsServiceStatusNotification;
import net.frontlinesms.messaging.sms.internet.SmsInternetService;
import net.frontlinesms.messaging.sms.modem.SmsModem;
import net.frontlinesms.messaging.sms.modem.SmsModemStatus;

import static org.mockito.Mockito.*;

/**
 * Test class for {@link SmsServiceManager}.
 * @author aga
 */
public class SmsServiceManagerTest extends BaseTestCase {
	/**
	 * Test that all messages sent will be given to the {@link SmsInternetService} in preference to
	 * any {@link SmsModem}s available.
	 * @throws IllegalAccessException 
	 * @throws NoSuchFieldException 
	 * @throws IllegalArgumentException 
	 * @throws SecurityException 
	 */
	public void testMessageDispatchPriorities_text() throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
		SmsServiceManager manager = new SmsServiceManager();

		SmsInternetService sisNoSend = createMockSmsInternetService(false, true);
		addSmsInternetService(manager, 1, sisNoSend);
		SmsInternetService sisNoSendNoBinary = createMockSmsInternetService(false, false);
		addSmsInternetService(manager, 2, sisNoSendNoBinary);
		SmsInternetService sisBinary = createMockSmsInternetService(true, true);
		addSmsInternetService(manager, 3, sisBinary);
		SmsInternetService sisNoBinary = createMockSmsInternetService(true, false);
		addSmsInternetService(manager, 4, sisNoBinary);
		
		SmsModem modem = createMockModem(true, true, true, true);
		addModem(manager, modem, "TestModem1");
		
		sendSms(manager, generateMessages(20, MessageType.GSM7BIT_TEXT));
		
		manager.doRun();
		
		// Check that all messages were sent with the TWO functioning internet services, and nothing else
		verify(modem, never()).sendSMS(any(FrontlineMessage.class));
		verify(sisNoSend, never()).sendSMS(any(FrontlineMessage.class));
		verify(sisNoSendNoBinary, never()).sendSMS(any(FrontlineMessage.class));
		verify(sisBinary, times(10)).sendSMS(any(FrontlineMessage.class));
		verify(sisNoBinary, times(10)).sendSMS(any(FrontlineMessage.class));
	}
	
	/**
	 * Test that all messages sent will be given to the {@link SmsInternetService} in preference to
	 * any {@link SmsModem}s available.
	 * @throws IllegalAccessException 
	 * @throws NoSuchFieldException 
	 * @throws IllegalArgumentException 
	 * @throws SecurityException 
	 */
	public void testMessageDispatchPriorities_binary() throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
		SmsServiceManager manager = new SmsServiceManager();

		SmsInternetService sisNoSend = createMockSmsInternetService(false, true);
		addSmsInternetService(manager, 1, sisNoSend);
		SmsInternetService sisNoSendNoBinary = createMockSmsInternetService(false, false);
		addSmsInternetService(manager, 2, sisNoSendNoBinary);
		SmsInternetService sisBinary = createMockSmsInternetService(true, true);
		addSmsInternetService(manager, 3, sisBinary);
		SmsInternetService sisNoBinary = createMockSmsInternetService(true, false);
		addSmsInternetService(manager, 4, sisNoBinary);
		
		SmsModem modem = createMockModem(true, true, true, true);
		addModem(manager, modem, "TestModem1");
		
		sendSms(manager, generateMessages(20, MessageType.BINARY));
		
		manager.doRun();
		
		// Check that all messages were sent with the ONE internet services which is functioning and sends binary, and nothing else
		verify(modem, never()).sendSMS(any(FrontlineMessage.class));
		verify(sisNoSend, never()).sendSMS(any(FrontlineMessage.class));
		verify(sisNoSendNoBinary, never()).sendSMS(any(FrontlineMessage.class));
		verify(sisNoBinary, never()).sendSMS(any(FrontlineMessage.class));
		verify(sisBinary, times(20)).sendSMS(any(FrontlineMessage.class));
	}
	
	@SuppressWarnings("unchecked")
	private void addSmsInternetService(SmsServiceManager manager, long id,
			SmsInternetService service) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field f = manager.getClass().getDeclaredField("smsInternetServices");
		f.setAccessible(true);
		((Map<Long, SmsInternetService>) f.get(manager)).put(id, service);
	}

	/** Test that text messages are sent only with suitable modems. */
	public void testModemSend_text() {
		SmsServiceManager manager = new SmsServiceManager();
		
		SmsModem disconnectedModem = createMockModem(false, false, true, true);
		addModem(manager, disconnectedModem, "Disconnected.");
		SmsModem gsmOnlyModem = createMockModem(true, true, false, false);
		addModem(manager, gsmOnlyModem, "GsmOnly");
		SmsModem ucs2Modem = createMockModem(true, true, false, true);
		addModem(manager, ucs2Modem, "ucs2");
		SmsModem binaryModem = createMockModem(true, true, true, false);
		addModem(manager, binaryModem, "binary");
		SmsModem everythingModem = createMockModem(true, true, true, true);
		addModem(manager, everythingModem, "everything");
		
		// Sending no messages
		manager.doRun();
		verify(disconnectedModem, never()).sendSMS(any(FrontlineMessage.class));
		verify(gsmOnlyModem, never()).sendSMS(any(FrontlineMessage.class));
		verify(ucs2Modem, never()).sendSMS(any(FrontlineMessage.class));
		verify(binaryModem, never()).sendSMS(any(FrontlineMessage.class));
		verify(everythingModem, never()).sendSMS(any(FrontlineMessage.class));

		// Send some simple text messages, and make sure that they were send with the expected modems
		Collection<FrontlineMessage> gsm7bitMessages = generateMessages(8, MessageType.GSM7BIT_TEXT);
		sendSms(manager, gsm7bitMessages);
		manager.doRun();
		verify(disconnectedModem, never()).sendSMS(any(FrontlineMessage.class));
		verify(gsmOnlyModem, times(2)).sendSMS(any(FrontlineMessage.class));
		verify(ucs2Modem, times(2)).sendSMS(any(FrontlineMessage.class));
		verify(binaryModem, times(2)).sendSMS(any(FrontlineMessage.class));
		verify(everythingModem, times(2)).sendSMS(any(FrontlineMessage.class));
	}
	
	/** Test that binary messages are sent only with suitable modems. */
	public void testModemSend_binary() {
		SmsServiceManager manager = new SmsServiceManager();
		
		SmsModem disconnectedModem = createMockModem(false, false, true, true);
		addModem(manager, disconnectedModem, "Disconnected.");
		SmsModem gsmOnlyModem = createMockModem(true, true, false, false);
		addModem(manager, gsmOnlyModem, "GsmOnly");
		SmsModem ucs2Modem = createMockModem(true, true, false, true);
		addModem(manager, ucs2Modem, "ucs2");
		SmsModem binaryModem = createMockModem(true, true, true, false);
		addModem(manager, binaryModem, "binary");
		SmsModem everythingModem = createMockModem(true, true, true, true);
		addModem(manager, everythingModem, "everything");
		
		// Send some binary messages
		Collection<FrontlineMessage> binaryMessages = generateMessages(8, MessageType.BINARY);
		sendSms(manager, binaryMessages);
		manager.doRun();
		verify(disconnectedModem, never()).sendSMS(any(FrontlineMessage.class));
		verify(gsmOnlyModem, never()).sendSMS(any(FrontlineMessage.class));
		verify(ucs2Modem, never()).sendSMS(any(FrontlineMessage.class));
		verify(binaryModem, times(4)).sendSMS(any(FrontlineMessage.class));
		verify(everythingModem, times(4)).sendSMS(any(FrontlineMessage.class));
	}
	
	/** Test that binary messages are sent only with suitable modems. */
	public void testModemSend_ucs2() {
		SmsServiceManager manager = new SmsServiceManager();
		
		SmsModem disconnectedModem = createMockModem(false, false, true, true);
		addModem(manager, disconnectedModem, "Disconnected.");
		SmsModem gsmOnlyModem = createMockModem(true, true, false, false);
		addModem(manager, gsmOnlyModem, "GsmOnly");
		SmsModem ucs2Modem = createMockModem(true, true, false, true);
		addModem(manager, ucs2Modem, "ucs2");
		SmsModem binaryModem = createMockModem(true, true, true, false);
		addModem(manager, binaryModem, "binary");
		SmsModem everythingModem = createMockModem(true, true, true, true);
		addModem(manager, everythingModem, "everything");
		
		// Send some UCS2 messages
		Collection<FrontlineMessage> ucs2Messages = generateMessages(8, MessageType.UCS2_TEXT);
		sendSms(manager, ucs2Messages);
		manager.doRun();
		verify(disconnectedModem, never()).sendSMS(any(FrontlineMessage.class));
		verify(gsmOnlyModem, never()).sendSMS(any(FrontlineMessage.class));
		verify(ucs2Modem, times(4)).sendSMS(any(FrontlineMessage.class));
		verify(binaryModem, never()).sendSMS(any(FrontlineMessage.class));
		verify(everythingModem, times(4)).sendSMS(any(FrontlineMessage.class));
	}
	
	/** Test that messages are polled from all modems who have message receiving enabled. */
	public void testModemMessageReceive() {
		SmsServiceManager manager = new SmsServiceManager();
		
		SmsModem[] receiveModems = new SmsModem[10];
		for (int i = 0; i < receiveModems.length; i++) {
			SmsModem modem = createMockModem(i%2==0, true, i%3==0, i%5==0);
			receiveModems[i] = modem;
			addModem(manager, modem, "Receive " + i);
		}
		SmsModem[] nonReceiveModems = new SmsModem[10];
		for (int i = 0; i < nonReceiveModems.length; i++) {
			SmsModem modem = createMockModem(i%2==0, false, i%3==0, i%5==0);
			nonReceiveModems[i] = modem;
			addModem(manager, modem, "NonReceive " + i);
		}
		
		// Now create some modems with messages
		CIncomingMessage mockMessage = mock(CIncomingMessage.class);
		
		SmsModem modemWith1Message = createMockModem(false, true, false, false);
		when(modemWith1Message.nextIncomingMessage())
				.thenReturn(mockMessage)
				.thenReturn(null);
		addModem(manager, modemWith1Message, "ModemWith1Message");
		
		SmsModem modemWith3Messages = createMockModem(false, true, false, false);
		when(modemWith3Messages.nextIncomingMessage())
				.thenReturn(mockMessage)
				.thenReturn(mockMessage)
				.thenReturn(mockMessage)
				.thenReturn(null);
		addModem(manager, modemWith3Messages, "ModemWith3Messages");

		
		manager.doRun();

		for(SmsModem modem : receiveModems) {
			verify(modem).nextIncomingMessage();
		}
		for(SmsModem modem : nonReceiveModems) {
			verify(modem, never()).nextIncomingMessage();
		}
		verify(modemWith1Message, times(2)).nextIncomingMessage();
		verify(modemWith3Messages, times(4)).nextIncomingMessage();
	}
	
	/** Tests that when there are no SMS devices, the messages are left in outbox. */
	public void testNoSmsDevices() {
		SmsServiceManager manager = new SmsServiceManager();
		FrontlineMessage m = FrontlineMessage.createOutgoingMessage(System.currentTimeMillis(), "+123456", "+987654", "Hi");
		manager.sendSMS(m);
		manager.doRun();
		
		assertEquals(Status.OUTBOX, m.getStatus());
	}
	
	public void testSmsDeviceEvent () {
		SmsServiceManager manager = new SmsServiceManager();
		
		EventBus mockEventBus = mock(EventBus.class);
		manager.setEventBus(mockEventBus);
		
		// Testing if the event is triggered with one failed-status device and a DORMANT device <SHOULD NOT>
		SmsModem modem1 = mock(SmsModem.class);
		addModem(manager, modem1, "will_fail");
		SmsModem modem2 = mock(SmsModem.class);
		addModem(manager, modem2, "will_be_owned");
		
		when(modem1.getStatus()).thenReturn(SmsModemStatus.FAILED_TO_CONNECT);
		when(modem2.getStatus()).thenReturn(SmsModemStatus.DORMANT);
		manager.smsDeviceEvent(modem1, SmsModemStatus.FAILED_TO_CONNECT);
		verify(mockEventBus, new NoMoreInteractions()).notifyObservers(any(SmsServiceStatusNotification.class));
		
		// Testing if the event is triggered with one failed-status device and a CONNECTING device <SHOULD NOT>
		when(modem2.getStatus()).thenReturn(SmsModemStatus.CONNECTING);
		manager.smsDeviceEvent(modem1, SmsModemStatus.FAILED_TO_CONNECT);
		verify(mockEventBus, new NoMoreInteractions()).notifyObservers(any(SmsServiceStatusNotification.class));
		
		// Testing if the event is triggered with the connecting device failing <SHOULD>
		when(modem2.getStatus()).thenReturn(SmsModemStatus.OWNED_BY_SOMEONE_ELSE);
		manager.smsDeviceEvent(modem2, SmsModemStatus.OWNED_BY_SOMEONE_ELSE);
		verify(mockEventBus, new Times(1)).notifyObservers(any(SmsServiceStatusNotification.class));
		
		// Testing if the event is triggered with one failed-status device and a CONNECTED device <SHOULD NOT>
		SmsModem modem3 = createMockModem(true, true, true, true);
		addModem(manager, modem3, "connected");
		manager.smsDeviceEvent(modem1, SmsModemStatus.FAILED_TO_CONNECT);
		verify(mockEventBus, new NoMoreInteractions()).notifyObservers(any(SmsServiceStatusNotification.class));
	}
	
	
//> PRIVATE HELPER METHODS
	/** @return a mock {@link SmsInternetService} with certain important methods stubbed */
	private SmsInternetService createMockSmsInternetService(boolean useForSending, boolean supportsBinary) {
		SmsInternetService mock = mock(SmsInternetService.class);
		when(mock.isConnected()).thenReturn(true);
		when(mock.isUseForSending()).thenReturn(useForSending);
		when(mock.isBinarySendingSupported()).thenReturn(supportsBinary);
		return mock;
	}

	/** @return a mock {@link SmsModem} with certain important methods stubbed */
	private SmsModem createMockModem(boolean useForSending, boolean useForReceiving, boolean supportsBinary, boolean supportsUcs2) {
		SmsModem mock = mock(SmsModem.class);
		when(mock.isConnected()).thenReturn(true);
		when(mock.isUseForSending()).thenReturn(useForSending);
		when(mock.isUseForReceiving()).thenReturn(useForReceiving);
		when(mock.isBinarySendingSupported()).thenReturn(supportsBinary);
		when(mock.isUcs2SendingSupported()).thenReturn(supportsUcs2);
		when(mock.getStatus()).thenReturn(SmsModemStatus.DORMANT);
		return mock;
	}
	
	/** Adds a {@link SmsModem} to {@link SmsServiceManager#phoneHandlers} by reflection. */
	@SuppressWarnings("unchecked")
	private void addModem(SmsServiceManager manager, SmsModem modem, String modemId) {
		try {
			Field handlerField = SmsServiceManager.class.getDeclaredField("phoneHandlers");
			handlerField.setAccessible(true);
			Map<String, SmsModem> handlers = (Map<String, SmsModem>) handlerField.get(manager);
			handlers.put(modemId, modem);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/** @return some generated SMS messages */
	private Collection<FrontlineMessage> generateMessages(int count, MessageType type) {
		HashSet<FrontlineMessage> messages = new HashSet<FrontlineMessage>();
		while(--count >= 0) {
			FrontlineMessage m;
			long now = System.currentTimeMillis();
			String recipientMsisdn = "Recipient " + count;
			String senderMsisdn = "Sender " + count;
			if(type == MessageType.BINARY) {
				byte[] data = new byte[count];
				for (int i = 0; i < data.length; i++) {
					data[i] = (byte) i;
				}
				m = FrontlineMessage.createBinaryOutgoingMessage(now, senderMsisdn, recipientMsisdn, 0, data);
			} else {
				String content = "Content " + count;
				if(type == MessageType.UCS2_TEXT) {
					// Add some random arabic letters to the text content
					content += "\u0634\u0626\u0647\u0629";
				} 
				m = FrontlineMessage.createOutgoingMessage(now, senderMsisdn, recipientMsisdn, content);
			}
			messages.add(m);
		}
		return messages;
	}

	/** Send multiple SMS to the manager */
	private void sendSms(SmsServiceManager manager, Collection<FrontlineMessage> messages) {
		for(FrontlineMessage m : messages) manager.sendSMS(m);
	}
}