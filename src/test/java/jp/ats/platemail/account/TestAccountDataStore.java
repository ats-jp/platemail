package jp.ats.platemail.account;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jp.ats.platemail.account.AccountDataStore;

public class TestAccountDataStore implements AccountDataStore {

	private static String json;

	public static void setJson(String json) {
		TestAccountDataStore.json = json;
	}

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
		return json;
	}

	@Override
	public void writeJson(String json) {
		System.out.println(json);
	}

	@Override
	public long getJsonLastModified() {
		return 0;
	}

	@Override
	public void setJsonFilePermissions() {}

	@Override
	public Set<String> getDomains() {
		return new HashSet<>(Arrays.asList("domain"));
	}
}
