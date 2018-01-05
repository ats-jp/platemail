package jp.ats.platemail.account.filter;

import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jp.ats.platemail.common.U;

/**
 * フィルタの検査部分
 */
public enum Test {

	CONTAINS("に", "が含まれる", (condition, target) -> target.contains(condition), Checker.notEmpty),

	NOT_CONTAINS("に", "が含まれない", (condition, target) -> !target.contains(condition), Checker.notEmpty),

	EQUALS("が", "と一致", (condition, target) -> target.equals(condition), Checker.notEmpty),

	NOT_EQUALS("が", "と一致しない", (condition, target) -> !target.equals(condition), Checker.notEmpty),

	STARTS_WITH("が", "で始まる", (condition, target) -> target.startsWith(condition), Checker.notEmpty),

	NOT_STARTS_WITH("が", "で始まらない", (condition, target) -> !target.startsWith(condition), Checker.notEmpty),

	ENDS_WITH("が", "で終わる", (condition, target) -> target.endsWith(condition), Checker.notEmpty),

	NOTS_END_WITH("が", "で終わらない", (condition, target) -> !target.endsWith(condition), Checker.notEmpty),

	EMPTY("が", "空白", (condition, target) -> target.isEmpty(), Checker.empty),

	NOT_EMPTY("が", "空白ではない", (condition, target) -> !target.isEmpty(), Checker.empty),

	AVAILABLE_HEADER("という", "ヘッダが存在する", (condition, target) -> target != null, Checker.empty),

	NOT_AVAILABLE_HEADER("という", "ヘッダが存在しない", (condition, target) -> target == null, Checker.empty),

	MATCHES_PATTERN("に", "がパターンマッチする", (regex, target) -> Pattern.compile(regex).matcher(target).find(), (regex) -> {
		String message = Checker.notEmpty.apply(regex);
		if (U.presents(message)) return message;

		try {
			Pattern.compile(regex);
			return "";
		} catch (PatternSyntaxException e) {
			return "パターン文字列の文法が不正です。";
		}
	});

	private final String japaneseParticle;

	private final String description;

	private final boolean usesCondition;

	private final TestFunction testFunction;

	private final Function<String, String> checkFunction;

	private Test(String japaneseParticle, String description, TestFunction testFunction, Function<String, String> checkFunction) {
		this.japaneseParticle = japaneseParticle;
		this.description = description;

		//条件文字列を使うかどうか、空文字列で判断
		usesCondition = U.presents(checkFunction.apply(""));

		this.testFunction = testFunction;
		this.checkFunction = checkFunction;
	}

	public String getDescription() {
		return description;
	}

	public String buildSentence(Target target, String wildcard, String condition) {
		if (usesCondition)
			return "「" + target.headerName(wildcard) + "」 " + japaneseParticle + " 「" + condition + "」 " + description;

		return "「" + target.headerName(wildcard) + "」 " + japaneseParticle + " " + description;
	}

	public boolean usesCondition() {
		return usesCondition;
	}

	public boolean execute(String condition, String target) {
		return testFunction.apply(condition, target);
	}

	/**
	 * 条件文字列が検査対象として使用できるかどうかをチェックする
	 * @param condition
	 * @return チェックメッセージ、空文字列の場合チェックOKということ
	 */
	public String checkCondition(String condition) {
		return checkFunction.apply(condition);
	}

	interface TestFunction {

		boolean apply(String condition, String target);
	}

	private static class Checker {

		private static final Function<String, String> notEmpty = (condition) -> U.presents(condition) ? "" : "条件文字列が未入力です。";

		private static final Function<String, String> empty = (condition) -> !U.presents(condition) ? "" : "条件文字列は入力不可です。";
	}
}
