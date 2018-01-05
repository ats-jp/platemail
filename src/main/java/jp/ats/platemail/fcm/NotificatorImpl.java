package jp.ats.platemail.fcm;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NotificatorImpl implements Notificator {

	private static final String FCM_URL = "https://fcm.googleapis.com/fcm/send";

	@Override
	public Response request(String serverKey, String json) {
		// HTTPリクエスト
		URL url;
		try {
			url = new URL(FCM_URL);
		} catch (MalformedURLException e) {
			throw new Error();
		}

		HttpURLConnection connection;
		try {
			connection = (HttpURLConnection) url.openConnection();
		} catch (IOException e) {
			throw new RuntimeException(FCM_URL + " への接続に失敗しました", e);
		}

		try {
			connection.setConnectTimeout(10000);
			connection.setReadTimeout(10000);
			connection.setDoOutput(true);
			connection.setUseCaches(false);

			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Authorization", "key=" + serverKey);

			connection.connect();

			try (OutputStream out = connection.getOutputStream()) {
				out.write(json.toString().getBytes(StandardCharsets.UTF_8));
				out.flush();
			}

			String body;
			try (InputStream input = connection.getInputStream()) {
				body = read(input);
			}

			return new Response(connection.getResponseCode(), body);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			connection.disconnect();
		}
	}

	private static String read(InputStream input) throws IOException {
		//FCMは
		//Content－Type: application/json; charset=UTF-8
		//なのでUTF-8固定
		InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
		StringBuilder builder = new StringBuilder();
		char[] buffer = new char[512];
		int read;
		while (0 <= (read = reader.read(buffer))) {
			builder.append(buffer, 0, read);
		}

		return builder.toString();
	}
}
