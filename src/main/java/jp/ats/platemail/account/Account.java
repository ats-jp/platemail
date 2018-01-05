package jp.ats.platemail.account;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.annotations.Expose;

/**
 * アカウント情報
 */
public class Account implements Managed {

	@Expose
	private long oid;

	@Expose
	private long revision;

	/**
	 * アカウント名
	 */
	@Expose
	private String account;

	/**
	 * ドメイン名
	 */
	@Expose
	private String domain;

	/**
	 * 氏名
	 */
	@Expose
	private String name;

	/**
	 * ローカルアカウントかどうか
	 */
	@Expose
	private boolean localAccount;

	/**
	 * フィルタ転送時エイリアス先にコピーを送信するかどうか
	 */
	@Expose
	private boolean aliasWhenFilterRedirection;

	/**
	 * このアカウントが保持するエイリアス
	 */
	@Expose
	private Set<String> aliases;

	/**
	 * このアカウントが保持するフィルタ
	 */
	@Expose
	private Set<Filter> filters;

	/**
	 * 最終更新時刻
	 */
	@Expose
	private String lastModified;

	private String lastModifiedForView;

	private Status status = Status.RESTORED;

	//for Gson
	Account() {}

	Account(long oid, String account, String domain) {
		this.oid = oid;
		this.account = account;
		this.domain = domain;
		status = Status.WILL_ADD;

		//初期値はtrue
		aliasWhenFilterRedirection = true;

		lastModified = AccountUtils.now();
	}

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

	/**
	 * アカウントを特定するためのOIDと読み込んだ時点でのrevisionを持つ{@link Key}を返します。
	 * @return key
	 */
	public Key key() {
		return new Key(getOid(), getRevision());
	}

	public static String getMailAddress(String account, String domain) {
		return account + "@" + domain;
	}

	public String getMailAddress() {
		return getMailAddress(account, domain);
	}

	public String getAccount() {
		return account;
	}

	public String getDomain() {
		return domain;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		update();
		this.name = name;
	}

	public boolean isLocalAccount() {
		return localAccount;
	}

	public void setLocalAccount(boolean localAccount) {
		update();
		this.localAccount = localAccount;
	}

	public boolean isAliasWhenFilterRedirection() {
		return aliasWhenFilterRedirection;
	}

	public void setAliasWhenFilterRedirection(boolean aliasWhenFilterRedirection) {
		update();
		this.aliasWhenFilterRedirection = aliasWhenFilterRedirection;
	}

	public List<String> getAliases() {
		prepare();
		return new LinkedList<>(aliases);
	}

	public void addAlias(String alias) {
		Objects.requireNonNull(alias);
		prepare();
		update();
		aliases.add(alias);
	}

	public void clearAliases() {
		prepare();
		update();
		aliases.clear();
	}

	public Set<Filter> getFilters() {
		prepare();
		return new TreeSet<>(filters);
	}

	public String getLastModified() {
		return lastModified;
	}

	//外部で使用するために表記を変えた最終更新時刻
	public String getLastModifiedForView() {
		return lastModifiedForView;
	}

	//外部で使用するために表記を変えた最終更新時刻をセット
	public void setLastModifiedForView(String lastModifiedForView) {
		this.lastModifiedForView = lastModifiedForView;
	}

	@Override
	public Status getStatus() {
		return status;
	}

	void addFilter(Filter filter) {
		Objects.requireNonNull(account);
		prepare();
		update();
		filters.add(filter);
	}

	void removeFilter(long oid) {
		prepare();
		update();
		filters.removeIf(f -> f.getOid() == oid);
	}

	/**
	 * このインスタンスが更新対象となったことを通知する
	 */
	void update() {
		status = status.update();
		lastModified = AccountUtils.now();
	}

	/**
	 * このインスタンスが削除対象となったことを通知する
	 */
	void remove() {
		status = status.remove();
	}

	/**
	 * アカウント変更のために、情報を元インスタンスからコピーする
	 * @param original
	 */
	void copy(Account original) {
		name = original.name;
		localAccount = original.localAccount;
		aliasWhenFilterRedirection = original.aliasWhenFilterRedirection;
		aliases = original.aliases;
		filters = original.filters;
		lastModified = AccountUtils.now();
	}

	private void prepare() {
		if (aliases == null) aliases = new TreeSet<>();
		if (filters == null) filters = new TreeSet<>();
	}
}
