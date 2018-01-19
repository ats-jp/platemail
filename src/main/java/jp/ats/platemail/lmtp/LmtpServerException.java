package jp.ats.platemail.lmtp;

@SuppressWarnings("serial")
public class LmtpServerException extends RuntimeException {

	private final int code;

	LmtpServerException(int code, String message) {
		super(message);
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
