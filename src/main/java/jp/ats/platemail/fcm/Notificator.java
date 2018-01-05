package jp.ats.platemail.fcm;

/**
 * FCM通知処理
 */
public interface Notificator {

	/**
	 * 通知を実行する
	 * @param serverKey
	 * @param requestJson FCMサーバに投げるJSON
	 * @return レスポンス情報
	 */
	Response request(String serverKey, String requestJson);

	/**
	 * FCMサーバからのレスポンス情報の入れ物クラス
	 */
	static class Response {

		private final int status;

		private final String bodyJson;

		public Response(int status, String bodyJson) {
			this.status = status;
			this.bodyJson = bodyJson;
		}

		public int getStatus() {
			return status;
		}

		public String getBodyJson() {
			return bodyJson;
		}
	}
}
