package jp.ats.platemail.mail;

import java.util.List;

/**
 * メールの全情報のうち、使用頻度の高い重要な情報を持つことを表す
 */
public interface MessageDigest {

	byte[] getRawMessage();

	List<HeaderElement> getHeaderElements();

	List<Recipient> getRecipients();
}
