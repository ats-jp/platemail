package jp.ats.platemail;

public class CP932 {

	/**
	 * JIS (ISO-2022-JP や EUC-JP 等) の変換テーブルを使用して CP932 に変換予定の文字列に対して、CP932 の変換テーブルに存在する不整合を訂正します。
	 *
	 * @param string CP932 に変換予定の文字列
	 * @return Unicode -> CP932 変換が可能になった文字列
	 */
	public static String treatForCP932(String string) {
		if (string == null) return null;

		char[] chars = string.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			switch (chars[i]) {
			case 0x301c:
				// WAVE DASH -> FULLWIDTH TILDE
				chars[i] = 0xff5e;
				break;

			case 0x2014:
				// EM DASH -> HORIZONTAL BAR
				chars[i] = 0x2015;
				break;

			case 0x2016:
				// DOUBLE VERTICAL LINE -> PARALLEL TO
				chars[i] = 0x2225;
				break;

			case 0x2212:
				// MINUS SIGN -> FULLWIDTH HYPHEN-MINUS
				chars[i] = 0xff0d;
				break;
			}
		}
		return new String(chars);
	}

	/**
	 * CP932 の変換テーブルを使用して JIS (ISO-2022-JP や EUC-JP 等) に変換予定の文字列に対して、CP932 の変換テーブルに存在する不整合を訂正します。
	 *
	 * @param string JIS に変換予定の文字列
	 * @return Unicode -> JIS 変換が可能になった文字列
	 */
	public static String treatForJIS(String string) {
		if (string == null) return null;

		char[] chars = string.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			switch (chars[i]) {
			case 0xff5e:
				// FULLWIDTH TILDE -> WAVE DASH
				chars[i] = 0x301c;
				break;

			case 0x2015:
				// HORIZONTAL BAR -> EM DASH
				chars[i] = 0x2014;
				break;

			case 0x2225:
				// PARALLEL TO -> DOUBLE VERTICAL LINE
				chars[i] = 0x2016;
				break;

			case 0xff0d:
				// FULLWIDTH HYPHEN-MINUS -> MINUS SIGN
				chars[i] = 0x2212;
				break;
			}
		}
		return new String(chars);
	}
}
