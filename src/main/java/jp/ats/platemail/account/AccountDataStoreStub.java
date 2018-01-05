package jp.ats.platemail.account;

import java.util.Collections;
import java.util.Set;

public class AccountDataStoreStub implements AccountDataStore {

	private static final Object lock = new Object();

	private static String json = "";

	@Override
	public boolean lock() {
		return true;
	}

	@Override
	public boolean unlock() {
		return true;
	}

	@Override
	public String readJson() {
		synchronized (lock) {
			return json;
		}
	}

	@Override
	public void writeJson(String json) {
		synchronized (lock) {
			AccountDataStoreStub.json = json;
		}
	}

	@Override
	public long getJsonLastModified() {
		return 0;
	}

	@Override
	public void setJsonFilePermissions() {}

	@Override
	@SuppressWarnings("unchecked")
	public Set<String> getDomains() {
		return Collections.EMPTY_SET;
	}
}
