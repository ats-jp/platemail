package jp.ats.platemail.account;

import jp.ats.platemail.Config;

public class WindowsAccountDataStore extends AccountDataStoreImpl {

	public WindowsAccountDataStore(Config config) {
		super(config);
	}

	@Override
	public void setJsonFilePermissions() {}
}
