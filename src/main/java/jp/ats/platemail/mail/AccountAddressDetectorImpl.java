package jp.ats.platemail.mail;

public class AccountAddressDetectorImpl implements AccountAddressDetector {

	@Override
	public String detect(String originalAddress) {
		return originalAddress;
	}
}
