package jp.ats.platemail.lmtp;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.ats.platemail.U;

//エンベロープ情報（送信者と宛先）抽出
class EnvelopeInfoDetector {

	private static final String senderHeaderName = "Return-Path";

	private static final String recipientHeaderName = "X-Original-To";

	//CRLFのバイト長
	private static final int crlfLength = 2;

	private final List<String> buffer = new LinkedList<>();

	private String sender;

	private String recipient;

	private static final Pattern isFolding = Pattern.compile("^\\s+");

	private static final Pattern addressExtract = Pattern.compile("<([^>]+)>");

	void add(byte[] bytes, int length) {
		String line = new String(bytes, 0, length - crlfLength, StandardCharsets.US_ASCII);

		buffer.add(line);

		//折り畳みヘッダフィールドの場合、次行へ
		if (isFolding.matcher(line).find()) return;

		String field = String.join("", buffer);

		buffer.clear();

		String name = getHeaderName(field);

		if (sender == null && name.equalsIgnoreCase(senderHeaderName)) {
			sender = extractAddress(field);
		} else if (recipient == null && name.equalsIgnoreCase(recipientHeaderName)) {
			recipient = extractAddress(field);
		}
	}

	void checkSenderAndRecipient() {
		if (sender != null && recipient != null)
			return;

		//Postfixから受け取ったメールであれば両方必ずあるはず
		//なのでない場合は以上とみなす
		throw new IllegalStateException("sender:<" + sender + ">, recipient:<" + recipient + ">");
	}

	public String getSender() {
		return sender;
	}

	public String getRecipient() {
		return recipient;
	}

	private static String getHeaderName(String target) {
		int colonPosition = target.indexOf(':');
		if (colonPosition == -1) return "";
		return target.substring(0, colonPosition);
	}

	private static String extractAddress(String target) {
		int colonIndex = target.indexOf(':');
		if (colonIndex == -1) return null;
		String body = target.substring(colonIndex + 1);

		Matcher matcher = addressExtract.matcher(body);

		if (matcher.find()) return matcher.group(1);

		return U.removeWhiteSpaces(body);
	}
}
