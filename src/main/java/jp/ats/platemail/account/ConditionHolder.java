package jp.ats.platemail.account;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.Expose;

import jp.ats.platemail.account.filter.LogicalOperator;

abstract class ConditionHolder {

	/**
	 * 処理方法
	 */
	@Expose
	LogicalOperator logicalOperator;

	/**
	 * 保持する条件
	 */
	@Expose
	List<Condition> conditions;

	abstract void update();

	public LogicalOperator getLogicalOperator() {
		return logicalOperator;
	}

	public void setLogicalOperator(LogicalOperator logicalOperator) {
		Objects.requireNonNull(logicalOperator);
		update();
		this.logicalOperator = logicalOperator;
	}

	/**
	 * 全Conditionを返す。
	 * 返されたListに直接新たなConditionを追加しpersistしても保存されないので注意が必要。
	 * @return 全Condition
	 */
	public List<Condition> getConditions() {
		prepare();
		return new LinkedList<>(conditions);
	}

	/**
	 * 新たなConditionを追加する。
	 * 更新の際には、一度{@link #clearConditions()}してから全追加しなおすこと
	 * @param condition
	 */
	public void addCondition(Condition condition) {
		Objects.requireNonNull(condition);
		prepare();
		update();
		conditions.add(condition);
	}

	public void clearConditions() {
		prepare();
		update();
		conditions.clear();
	}

	public List<String> getConditionSentences() {
		prepare();

		String delimiter = "\t";

		Iterable<String> i = () -> conditions
			.stream()
			.map(c -> c.getSentence().replaceAll(delimiter, " "))
			.iterator();
		String joined = String.join(delimiter + logicalOperator.express() + " ", i);

		return Arrays.asList(joined.split(delimiter));
	}

	private void prepare() {
		if (conditions == null) conditions = new LinkedList<>();
	}
}
