package jp.ats.platemail.mail;

import java.util.LinkedList;
import java.util.List;

import javax.mail.Message.RecipientType;

public class SimpleMessageListener implements MessageListener, MessageDigest {

	private final List<HeaderElement> headerElements = new LinkedList<>();

	private final List<Recipient> recipients = new LinkedList<>();

	private String messageId;

	private byte[] message;

	@Override
	public void notifyRecipient(RecipientType type, String personal, String address) {
		recipients.add(new Recipient(type, personal, address));
	}

	@Override
	public List<Recipient> getRecipients() {
		return recipients;
	}

	@Override
	public void notifyHeader(String header, String value) {
		HeaderElement headerElement = new HeaderElement(header, value);
		headerElements.add(headerElement);
	}

	@Override
	public List<HeaderElement> getHeaderElements() {
		return headerElements;
	}

	@Override
	public void notifyMessageId(String messageId) {
		this.messageId = messageId;
	}

	@Override
	public void notifyRawMessage(byte[] message) {
		this.message = message;
	}

	public String getMessageId() {
		return messageId;
	}

	@Override
	public byte[] getRawMessage() {
		return message;
	}

	@Override
	public PartListener createPartListener(int index) {
		return new SimplePartListener();
	}

	private class SimplePartListener implements PartListener {

		@Override
		public PartListener createMultipartListener(int multipartIndex) {
			return new SimplePartListener();
		}
	}
}
