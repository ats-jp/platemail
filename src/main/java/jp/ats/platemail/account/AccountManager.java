package jp.ats.platemail.account;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jp.ats.platemail.Config;
import jp.ats.platemail.U;
import jp.ats.platemail.account.filter.LogicalOperator;
import jp.ats.platemail.account.vam.VamController;
import jp.ats.platemail.account.vam.VamResult;

public class AccountManager {

	private final Map<Long, Identified> managedMembers = new HashMap<>();

	private final Map<String, List<Account>> domains = new HashMap<>();

	private final List<String> localDomains;

	private final Config config;

	private final AccountDataStore dataStore;

	private final Locker locker;

	private final Root root;

	private volatile long jsonTimestamp;

	/**
	 * 内部で適切にロックを行うインスタンスを生成
	 * @param config
	 * @return ロックするインスタンス
	 */
	public static AccountManager getInstance(Config config) {
		AccountDataStore dataStore = U.getInstance(config.getAccountDataStoreClass(), config);
		return new AccountManager(config, dataStore, Locker.getInstance(dataStore));
	}

	/**
	 * 外部でアカウント操作処理全体をロックする場合のインスタンスを生成
	 * @param config
	 * @return ロックしないインスタンス
	 */
	public static AccountManager getInstanceWithoutLock(Config config) {
		return new AccountManager(
			config,
			U.getInstance(config.getAccountDataStoreClass(), config),
			Locker.getDummyInstance());
	}

	private AccountManager(Config config, AccountDataStore dataStore, Locker locker) {
		this.config = config;
		this.dataStore = dataStore;
		this.locker = locker;

		localDomains = U.splitByComma(config.getLocalDomains());

		dataStore.getDomains().forEach(domain -> domains.put(domain, new LinkedList<>()));

		locker.lock();
		try {
			root = readWithoutLock(gson());

			//filterでmacroを参照するので先に初期化
			root.getMacros().forEach((oid, macro) -> regist(macro));
			root.getAccounts().forEach((address, account) -> init(account));
			//lock内で更新
			jsonTimestamp = dataStore.getJsonLastModified();
		} finally {
			locker.unlock();
		}
	}

	/**
	 * 更新に使用するアカウントインスタンスを探し出す。
	 * 更新するインスタンスは、このメソッド以外から取得してはならない。
	 * @param key インスタンス特定に必要となるキー
	 * @return 更新用のインスタンス
	 * @throws AlreadyUpdatedException 既に対象が更新、削除されてしまった場合
	 */
	public Account findAccount(Key key) throws AlreadyUpdatedException {
		return (Account) find(key);
	}

	/**
	 * アカウント名を変更する。
	 * 変更の対象となるインスタンスは {{@link #findAccount(Key)}}で取得したものを使用すること。
	 * @param newAccount 新アカウント名
	 * @param account アカウント名を変更したいインスタンス
	 * @return 新アカウント（プロパティの更新はこのインスタンスで行うこと）
	 * @throws DuplicateAddressException 既に存在するアドレスの場合
	 * @throws AlreadyUpdatedException 既に対象が更新、削除されてしまった場合
	 */
	public Account changeAccount(String newAccount, Account account) throws DuplicateAddressException, AlreadyUpdatedException {
		removeAccount(account.key());
		Account changed = newAccount(newAccount, account.getDomain());
		changed.copy(account);

		return changed;
	}

	/**
	 * 更新に使用するマクロインスタンスを探し出す。
	 * 更新するインスタンスは、このメソッド以外から取得してはならない。
	 * @param key インスタンス特定に必要となるキー
	 * @return 更新用のインスタンス
	 * @throws AlreadyUpdatedException 既に対象が更新、削除されてしまった場合
	 */
	public Macro findMacro(Key key) throws AlreadyUpdatedException {
		return (Macro) find(key);
	}

	/**
	 * 更新に使用するインスタンスを探し出す。
	 * 更新するインスタンスは、このメソッド以外から取得してはならない。
	 * @param oid フィルタのoid
	 * @param key 他の更新を検出するための親アカウントキー
	 * @return 更新用のインスタンス
	 * @throws AlreadyUpdatedException 既に対象の親アカウントが更新、またはフィルタが削除されてしまった場合
	 */
	public Filter findFilter(long oid, Key key) throws AlreadyUpdatedException {
		Filter filter = (Filter) managedMembers.get(oid);
		//他で削除されてしまった
		if (filter == null) throw new AlreadyUpdatedException();

		Account account = filter.getParent();

		//フィルタの親のアカウントのoidと、AccountKeyのoidが違う原因不明の不正な状態
		if (account.getOid() != key.oid()) throw new IllegalStateException(account.getOid() + " != " + key.oid());

		//以前にAccountKeyを作り出した時点から、他者のpersistで更新されているかチェック
		if (account.getRevision() != key.revision()) throw new AlreadyUpdatedException();

		return filter;
	}

	/**
	 * 有効なメールアドレスドメイン名のすべてを返す。
	 * @return 全ドメイン名
	 */
	public Set<Domain> getDomains() {
		//key:destination, value:alias となるMap
		Map<String, String> destAlias = Domain.createDestinationAliasMap(config);

		//key:alias, value:destination となるMap
		Map<String, String> aliasDest = Domain.createAliasDestinationMap(config);

		Set<Domain> result = new TreeSet<>();
		dataStore.getDomains().forEach(domain -> {
			if (aliasDest.containsKey(domain)) return;
			result.add(new Domain(domain, destAlias.get(domain)));
		});

		return result;
	}

	/**
	 * {@link AccountManager}が対象とする全{@link Account}を返す。
	 * @return 全{@link Account}
	 */
	public List<Account> getAllAccounts() {
		return root.getAccounts().values().stream().collect(Collectors.toList());
	}

	/**
	 * 指定されたドメインに属するアカウントのみ返す。
	 * @param domain
	 * @return 絞り込まれたアカウント
	 */
	public List<Account> getAccounts(String domain) {
		return root.getAccounts().values().stream().filter(a -> domain.equals(a.getDomain())).collect(Collectors.toList());
	}

	/**
	 * ローカルドメインのアカウントのみ返す。
	 * @return ローカルドメインの全アカウント
	 */
	public Map<String, Account> getLocalAccounts() {
		Map<String, Account> map = new TreeMap<>();
		localDomains.forEach(domain -> getAccounts(domain).forEach(a -> map.put(a.getMailAddress(), a)));

		return map;
	}

	/**
	 * メールアドレスからアカウントを特定する。
	 * このメソッドで取得したアカウントは更新には使用しないこと
	 * @param mailAddress
	 * @return 存在しなければnull
	 */
	public Account getAccount(String mailAddress) {
		String[] splitted = AddressUtils.splitMailAddress(mailAddress);

		//key:alias, value:destination となるMap
		Map<String, String> aliasDest = Domain.createAliasDestinationMap(config);

		//aliasドメインの場合、参照先に変換してアカウント取得
		String destDomain = aliasDest.get(splitted[1]);
		if (destDomain != null) mailAddress = splitted[0] + "@" + destDomain;

		return root.getAccounts().get(mailAddress);
	}

	/**
	 * アカウントを削除する。
	 * @param key
	 * @throws AlreadyUpdatedException
	 */
	public void removeAccount(Key key) throws AlreadyUpdatedException {
		findAccount(key).remove();
	}

	/**
	 * フィルタを削除する。
	 * @param key
	 * @throws AlreadyUpdatedException
	 */
	public void removeMacro(Key key) throws AlreadyUpdatedException {
		Macro macro = findMacro(key);
		macro.remove();
	}

	/**
	 * フィルタを削除する。
	 * @param oid
	 * @param key 他の更新を検出するための親アカウントキー
	 * @throws AlreadyUpdatedException
	 */
	public void removeFilter(long oid, Key key) throws AlreadyUpdatedException {
		Filter filter = findFilter(oid, key);
		filter.remove();
	}

	/**
	 * 新規アカウントを追加する場合の空のオブジェクトを作成します。
	 * @param account 
	 * @param domain 
	 * @return 新オブジェクト
	 * @throws DuplicateAddressException 既に登録されているアカウント（メールアドレスが重複してしまう）の場合
	 */
	public Account newAccount(String account, String domain) throws DuplicateAddressException {
		String mailAddress = Account.getMailAddress(account, domain);
		if (root.isAvailable(mailAddress)) throw new DuplicateAddressException(mailAddress);

		Account newAccount = new Account(root.temporalOid(), account, domain);
		root.addAccount(newAccount);

		return newAccount;
	}

	/**
	 * 新規マクロを追加する場合の空のオブジェクトを作成します。
	 * @param parent 
	 * @return 新オブジェクト
	 */
	public Macro newMacro(Account parent) {
		return new Macro(root.temporalOid());
	}

	/**
	 * 新規フィルタを追加する場合の空のオブジェクトを作成します。
	 * @param parent 
	 * @return 新オブジェクト
	 */
	public Filter newFilter(Account parent) {
		return new Filter(root.temporalOid(), parent);
	}

	/**
	 * 他のプロセスによって更新されたかどうかを返します。
	 * @return 他のプロセスによって更新されたかどうか
	 */
	public boolean wasUpdated() {
		return dataStore.getJsonLastModified() != jsonTimestamp;
	}

	/**
	 * オブジェクトツリーの保存を実行します。
	 * @return 登録結果情報
	 * @throws AlreadyUpdatedException 読み込んでから更新の間に、他プロセスが更新してしまい、既にリビジョンが上がってしまっている場合
	 */
	public Result persist() throws AlreadyUpdatedException {
		Gson gson = gson();
		locker.lock();
		try {

			//今読み込んだ保存側
			//今から登録更新削除する対象となるアカウントが、この中で既に更新されているか検査する
			Root storedRoot = readWithoutLock(gson);

			persistMacros(storedRoot);

			VamController vamController = U.getInstance(config.getVamControllerClass(), config);
			persistAccounts(storedRoot, vamController);

			root.update();

			VamResult vamResult = vamController.commit();

			String json = gson.toJson(root);
			dataStore.writeJson(json);

			//lock内で更新
			jsonTimestamp = dataStore.getJsonLastModified();

			Result result = new Result(vamResult, json);

			return result;
		} finally {
			locker.unlock();
		}
	}

	/**
	 * persist結果
	 */
	public static class Result {

		public final VamResult vam;

		public final String json;

		private Result(VamResult vam, String json) {
			this.vam = vam;
			this.json = json;
		}
	}

	void regist(Identified managed) {
		long oid = managed.getOid();
		if (managedMembers.containsKey(oid)) throw new RuntimeException("OID:[" + oid + "] が重複しています。");
		managedMembers.put(oid, managed);
	}

	private Managed find(Key key) throws AlreadyUpdatedException {
		Objects.requireNonNull(key);
		Managed managed = find(key.oid());

		long revision = key.revision();

		//以前にAccountKeyを作り出した時点から、他者のpersistで更新されているかチェック
		if (managed.getRevision() != revision) throw new AlreadyUpdatedException();

		return managed;
	}

	private Managed find(long oid) throws AlreadyUpdatedException {
		Managed managed = (Managed) managedMembers.get(oid);
		//他で削除されてしまった
		if (managed == null) throw new AlreadyUpdatedException();
		return managed;
	}

	private void persistMacros(Root storedRoot) throws AlreadyUpdatedException {
		Set<Long> oids = new TreeSet<>();

		//予定側全アカウント
		Map<Long, Macro> willStoreMap = root.getMacros();

		//保存側全アカウント
		Map<Long, Macro> storedMap = storedRoot.getMacros();

		//すべてのキーを一旦まとめる
		oids.addAll(willStoreMap.keySet());
		oids.addAll(storedMap.keySet());

		//最終保存用
		Map<Long, Macro> mergedMap = new TreeMap<>();
		for (Long oid : oids) {
			Macro willStore = willStoreMap.get(oid);
			Macro stored = storedMap.get(oid);

			//予定側の状態インスタンス
			Status status;

			if (willStore == null) {
				status = Status.NULL;
			} else {
				status = willStore.getStatus();
			}

			//予定側の状態インスタンスに、どのようにマージするか決めさせる
			Macro merged = status.merge(willStore, stored);

			//マージした結果がnullということは、登録しない、つまり削除されるということ
			if (merged != null) {
				mergedMap.put(oid, merged);
			}
		}

		//マージ結果で置き換えてマージ処理完了
		root.replaceMacros(mergedMap);
	}

	private void persistAccounts(Root storedRoot, VamController vamController) throws AlreadyUpdatedException {
		Set<String> allAddresses = new TreeSet<>();

		//予定側全アカウント
		Map<String, Account> willStoreMap = root.getAccounts();

		//保存側全アカウント
		Map<String, Account> storedMap = storedRoot.getAccounts();

		//すべてのキー（メールアドレス）を一旦まとめる
		allAddresses.addAll(willStoreMap.keySet());
		allAddresses.addAll(storedMap.keySet());

		//key:destination, value:alias となるMap
		Map<String, String> destAliasMap = Domain.createDestinationAliasMap(config);

		//最終保存用
		Map<String, Account> mergedMap = new TreeMap<>();
		for (String mailAddress : allAddresses) {
			Account willStore = willStoreMap.get(mailAddress);
			Account stored = storedMap.get(mailAddress);

			//予定側の状態インスタンス
			Status status;

			if (willStore == null) {
				status = Status.NULL;
			} else {
				status = willStore.getStatus();
			}

			//予定側の状態インスタンスに、どのようにマージするか決めさせる
			Account merged = status.merge(willStore, stored);

			//マージした結果がnullということは、登録しない、つまり削除されるということ
			if (merged != null) {
				mergedMap.put(mailAddress, merged);
				vamController.add(mailAddress);

				String[] splitted = AddressUtils.splitMailAddress(mailAddress);
				String aliasDomain = destAliasMap.get(splitted[1]);
				//エイリアスの存在するドメインであれば、そのエイリアス分もvirtual alias mapに追加する
				if (U.presents(aliasDomain)) vamController.add(splitted[0] + "@" + aliasDomain);
			}
		}

		//マージ結果で置き換えてマージ処理完了
		root.replaceAccounts(mergedMap);

		root.getAccounts()
			.values()
			.forEach(
				account -> account
					.getFilters()
					.forEach(AccountManager::prepareForFilterPersistence));
	}

	private static void prepareForFilterPersistence(Filter filter) {
		if (!filter.hasMacro()) return;
		filter.setConditionsWithoutUpdateNotice(LogicalOperator.AND);
	}

	private void init(Account account) {
		regist(account);
		account.getFilters().forEach(filter -> {
			filter.init(account);
			if (filter.hasMacro()) {
				Macro macro;
				try {
					macro = (Macro) find(filter.getMacroOid());
				} catch (AlreadyUpdatedException e) {
					//既にmacroが削除されていた場合は、filterを使用不可にする
					return;
				}

				List<Condition> conditions = macro.getConditions();
				filter.setConditionsWithoutUpdateNotice(macro.getLogicalOperator(), conditions.toArray(new Condition[conditions.size()]));
			}

			regist(filter);
		});
	}

	private static Gson gson() {
		return new GsonBuilder()
			.setPrettyPrinting()
			.excludeFieldsWithoutExposeAnnotation()
			.create();
	}

	private Root readWithoutLock(Gson gson) {
		String json = dataStore.readJson();

		if (!U.presents(json)) return new Root();

		return gson.fromJson(json, Root.class);
	}
}
