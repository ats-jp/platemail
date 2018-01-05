package jp.ats.platemail.account;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;

import com.google.gson.annotations.Expose;

import jp.ats.platemail.account.filter.LogicalOperator;
import jp.ats.platemail.account.filter.MailSizeOperator;

/**
 * フィルタ情報
 */
public class Filter extends ConditionHolder implements Identified, Comparable<Filter> {

	private static final long UNUSE_MACRO_OID = -1;

	@Expose
	private long oid;

	/**
	 * 使用可否
	 */
	@Expose
	private boolean uses;

	/**
	 * 宛先メールアドレス
	 */
	@Expose
	private String sendTo;

	/**
	 * メールサイズオーバー時の動作
	 */
	@Expose
	private MailSizeOperator mailSizeOperator;

	/**
	 * 制限メールサイズ
	 */
	@Expose
	private int mailSizeKBytes;

	/**
	 * ふれあいメールを除いて転送
	 */
	@Expose
	private boolean excludeMailingList;

	/**
	 * マクロoid
	 */
	@Expose
	private long macro = UNUSE_MACRO_OID;

	private Account parent;

	//for Gson
	Filter() {}

	Filter(long oid, Account parent) {
		this.oid = oid;
		this.parent = parent;
		parent.addFilter(this);
	}

	@Override
	public long getOid() {
		return oid;
	}

	public boolean isUses() {
		return uses;
	}

	public void setUses(boolean uses) {
		update();
		this.uses = uses;
	}

	public String getSendTo() {
		return sendTo;
	}

	public void setSendTo(String sendTo) {
		Objects.requireNonNull(sendTo);
		update();
		this.sendTo = sendTo;
	}

	public MailSizeOperator getMailSizeOperator() {
		return mailSizeOperator;
	}

	public void setMailSizeOperator(MailSizeOperator mailSizeOperator) {
		Objects.requireNonNull(mailSizeOperator);
		update();
		this.mailSizeOperator = mailSizeOperator;
	}

	public boolean hasMacro() {
		return macro != UNUSE_MACRO_OID;
	}

	public long getMacroOid() {
		return macro;
	}

	public void setMacroOid(long macro) {
		update();
		this.macro = macro;
	}

	public int getMailSizeKBytes() {
		return mailSizeKBytes;
	}

	public void setMailSizeKBytes(int mailSizeKBytes) {
		update();
		this.mailSizeKBytes = mailSizeKBytes;
	}

	@Override
	public int compareTo(Filter another) {
		return Long.signum(oid - another.oid);
	}

	public Account getParent() {
		return parent;
	}

	//更新されたことをAccountに通知せずConditionを変更する
	void setConditionsWithoutUpdateNotice(LogicalOperator logicalOperator, Condition... condition) {
		prepare();
		this.logicalOperator = logicalOperator;
		conditions.clear();
		conditions.addAll(Arrays.asList(condition));
	}

	void init(Account parent) {
		this.parent = parent;
	}

	void remove() {
		parent.removeFilter(getOid());
	}

	@Override
	void update() {
		parent.update();
	}

	private void prepare() {
		if (conditions == null) conditions = new LinkedList<>();
	}
}
