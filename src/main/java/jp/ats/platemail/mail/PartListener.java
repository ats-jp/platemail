package jp.ats.platemail.mail;

import java.io.IOException;
import java.io.InputStream;

public interface PartListener extends Listener {

	default void notifyContentType(String contentType) {}

	default void notifyPlainText(String text) {}

	default void notifyHtmlText(String text, String charset) {}

	default void notifyOtherText(String text) {}

	default void notifyMultipartMixed(InputStream multipart) {}

	default void notifyMultipartAlternative(InputStream multipart) {}

	default void notifyAttachment(String fileName, InputStream attachment) throws IOException {}

	PartListener createMultipartListener(int multipartIndex);
}
