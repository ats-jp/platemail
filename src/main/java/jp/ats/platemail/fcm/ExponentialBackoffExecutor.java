package jp.ats.platemail.fcm;

import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.ats.platemail.fcm.Notificator.Response;

/**
 * Exponential Backoffアルゴリズムで、FCM通知処理の再試行を行う
 */
public class ExponentialBackoffExecutor {

	private static final int MAX_RETRY_COUNT = 7;

	/**
	 * Exponential Backoffアルゴリズムで、FCM通知処理の再試行を行う
	 * @param notificator
	 * @param serverKey FCMサーバキー
	 * @param json リクエストJSON
	 * @return multicast_id マルチキャスト メッセージを識別する一意の ID
	 * @throws ServerSideException サーバ側でエラーが発生し、リトライしてもエラーが解消されない場合
	 * @throws InterruptedException 割り込みが発生し、処理を中断した場合
	 */
	public static String execute(
		Notificator notificator,
		String serverKey,
		String json) throws ServerSideException, InterruptedException {
		for (int retryCount = 0; retryCount < MAX_RETRY_COUNT; retryCount++) {
			Response response = notificator.request(serverKey, json);

			if (doesRetry(response)) {
				long randomMilliseconds = (long) (Math.random() * 1000);
				long waitTime = BigInteger.valueOf(2).pow(retryCount).longValue() * 1000L + randomMilliseconds;

				Thread.sleep(waitTime);
			} else {
				return extractMulticastId(response.getBodyJson());
			}
		}

		throw new ServerSideException();
	}

	/**
	 * サーバ側でエラーが発生し、リトライしてもエラーが解消されない場合に発生する例外
	 */
	public static class ServerSideException extends Exception {

		private static final long serialVersionUID = -5821753146936081487L;
	}

	/**
	 * リクエストの不備などが原因でエラーが起きた場合に発生する例外
	 */
	public static class NotificationFailureException extends RuntimeException {

		private static final long serialVersionUID = 5366462458975562732L;

		private NotificationFailureException(String message) {
			super(message);
		}
	}

	/**
	 * レスポンス情報を元に、再試行対象かどうかチェックする
	 * @param response
	 * @return boolean
	 */
	private static boolean doesRetry(Response response) {
		int status = response.getStatus();
		String errorCode = extractErrorCode(response.getBodyJson());

		if (status == HttpURLConnection.HTTP_OK && errorCode == null) {
			// 正常終了
			return false;
		}

		if (errorCode.equals("Unavailable")
			|| errorCode.equals("InternalServerError")
			|| errorCode.equals("DeviceMessageRateExceeded")
			|| errorCode.equals("TopicsMessageRateExceeded")) {
			// 再試行対象
			return true;
		}

		throw new NotificationFailureException(
			"再試行不可能なエラーが返されました (Response status:[" + status + "] error code:[" + errorCode + "] body json:" + response.getBodyJson() + ")");
	}

	private static final Pattern errorPattern = Pattern.compile(
		"\"results\"\\s*:\\s*\\[\\s*\\{\\s*\"error\"\\s*:\\s*\"([^\"]+)\"\\s*\\}\\s*\\]",
		Pattern.MULTILINE);

	/**
	 * FCM Response JSONからerrorを抽出
	 * @param json
	 * @return errorCode
	 */
	private static String extractErrorCode(String json) {
		Matcher matcher = errorPattern.matcher(json);

		if (!matcher.find()) return null;

		return matcher.group(1);
	}

	private static final Pattern idPattern = Pattern.compile("\"multicast_id\"\\s*:\\s*(\\d+)", Pattern.MULTILINE);

	/**
	 * FCM Response JSONからmulticast_idを抽出
	 * @param json
	 * @return errorCode
	 */
	private static String extractMulticastId(String json) {
		Matcher matcher = idPattern.matcher(json);

		if (!matcher.find()) throw new RuntimeException("IDの抽出に失敗しました " + json);

		return matcher.group(1);
	}
}
