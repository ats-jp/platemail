package jp.ats.platemail;

import java.util.LinkedList;
import java.util.List;

import javax.mail.Message.RecipientType;

import jp.ats.platemail.mail.HeaderElement;
import jp.ats.platemail.mail.MessageDigest;
import jp.ats.platemail.mail.MessageListener;
import jp.ats.platemail.mail.PartListener;
import jp.ats.platemail.mail.Recipient;

public class SimpleMessageDigest implements MessageListener, MessageDigest {

	private final List<HeaderElement> headerElements = new LinkedList<>();

	private final List<Recipient> recipients = new LinkedList<>();

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
	public void notifyRawMessage(byte[] message) {
		this.message = message;
	}

	@Override
	public byte[] getRawMessage() {
		return message;
	}

	@Override
	public PartListener createPartListener(int index) {
		return new MyPartListener();
	}

	private class MyPartListener implements PartListener {

		@Override
		public PartListener createMultipartListener(int multipartIndex) {
			return new MyPartListener();
		}
	}
}
