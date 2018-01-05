package jp.ats.platemail.mail;

public class HeaderElement {

	private final String header;

	private final String value;

	public HeaderElement(String header, String value) {
		this.header = header;
		this.value = value;
	}

	public String getHeader() {
		return header;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return header + ": " + value;
	}
}
