package jp.ats.platemail.lmtp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import jp.ats.platemail.LineSpliterator;

/*
 * 参考URL
 * http://research.nii.ac.jp/~ichiro/syspro98/smtp.html
 * http://www.faqs.org/rfcs/rfc2033.html
 * https://git.zimbra.com/repos/zimbra-foss/ZimbraCommon/src/java/com/zimbra/common/lmtp/LmtpClient.java
 */
public class LmtpClient {

	private static final String defaultHost = "localhost";

	private static final int DEFAULT_PORT = 24;

	public static void execute(byte[] message) throws IOException {
		try (Socket socket = new Socket(InetAddress.getLocalHost(), DEFAULT_PORT);
			InputStream input = socket.getInputStream();
			OutputStream output = socket.getOutputStream()) {
			execute(defaultHost, input, output, message);
		}
	}

	public static void execute(
		String host,
		byte[] message)
		throws IOException {
		try (Socket socket = new Socket(InetAddress.getLocalHost(), DEFAULT_PORT);
			InputStream input = socket.getInputStream();
			OutputStream output = socket.getOutputStream()) {
			execute(host, input, output, message);
		}
	}

	public static void execute(
		String host,
		InputStream input,
		OutputStream output,
		byte[] message)
		throws IOException {
		ProgressAgent visitor = new ProgressAgent(host, input, output);
		ProgressStatus[] status = { ProgressStatus.HEADER_LINE };
		new LineSpliterator().split(new ByteArrayInputStream(message), (bytes, length) -> {
			try {
				status[0] = status[0].accept(visitor, bytes, length);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		});

		visitor.finish();
	}
}
