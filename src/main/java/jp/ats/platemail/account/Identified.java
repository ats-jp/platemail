package jp.ats.platemail.account;

/**
 * OIDにより管理されている対象を表す
 * @see Account
 * @see Filter
 */
public interface Identified {

	/**
	 * {@link AccountManager}内で一意の番号であるOIDを返す。
	 * @return OID
	 */
	long getOid();
}
