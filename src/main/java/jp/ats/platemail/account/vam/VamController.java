package jp.ats.platemail.account.vam;

/**
 * メールサーバのアカウント（メールアドレス）を管理する処理を抽象化したインターフェイス
 */
public interface VamController {

	/**
	 * メールサーバで管理しているアカウントに、エイリアスを追加する
	 * アカウントが存在しない場合、新規に追加される
	 * @param address
	 */
	void add(String address);

	/**
	 * メールサーバに変更を反映させる
	 * @return 書き込み件数
	 */
	VamResult commit();
}
