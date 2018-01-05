package jp.ats.platemail.account;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import jp.ats.platemail.account.filter.Target;
import jp.ats.platemail.account.filter.Test;
import jp.ats.platemail.common.U;

/**
 * 条件情報
 */
public class Condition {

	/**
	 * 対象ヘッダ
	 */
	@Expose
	private Target target;

	/**
	 * 任意入力ヘッダ名
	 */
	@Expose
	private String wildcard;

	/**
	 * 条件文字列
	 */
	@Expose
	private String condition;

	/**
	 * 検査オペレータ
	 */
	@Expose
	private Test test;

	/**
	 * JSONから復元
	 * @param json
	 * @return instance
	 */
	public static Condition getInstance(String json) {
		return new Gson().fromJson(json, Condition.class);
	}

	public Target getTarget() {
		return target;
	}

	public void setTarget(Target target) {
		this.target = target;
	}

	public String getWildcard() {
		return wildcard;
	}

	public void setWildcard(String wildcard) {
		this.wildcard = wildcard;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public Test getTest() {
		return test;
	}

	public void setTest(Test test) {
		this.test = test;
	}

	/**
	 * 文章化された条件を返す
	 * @return 文章化された条件
	 */
	public String getSentence() {
		return test.buildSentence(target, wildcard, condition);
	}

	@Override
	public String toString() {
		return getSentence();
	}

	public void trimStringValues() {
		wildcard = U.removeWhiteSpaces(wildcard);
		condition = U.removeWhiteSpaces(condition);
	}
}
