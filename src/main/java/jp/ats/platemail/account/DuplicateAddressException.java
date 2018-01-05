package jp.ats.platemail.account;

@SuppressWarnings("serial")
public class DuplicateAddressException extends Exception {

	DuplicateAddressException(String mailAddress) {
		super(mailAddress);
	}
}
