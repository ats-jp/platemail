package jp.ats.platemail.account;

@SuppressWarnings("serial")
public class AlreadyUpdatedException extends Exception {

	private static final String rejectMessage = "このアカウントは既に他者によって更新されています。";

	AlreadyUpdatedException() {
		super(rejectMessage);
	}
}
