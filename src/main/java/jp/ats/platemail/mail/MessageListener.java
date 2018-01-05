package jp.ats.platemail.mail;

import java.util.Date;

import javax.mail.Message.RecipientType;

public interface MessageListener extends Listener {

	default void notifyFrom(String personal, String address) {}

	default void notifyReplyTo(String personal, String address) {}

	default void notifyRecipient(RecipientType type, String personal, String address) {}

	default void notifySubject(String subject) {}

	default void notifySendDate(Date date) {}

	default void notifyRecievedDate(Date date) {}

	default void notifyMessageId(String messageId) {}

	default void notifyInReplyTo(String inReplyTo) {}

	default void notifyRawMessage(byte[] message) {}

	default void notifyPlainTextBody(String body) {}

	default void notifyHtmlTextBody(String body, String charset) {}

	PartListener createPartListener(int index);
}
