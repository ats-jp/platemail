package jp.ats.platemail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/*
 * 参考URL
 * http://research.nii.ac.jp/~ichiro/syspro98/smtp.html
 * http://www.faqs.org/rfcs/rfc2033.html
 * https://git.zimbra.com/repos/zimbra-foss/ZimbraCommon/src/java/com/zimbra/common/lmtp/LmtpClient.java
 */
public class LmtpClient {

	private static final int DEFAULT_PORT = 24;

	private static final String CRLF = "\r\n";

	public void send(
		String host,
		String sender,
		String recipient,
		InputStream message)
		throws IOException, ProtocolException {
		try (Socket socket = new Socket(InetAddress.getLocalHost(), DEFAULT_PORT);
			InputStream input = socket.getInputStream();
			OutputStream output = socket.getOutputStream()) {
			send(input, output, host, sender, recipient, message);
		}
	}

	public void send(
		InputStream input,
		OutputStream output,
		String host,
		String sender,
		String recipient,
		InputStream message)
		throws IOException, ProtocolException {
		BufferedReader reply = new BufferedReader(new InputStreamReader(input));

		readReply(reply, 220);

		write(output, "LHLO " + host + CRLF);

		while (readReply(reply, 250));

		write(output, "MAIL FROM:<" + sender + ">" + CRLF);

		readReply(reply, 250);

		write(output, "RCPT TO:<" + recipient + ">" + CRLF);

		readReply(reply, 250);

		write(output, "DATA" + CRLF);

		readReply(reply, 354);

		boolean[] endsHeader = { false };

		//本文のCRLF.を変換して書き込み
		new LineSpliterator().split(message, (buffer, length) -> {
			try {
				if (endsHeader[0]) {
					//body内の . は、 .. に変換
					if (buffer[0] == '.') output.write('.');
				} else if (buffer[0] == '\r' && buffer[0] == '\n') endsHeader[0] = true;

				output.write(buffer, 0, length);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		});

		write(output, CRLF + "." + CRLF);

		readReply(reply, 250);

		write(output, "QUIT" + CRLF);

		readReply(reply, 221);
	}

	private void write(OutputStream output, String data) throws IOException {
		output.write(data.getBytes(StandardCharsets.UTF_8));
		output.flush();
	}

	private boolean readReply(BufferedReader reader, int successCode) throws IOException {
		String code = String.valueOf(successCode);
		int continueMarkIndex = code.length();
		String line = reader.readLine();
		if (!line.startsWith(code)) {
			throw new ProtocolException(line);
		}

		return line.charAt(continueMarkIndex) == '-';
	}
}
