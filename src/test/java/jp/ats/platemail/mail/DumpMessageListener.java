package jp.ats.platemail.mail;

import static jp.ats.platemail.mail.DevelopUtils.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import javax.mail.Message.RecipientType;

import jp.ats.platemail.mail.MessageListener;
import jp.ats.platemail.mail.PartListener;

public class DumpMessageListener implements MessageListener {

	private final Path home;

	public DumpMessageListener(Path parent, int index) {
		try {
			home = Files.createDirectory(parent.resolve("message-" + format(index)));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void notifyFrom(String personal, String address) {
		DevelopUtils.write(home.resolve("FROM"), care(personal) + "\t" + address);
	}

	@Override
	public void notifyReplyTo(String personal, String address) {
		DevelopUtils.write(home.resolve("REPLY_TO"), care(personal) + "\t" + address);
	}

	@Override
	public void notifyRecipient(RecipientType type, String personal, String address) {
		DevelopUtils.append(home.resolve(type.toString().toUpperCase()), care(personal) + "\t" + address + "\r\n");
	}

	@Override
	public void notifySubject(String subject) {
		DevelopUtils.write(home.resolve("SUBJECT"), subject);
	}

	@Override
	public void notifySendDate(Date date) {
		DevelopUtils.write(home.resolve("SEND_DATE"), date.toString());
	}

	@Override
	public void notifyRecievedDate(Date date) {
		DevelopUtils.write(home.resolve("RECIEVED_DATE"), date.toString());
	}

	@Override
	public void notifyHeader(String header, String value) {
		DevelopUtils.append(home.resolve("HEADERS"), header + "\t" + value + "\r\n");
	}

	@Override
	public void notifyRawMessage(byte[] message) {
		DevelopUtils.save(home.resolve("EML"), new ByteArrayInputStream(message));
	}

	@Override
	public PartListener createPartListener(int index) {
		return new DumpPartListener(home, index);
	}

	@Override
	public void notifyPlainTextBody(String body) {
		DevelopUtils.write(home.resolve("PLAIN_TEXT_BODY"), body);
	}

	@Override
	public void notifyHtmlTextBody(String body, String charset) {
		if (charset != null)
			DevelopUtils.write(home.resolve("PLAIN_HTML_BODY"), body, charset);
	}
}
