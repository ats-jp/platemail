package jp.ats.platemail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.Socket;

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
		byte[] message)
		throws IOException, ProtocolException {
		try (Socket sock = new Socket(InetAddress.getLocalHost(), DEFAULT_PORT);
			BufferedReader reply = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			PrintStream send = new PrintStream(sock.getOutputStream())) {

			readReply(reply, 220);

			send.print("LHLO " + host + CRLF);
			send.flush();

			while (readReply(reply, 250));

			send.print("MAIL FROM:<" + sender + ">" + CRLF);
			send.flush();

			readReply(reply, 250);

			send.print("RCPT TO:<" + recipient + ">" + CRLF);
			send.flush();

			readReply(reply, 250);

			send.print("DATA" + CRLF);
			send.flush();

			readReply(reply, 354);

			/*
			 * 
			 * 本文のCRLF.を変換して書き込み
			 * 
			 */

			send.print(CRLF);
			send.print(".");
			send.print(CRLF);
			send.flush();

			readReply(reply, 250);

			send.print("QUIT" + CRLF);
			send.flush();

			readReply(reply, 221);
		}
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
