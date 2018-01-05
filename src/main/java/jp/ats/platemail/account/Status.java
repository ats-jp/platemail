package jp.ats.platemail.account;

import java.util.Objects;

/**
 * アカウントには、保存側と（登録等）予定側の2つがあるが、このクラスは予定側の状態をあらわす。
 */
public enum Status {

	/**
	 * アカウントが、保存側にはあるが予定側には存在しない状態
	 */
	NULL {

		@Override
		<T extends Managed> T merge(T willStore, T stored) {
			//予定側は存在せず保存側が存在するので、保存側を返す
			return stored;
		}

		@Override
		Status update() {
			throw new UnsupportedOperationException();
		}

		@Override
		Status remove() {
			throw new UnsupportedOperationException();
		}
	},

	/**
	 * 予定側のアカウントが保存状態から復元されただけで、まだ何も操作されていない状態
	 */
	RESTORED {

		@Override
		<T extends Managed> T merge(T willStore, T stored) {
			Objects.requireNonNull(willStore);

			//保存側がなくなっている場合、予定側は何も変化がないので削除してもOK
			if (stored == null) return null;

			//アドレスが同じでOIDが違う場合は、保存側の元のアカウントが削除されて同じアドレスで再登録されたと判断
			//予定側は何も変わっていないので保存側を返す
			if (willStore.getOid() != stored.getOid()) return stored;

			//予定側は版は上がっていないのでどちらにせよ保存側を返す
			return stored;
		}

		@Override
		Status update() {
			return WILL_UPDATE;
		}

		@Override
		Status remove() {
			return WILL_REMOVE;
		}
	},

	/**
	 * 新規追加予定の状態
	 */
	WILL_ADD {

		@Override
		<T extends Managed> T merge(T willStore, T stored) throws AlreadyUpdatedException {
			Objects.requireNonNull(willStore);

			//保存側がなければ予定側を登録するしかない
			//保存側で寸前に削除されたのかもしれないが検出しようがないので
			if (stored == null) return willStore;

			//新規登録時のOIDは、まだ確定していない仮番号なので、検査には使用しない

			//同じアドレスで既に他から保存済みなので、この追加は受け付けられない
			throw new AlreadyUpdatedException();
		}

		@Override
		Status update() {
			return this;
		}

		@Override
		Status remove() {
			throw new UnsupportedOperationException();
		}
	},

	/**
	 * 更新予定の状態
	 */
	WILL_UPDATE {

		@Override
		<T extends Managed> T merge(T willStore, T stored) throws AlreadyUpdatedException {
			Objects.requireNonNull(willStore);

			//保存側が存在しなければ、既に削除されているということで更新は受け付けられない
			if (stored == null) throw new AlreadyUpdatedException();

			//OIDが一致しない場合、保存側で一度削除されて同じアドレスで追加されたということなので、更新は受け付けられない
			if (willStore.getOid() != stored.getOid()) throw new AlreadyUpdatedException();

			//保存側が既に更新されている
			if (willStore.getRevision() != stored.getRevision()) throw new AlreadyUpdatedException();

			//版番号更新
			willStore.updateRevision();

			return willStore;
		}

		@Override
		Status update() {
			return this;
		}

		@Override
		Status remove() {
			throw new UnsupportedOperationException();
		}
	},

	/**
	 * 削除予定の状態
	 */
	WILL_REMOVE {

		@Override
		<T extends Managed> T merge(T willStore, T stored) throws AlreadyUpdatedException {
			Objects.requireNonNull(willStore);

			//保存側が存在しなければ既に削除されているということで削除対象
			if (stored == null) return null;

			//OIDが一致しない場合、保存側で一度削除されて同じアドレスで追加されたということなので削除は受け付けられない
			if (willStore.getOid() != stored.getOid()) throw new AlreadyUpdatedException();

			//保存側が既に更新されている
			if (willStore.getRevision() != stored.getRevision()) throw new AlreadyUpdatedException();

			//保存側が何も変わっていないので削除OK
			return null;
		}

		@Override
		Status update() {
			throw new UnsupportedOperationException();
		}

		@Override
		Status remove() {
			throw new UnsupportedOperationException();
		}
	};

	/**
	 * 予定側と保存側を状態に合わせてチェックし、最終的に保存するアカウントを返す。
	 * @param willStore 予定側アカウント
	 * @param stored 保存側アカウント
	 * @return マージ済み保存できるアカウント、nullの場合削除しなければならない
	 * @throws AlreadyUpdatedException 保存側が既に更新や削除が行われている場合
	 */
	abstract <T extends Managed> T merge(T willStore, T stored) throws AlreadyUpdatedException;

	/**
	 * このインスタンスにこれからアカウントの更新を行うことを通知し、その結果変化するステータスを返す。
	 */
	abstract Status update();

	/**
	 * このインスタンスにこれからアカウントの削除を行うことを通知し、その結果変化するステータスを返す。
	 */
	abstract Status remove();
}
