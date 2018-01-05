package jp.ats.platemail.account;

import java.time.LocalDateTime;

public class AccountUtils {

	public static String now() {
		return LocalDateTime.now().toString();
	}

	public static LocalDateTime parse(String timestamp) {
		return LocalDateTime.parse(timestamp);
	}
}
