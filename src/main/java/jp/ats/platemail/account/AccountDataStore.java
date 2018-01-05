package jp.ats.platemail.account;

import java.util.Set;

import jp.ats.platemail.common.Config;
import jp.ats.platemail.common.U;

/**
 * アカウント情報で必要となるI/Oを定義したインターフェイス
 */
public interface AccountDataStore {

	/**
	 * アカウント処理全体をロックする。
	 * 既に他プロセスがロック済み等ロックできなかった場合、falseを返す。
	 * @return ロックできたかどうか
	 */
	boolean lock();

	/**
	 * アカウント処理全体をアンロックする。
	 * アンロックに失敗した場合、falseを返す。
	 * @return アンロックできたかどうか
	 */
	boolean unlock();

	/**
	 * 全アカウント情報を管理するファイルの内容を読み込む。
	 * @return 全アカウント情報json
	 */
	String readJson();

	/**
	 * 全アカウント情報を管理するファイルに管理している情報を読み込む。
	 * @param json 全アカウント情報json
	 */
	void writeJson(String json);

	/**
	 * Root要素が持つlastModifiedとは別に、全アカウント情報を管理するファイルの最終更新時刻を取得する。
	 * @return 最終更新時刻
	 */
	long getJsonLastModified();

	/**
	 * 全アカウント情報を管理するファイルに適切なパーミッションを設定する。
	 */
	void setJsonFilePermissions();

	/**
	 * このシステムで有効な全ドメインを返す。
	 * @return 全有効ドメイン
	 */
	Set<String> getDomains();

	static AccountDataStore getInstance(Config config) {
		return U.getInstance(config.getAccountDataStoreClass(), config);
	}

}
