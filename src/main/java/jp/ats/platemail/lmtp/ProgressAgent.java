package jp.ats.platemail.lmtp;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

class ProgressAgent {

	static final String CRLF = "\r\n";

	private final String host;

	private final BufferedReader input;

	private final OutputStream output;

	private final EnvelopeInfoDetector detector = new EnvelopeInfoDetector();

	private final ByteArrayOutputStream headers = new ByteArrayOutputStream();

	private boolean endsWithCrlf;

	ProgressAgent(String host, InputStream input, OutputStream output) {
		this.host = host;
		this.input = new BufferedReader(new InputStreamReader(input));
		this.output = output;
	}

	void addHeader(byte[] bytes, int length) {
		detector.add(bytes, length);
		headers.write(bytes, 0, length);
	}

	void startCommunicate() throws IOException {
		//senderとreceipientを検出できたかどうかチェック
		detector.checkSenderAndRecipient();

		//220 foo.edu LMTP server ready
		readReply(input, 220);

		sendServer("LHLO ", host, CRLF);

		//250-foo.edu
		//250-PIPELINING
		// :
		// :
		//250 SIZE
		while (readReply(input, 250));

		sendServer("MAIL FROM:<", detector.getSender(), ">", CRLF);

		//250 OK
		readReply(input, 250);

		sendServer("RCPT TO:<", detector.getRecipient(), ">", CRLF);

		//250 OK
		//or
		//550 No such user here
		readReply(input, 250);

		sendServer("DATA", CRLF);

		//354 Start mail input; end with <CRLF>.<CRLF>
		readReply(input, 354);
	}

	void flushHeaders() throws IOException {
		output.write(headers.toByteArray());
	}

	void sendServer(byte[] bytes, int length) throws IOException {
		output.write(bytes, 0, length);
	}

	//body行がCRLFで終わっていたか
	void setEndsWithCrlf(boolean endsWithCrlf) {
		this.endsWithCrlf = endsWithCrlf;
	}

	void finish() throws IOException {
		if (endsWithCrlf) {
			//最後がCRLFの場合は、 .<CRLF>
			sendServer(".", ProgressAgent.CRLF);
		} else {
			//最後がCRLFではない場合は、 <CRLF>.<CRLF>
			sendServer(ProgressAgent.CRLF, ".", ProgressAgent.CRLF);
		}

		//250 OK
		//or
		//452 <recipient@foo.edu> is temporarily over quota
		readReply(input, 250);

		sendServer("QUIT", CRLF);

		//221 foo.edu closing connection
		readReply(input, 221);
	}

	private void sendServer(String... datas) throws IOException {
		for (String data : datas) {
			output.write(data.getBytes(StandardCharsets.UTF_8));
		}

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
