package jp.ats.platemail.mail;

import javax.mail.Message.RecipientType;

public class Recipient {

	private final RecipientType type;

	private final String personal;

	private final String address;

	public Recipient(RecipientType type, String personal, String address) {
		this.type = type;

		this.personal = personal;

		this.address = address;
	}

	public RecipientType getType() {
		return type;
	}

	public String getPersonal() {
		return personal;
	}

	public String getAddress() {
		return address;
	}

}
