package jp.ats.platemail.account;

import jp.ats.platemail.account.AccountDataStoreImpl;
import jp.ats.platemail.common.Config;

public class WindowsAccountDataStore extends AccountDataStoreImpl {

	public WindowsAccountDataStore(Config config) {
		super(config);
	}

	@Override
	public void setJsonFilePermissions() {}
}
