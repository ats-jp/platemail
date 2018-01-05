package jp.ats.platemail.account;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddressUtils {

	private static final Pattern addressPattern = Pattern.compile("([^@]+)@([^@]+)");

	public static String[] splitMailAddress(String address) throws IllegalAddressException {
		Matcher matcher = addressPattern.matcher(address);
		if (!matcher.matches()) throw new IllegalAddressException(address);

		return new String[] { matcher.group(1), matcher.group(2) };
	}

	@SuppressWarnings("serial")
	public static class IllegalAddressException extends RuntimeException {

		private final String illegalAddress;

		public IllegalAddressException(String illegalAddress) {
			super("不正なメールアドレス: [" + illegalAddress + "]");
			this.illegalAddress = illegalAddress;
		}

		public String getIllegalAddress() {
			return illegalAddress;
		}
	}
}
