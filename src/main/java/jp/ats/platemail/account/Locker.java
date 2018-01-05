package jp.ats.platemail.account;

import jp.ats.platemail.common.Config;
import jp.ats.platemail.common.U;

public interface Locker {

	void lock();

	void unlock();

	static Locker getInstance(Config config) {
		return new LockerImpl(U.getInstance(config.getAccountDataStoreClass(), config));
	}

	static Locker getInstance(AccountDataStore dataStore) {
		return new LockerImpl(dataStore);
	}

	static Locker getDummyInstance() {
		return new DummyLocker();
	}

	class LockerImpl implements Locker {

		//チェック回数
		private static final int retryCount = 20;

		//ロックが解放されたかチェックする間隔
		private static final int waitMillis = 500;

		private final AccountDataStore dataStore;

		private LockerImpl(AccountDataStore dataStore) {
			this.dataStore = dataStore;
		}

		@Override
		public void lock() {
			int counter = 0;
			while (!dataStore.lock()) {
				if (counter++ > retryCount) throw new IllegalStateException("ロックを取得することができませんでした。");
				try {
					Thread.sleep(waitMillis);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}

		@Override
		public void unlock() {
			if (!dataStore.unlock())
				throw new IllegalStateException("ロックを解放することができませんでした。");
		}
	}

	class DummyLocker implements Locker {

		private DummyLocker() {}

		@Override
		public void lock() {}

		@Override
		public void unlock() {}
	}
}
