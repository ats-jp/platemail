package jp.ats.platemail.account;

public class Key {

	private final long oid;

	private final long revision;

	/**
	 * 他で保存されていた文字列化表現から{@link Key}を復元する。
	 * @param serialed
	 * @return key
	 */
	public static Key parse(String serialed) {
		int delimPosition = serialed.indexOf('-');
		return new Key(
			Long.parseLong(serialed.substring(0, delimPosition)),
			Long.parseLong(serialed.substring(delimPosition + 1)));
	}

	Key(long oid, long revision) {
		this.oid = oid;
		this.revision = revision;
	}

	/**
	 * このKeyを持つ{@link Account}の{@link AccountManager}内で一意の番号であるOIDを返す。
	 * @return OID
	 */
	public long oid() {
		return oid;
	}

	/**
	 * このKeyを持つ{@link Account}の改訂番号を返す。
	 * @return revision
	 */
	public long revision() {
		return revision;
	}

	/**
	 * 文字列化し、他で保存可能な状態にする。
	 * @return 文字列化
	 */
	public String serialize() {
		return oid + "-" + revision;
	}
}
