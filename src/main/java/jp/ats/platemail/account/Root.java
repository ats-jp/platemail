package jp.ats.platemail.account;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.google.gson.annotations.Expose;

/**
 * オブジェクトツリーのルート
 */
class Root {

	@Expose
	private long revision;

	@Expose
	private long nextOid;

	@Expose
	private Map<String, Account> accounts = new TreeMap<>(new MailAddressComparator());

	@Expose
	private Map<Long, Macro> macros = new TreeMap<>();

	@Expose
	private String lastModified;

	long temporalOid() {
		//どうせpersistが失敗したらここで払い出したoidも消えるので、簡易に発行
		return nextOid++;
	}

	void update() {
		lastModified = AccountUtils.now();
		revision++;
	}

	Map<String, Account> getAccounts() {
		return new TreeMap<>(accounts);
	}

	void addAccount(Account account) {
		Objects.requireNonNull(account);
		accounts.put(account.getMailAddress(), account);
	}

	Map<Long, Macro> getMacros() {
		return new TreeMap<>(macros);
	}

	void addMacro(Macro macro) {
		Objects.requireNonNull(macro);
		macros.put(macro.getOid(), macro);
	}

	boolean isAvailable(String mailAddress) {
		Objects.requireNonNull(mailAddress);
		return accounts.containsKey(mailAddress);
	}

	void replaceAccounts(Map<String, Account> accounts) {
		this.accounts = new TreeMap<>(new MailAddressComparator());
		this.accounts.putAll(accounts);
	}

	void replaceMacros(Map<Long, Macro> macros) {
		this.macros = new TreeMap<>();
		this.macros.putAll(macros);
	}

	private static class MailAddressComparator implements Comparator<String> {

		@Override
		public int compare(String address1, String address2) {
			String[] splitted1 = AddressUtils.splitMailAddress(address1);
			String[] splitted2 = AddressUtils.splitMailAddress(address2);

			int compareDomains = splitted1[1].compareTo(splitted2[1]);

			return compareDomains == 0 ? splitted1[0].compareTo(splitted2[0]) : compareDomains;
		}
	}
}
