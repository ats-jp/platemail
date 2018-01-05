package jp.ats.platemail.account.filter;

import java.util.List;

import jp.ats.platemail.account.Condition;
import jp.ats.platemail.mail.MessageDigest;

/**
 * 条件同士の結合方法を定義
 */
public enum LogicalOperator {

	AND("かつ", false),

	OR("または", true);

	private final String expression;

	private final boolean factor;

	private LogicalOperator(String expression, boolean factor) {
		this.expression = expression;
		this.factor = factor;
	}

	public String express() {
		return expression;
	}

	public boolean matches(List<Condition> conditions, MessageDigest message) {
		for (Condition condition : conditions) {
			boolean result = condition.getTarget().delegateTest(
				message.getHeaderElements(),
				condition.getWildcard(),
				condition.getCondition(),
				condition.getTest());

			if (result == factor) return factor;
		}

		return !factor;
	}
}
