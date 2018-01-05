package jp.ats.platemail.account.filter;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.mail.internet.MimeUtility;

import jp.ats.platemail.common.U;
import jp.ats.platemail.mail.HeaderElement;

/**
 * フィルタの検査対象
 */
public enum Target {

	Cc,

	Comments,

	Content_Type,

	Content_Transfer_Encoding,

	Date,

	Delivered_To,

	Errors_To,

	From,

	In_Reply_To,

	Message_ID,

	MIME_Version,

	Organization,

	Priority,

	Received,

	Reply_To,

	Return_Path,

	Sender,

	Subject,

	To,

	X_Mailer,

	X_Priority,

	X_Sender,

	WILDCARD("直接入力");

	private final boolean usesWildcardValue;

	private final String displayName;

	private Target(String... displayName) {
		//表示名がインスタンス名から決定ではなく、指定されているということは、ヘッダ名を受け取って使用するタイプであると判断
		//簡潔に記述するために、関連の薄い値からプロパティを決定している
		this.usesWildcardValue = displayName.length > 0;
		this.displayName = usesWildcardValue ? displayName[0] : replace();
	}

	public String headerName(String wildcard) {
		return U.trim(usesWildcardValue ? wildcard : replace());
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean usesWildcardValue() {
		return usesWildcardValue;
	}

	public boolean checkWildCardValue(String wildCard) {
		boolean presents = U.presents(wildCard);
		if (usesWildcardValue) return presents;
		return !presents;
	}

	private String replace() {
		return name().replace('_', '-');
	}

	public boolean delegateTest(List<HeaderElement> elements, String wildcard, String condition, Test test) {
		String headerName = headerName(wildcard).toLowerCase();

		//ヘッダが複数あった場合、一件でも条件に合致すればtrueとする
		return elements.stream()
			.filter(element -> headerName.equals(U.trim(element.getHeader()).toLowerCase()))
			.map(Target::value)
			.filter(headerValue -> test.execute(U.trim(condition), headerValue))
			.map(headerValue -> true)
			.findFirst()
			.orElse(false);
	}

	private static String value(HeaderElement element) {
		try {
			return MimeUtility.decodeText(element.getValue());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
