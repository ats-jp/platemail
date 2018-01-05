package jp.ats.platemail.account;

import com.google.gson.annotations.Expose;

public class Macro extends ConditionHolder implements Managed {

	@Expose
	private long oid;

	@Expose
	private long revision;

	@Expose
	private String name;

	private Status status = Status.RESTORED;

	//for Gson
	Macro() {}

	Macro(long oid) {
		this.oid = oid;
		status = Status.WILL_ADD;
		lastModified = AccountUtils.now();
	}

	/**
	 * 最終更新時刻
	 */
	@Expose
	private String lastModified;

	@Override
	public long getOid() {
		return oid;
	}

	@Override
	public long getRevision() {
		return revision;
	}

	@Override
	public void updateRevision() {
		revision++;
	}

	@Override
	public Status getStatus() {
		return status;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * このインスタンスが削除対象となったことを通知する
	 */
	void remove() {
		status = status.remove();
	}

	/**
	 * このインスタンスが更新対象となったことを通知する
	 */
	@Override
	void update() {
		status = status.update();
		lastModified = AccountUtils.now();
	}

}
