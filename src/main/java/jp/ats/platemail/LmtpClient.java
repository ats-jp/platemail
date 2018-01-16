package jp.ats.platemail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * 参考URL
 * http://research.nii.ac.jp/~ichiro/syspro98/smtp.html
 * http://www.faqs.org/rfcs/rfc2033.html
 * https://git.zimbra.com/repos/zimbra-foss/ZimbraCommon/src/java/com/zimbra/common/lmtp/LmtpClient.java
 */
public class LmtpClient {

	private static final String senderHeaderName = "Return-Path";

	private static final String recipientHeaderName = "X-Original-To";

	private static final int DEFAULT_PORT = 24;

	private static final String CRLF = "\r\n";

	public static void send(
		String host,
		InputStream message)
		throws IOException {
		try (Socket socket = new Socket(InetAddress.getLocalHost(), DEFAULT_PORT);
			InputStream input = socket.getInputStream();
			OutputStream output = socket.getOutputStream()) {
			send(host, input, output, message);
		}
	}

	public static void send(
		String host,
		InputStream input,
		OutputStream output,
		InputStream message)
		throws IOException {
		ByteArrayOutputStream header = new ByteArrayOutputStream();
		EnvelopeDetector detector = new EnvelopeDetector();

		BufferedReader reply = new BufferedReader(new InputStreamReader(input));

		boolean[] endsHeader = { false };
		boolean[] endsWithCrlf = { false };
		//本文のCRLF.を変換して書き込み
		new LineSpliterator().split(message, (buffer, length) -> {
			try {
				if (!endsHeader[0]) {
					if (length == 2 && buffer[0] == '\r' && buffer[1] == '\n') {
						if (detector.hasSenderAndRecipient())
							//Postfixから受け取ったメールであれば両方必ずあるはず
							//なのでない場合は以上とみなす
							throw new IllegalStateException(detector.getSenderAndRecipient());

						//CRLFのみの行（空行）でヘッダの終わりを判定
						endsHeader[0] = true;

						startCommunicate(reply, output, host, detector.sender, detector.recipient);

						//全Header
						output.write(header.toByteArray());
						//空行
						output.write(buffer, 0, length);
					} else {
						String line = new String(buffer, 0, length - 2 /*改行コード(CRLF)分削除*/, StandardCharsets.US_ASCII);
						detector.add(line);
						header.write(buffer, 0, length);
					}
				} else {
					//body内の行頭の . は、 .. に変換
					if (buffer[0] == '.') output.write('.');
					output.write(buffer, 0, length);

					//行末がCRLFかを判定
					if (length > 2 && buffer[length - 1] == '\n' && buffer[length - 2] == '\r') {
						endsWithCrlf[0] = true;
					} else {
						endsWithCrlf[0] = false;
					}
				}
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		});

		if (endsWithCrlf[0]) {
			//最後がCRLFの場合は、 .<CRLF>
			write(output, "." + CRLF);
		} else {
			//最後がCRLFではない場合は、 <CRLF>.<CRLF>
			write(output, CRLF + "." + CRLF);
		}

		//250 OK
		//or
		//452 <recipient@foo.edu> is temporarily over quota
		readReply(reply, 250);

		write(output, "QUIT" + CRLF);

		//221 foo.edu closing connection
		readReply(reply, 221);
	}

	private static void startCommunicate(
		BufferedReader reply,
		OutputStream output,
		String host,
		String sender,
		String recipient)
		throws IOException {
		//220 foo.edu LMTP server ready
		readReply(reply, 220);

		write(output, "LHLO " + host + CRLF);

		//250-foo.edu
		//250-PIPELINING
		//250 SIZE
		while (readReply(reply, 250));

		write(output, "MAIL FROM:<" + sender + ">" + CRLF);

		//250 OK
		readReply(reply, 250);

		write(output, "RCPT TO:<" + recipient + ">" + CRLF);

		//250 OK
		//or
		//550 No such user here
		readReply(reply, 250);

		write(output, "DATA" + CRLF);

		//354 Start mail input; end with <CRLF>.<CRLF>
		readReply(reply, 354);
	}

	private static class EnvelopeDetector {

		private final StringBuilder builder = new StringBuilder();

		private String sender;

		private String recipient;

		private static final Pattern isFolding = Pattern.compile("^\\s+");

		private static final Pattern addressExtract = Pattern.compile("<([^>]+)>");

		private void add(String line) {
			builder.append(line);

			if (isFolding.matcher(line).find()) return;

			String field = builder.toString();
			String name = getHeaderName(field);

			if (sender == null && name.equalsIgnoreCase(senderHeaderName)) {
				sender = extractAddress(field);
			} else if (recipient == null && name.equalsIgnoreCase(recipientHeaderName)) {
				recipient = extractAddress(field);
			}
		}

		private boolean hasSenderAndRecipient() {
			return sender != null && recipient != null;
		}

		private String getSenderAndRecipient() {
			return "sender:<" + sender + ">, recipient:<" + recipient + ">";
		}

		private static String getHeaderName(String target) {
			int colonPosition = target.indexOf(':');
			if (colonPosition == -1) return "";
			return target.substring(0, colonPosition);
		}

		private static String extractAddress(String target) {
			int colonIndex = target.indexOf(':');
			if (colonIndex == -1) return null;
			String body = target.substring(0, colonIndex);

			Matcher matcher = addressExtract.matcher(target);

			if (matcher.find()) return matcher.group(1);

			return U.removeWhiteSpaces(body);
		}
	}

	private static void write(OutputStream output, String data) throws IOException {
		output.write(data.getBytes(StandardCharsets.UTF_8));
		output.flush();
	}

	private static boolean readReply(BufferedReader reader, int successCode) throws IOException {
		String code = String.valueOf(successCode);
		int continueMarkIndex = code.length();
		String line = reader.readLine();
		if (!line.startsWith(code)) {
			String errorCode = line.split("[ -]")[0];
			throw new LmtpServerException(Integer.parseInt(errorCode), line);
		}

		return line.charAt(continueMarkIndex) == '-';
	}
}
