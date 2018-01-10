package jp.ats.platemail.mail;

import java.io.UnsupportedEncodingException;

import javax.mail.internet.MimeUtility;

import jp.ats.platemail.U;

public class Address {

	private final String personal;

	private final String address;

	public Address(String personal, String address) {
		this.personal = personal;
		this.address = address;
	}

	public String getPersonal() {
		return personal;
	}

	public String getAddress() {
		return address;
	}

	public String getStringExpression() {
		if (!U.presents(personal)) return address;
		try {
			return MimeUtility.encodeText(personal, "UTF-8", "B") + " <" + address + ">";
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
