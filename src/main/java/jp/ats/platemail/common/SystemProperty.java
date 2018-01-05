package jp.ats.platemail.common;

public class SystemProperty {

	public static void set() {
		System.setProperty("sun.nio.cs.map", "x-windows-iso2022jp/ISO-2022-JP");
		System.setProperty("mail.mime.base64.ignoreerrors", "true");
		System.setProperty("mail.mime.ignoreunknownencoding", "true");
		System.setProperty("mail.mime.decodetext.strict", "false");
		System.setProperty("mail.mime.address.strict", "false");
		System.setProperty("mail.mime.parameters.strict", "false");
	}
}
