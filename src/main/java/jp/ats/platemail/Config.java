package jp.ats.platemail;

import java.util.Properties;

import jp.ats.platemail.account.AccountDataStore;
import jp.ats.platemail.account.vam.VamController;
import jp.ats.relay.CommandExecutor;
import jp.ats.relay.NextCommand;
import jp.ats.relay.Shell;

public class Config {

	private static final ThreadLocal<Config> threadLocal = ThreadLocal.withInitial(() -> new Config());

	private final Properties properties;

	public static Config getInstance() {
		return threadLocal.get();
	}

	public Config(Properties properties) {
		this.properties = properties;
	}

	private Config() {
		properties = Shell.config("conf/platemail.properties");
	}

	/**
	 * @return トリガー処理のロックフラグ用ディレクトリ
	 */
	public String getLockDirectoryForTrigger() {
		return properties.getProperty("lockdir-for-trigger");
	}

	/**
	 * @return メール転送処理のメール取得ディレクトリ
	 */
	public String getWorkDirectoryForTransfer() {
		return properties.getProperty("dir-for-transfer");
	}

	/**
	 * @return メール転送処理のロックフラグ用ディレクトリ
	 */
	public String getLockDirectoryForTransfer() {
		return properties.getProperty("lockdir-for-transfer");
	}

	/**
	 * @return 次工程を起動する{@link NextCommand}の実装クラス
	 */
	public String getNextCommandClass() {
		return properties.getProperty("next-command-class");
	}

	/**
	 * @return トリガーコマンド
	 */
	public String getTriggerCommand() {
		return properties.getProperty("trigger-command");
	}

	/**
	 * @return {@link VamController}の実装クラス
	 */
	public String getVamControllerClass() {
		return properties.getProperty("vam-controller-class");
	}

	/**
	 * @return {@link AccountDataStore}の実装クラス
	 */
	public String getAccountDataStoreClass() {
		return properties.getProperty("account-data-store-class");
	}

	/**
	 * @return アカウント管理処理のロックフラグ用ディレクトリ
	 */
	public String getLockdirForAccount() {
		return properties.getProperty("lockdir-for-account");
	}

	/**
	 * @return エラーメール一時置き場ディレクトリ
	 */
	public String getDirectoryForErrorMail() {
		return properties.getProperty("dir-for-error-mail");
	}

	/**
	 * @return アカウント情報保存ファイル
	 */
	public String getAccountRepository() {
		return properties.getProperty("account-repository");
	}

	/**
	 * @return アカウント情報保存ファイルの過去分を保存するか
	 */
	public String getSaveAccountRepositoryHistory() {
		return properties.getProperty("save-account-repository-history");
	}

	/**
	 * @return アカウント情報保存ファイルの過去分格納場所
	 */
	public String getDirectoryForAccountRepositoryHistory() {
		return properties.getProperty("dir-for-account-repository-history");
	}

	/**
	 * @return アプリケーションでメール管理するドメイン<br>
	 * カンマ区切りで複数指定可能
	 */
	public String getLocalDomains() {
		return properties.getProperty("local-domains");
	}

	/**
	 * @return 実際にメールを受信するシステムユーザー
	 */
	public String getRealMailUser() {
		return properties.getProperty("real-mail-user");
	}

	/**
	 * @return Postfixのアカウントファイル
	 */
	public String getVirtualAliasMap() {
		return properties.getProperty("virtual-alias-map");
	}

	/**
	 * @return 使用可能ドメイン定義ファイル
	 */
	public String getVirtualAliasDomains() {
		return properties.getProperty("virtual-alias-domains");
	}

	/**
	 * @return ドメイン別名定義<br>
	 * エイリアス -> エイリアス先ドメイン（カンマ区切りで複数指定可能）
	 */
	public String getDomainAliases() {
		return properties.getProperty("domain-aliases");
	}

	/**
	 * @return メールサーバのアカウントをリフレッシュするコマンド
	 */
	public String getMailAccountRefreshCommand() {
		return properties.getProperty("mail-account-refresh-command");
	}

	/**
	 * @return MailRedirectorの実装クラス
	 */
	public String getMailRedirectorClass() {
		return properties.getProperty("mail-redirector-class");
	}

	/**
	 * @return MessageIdGeneratorの実装クラス
	 */
	public String getMessageIdGeneratorClass() {
		return properties.getProperty("message-id-generator-class");
	}

	/**
	 * @return エンベロープ送信者を書き換えない対象となる転送メール宛先のドメイン名（カンマ区切り）
	 */
	public String getOriginalSenderRedirectionDomains() {
		return properties.getProperty("original-sender-redirection-domains");
	}

	/**
	 * @return メール送信用コマンド
	 */
	public String getMailSendCommand() {
		return properties.getProperty("mail-send-command");
	}

	/**
	 * @return {@link CommandExecutor}の実装クラス
	 */
	public String getCommandExecutorClass() {
		return properties.getProperty("command-executor-class");
	}

	/**
	 * @return AccountAddressDetectorの実装クラス
	 */
	public String getAccountAddressDetectorClass() {
		return properties.getProperty("account-address-detector-class");
	}

	/**
	 * @return システムエラー発生時の通知メールを使用するか
	 */
	public String usesSystemErrorMail() {
		return properties.getProperty("uses-system-error-mail");
	}

	/**
	 * @return バウンスメールの送信者
	 */
	public String getBounceMailFrom() {
		return properties.getProperty("bounce-mail-from");
	}

	/**
	 * @return em以外の外部への転送（エイリアス、フィルタ転送ともに）を禁止するか
	 */
	public String deniesTransferOutside() {
		return properties.getProperty("denies-transfer-outside");
	}

	/**
	 * @return 転送処理最大並列スレッド数
	 */
	public String getMaxConcurrencyOfTransfer() {
		return properties.getProperty("transfer-max-concurrency");
	}

	/**
	 * @return 次処理に受け渡す際、改行コードをCRLFに変換するか
	 */
	public String convertsToCrlf() {
		return properties.getProperty("convert-to-crlf");
	}

	/**
	 * @return 転送処理に次処理はあるか
	 */
	public String hasNext() {
		return properties.getProperty("has-next");
	}

	/**
	 * @return 転送処理がDBを使用するか
	 */
	public String usesDatabase() {
		return properties.getProperty("uses-database");
	}

	/**
	 * @return 転送処理の次コマンド
	 */
	public String getNextCommand() {
		return properties.getProperty("next-command");
	}

	/**
	 * @return 次処理のロックディレクトリ
	 */
	public String getLockDirectoryForNext() {
		return properties.getProperty("lockdir-for-next");
	}

	/**
	 * @return 次処理の作業ディレクトリ
	 */
	public String getWorkDirectoryForNext() {
		return properties.getProperty("dir-for-next");
	}

	/**
	 * @return LMTP転送処理のメール取得ディレクトリ
	 */
	public String getWorkDirectoryForLmtp() {
		return properties.getProperty("dir-for-lmtp");
	}

	/**
	 * @return LMTP転送処理のロックフラグ用ディレクトリ
	 */
	public String getLockDirectoryForLmtp() {
		return properties.getProperty("lockdir-for-lmtp");
	}

	/**
	 * @return LMTP処理最大並列スレッド数
	 */
	public String getMaxConcurrencyOfLmtp() {
		return properties.getProperty("lmtp-max-concurrency");
	}
}
