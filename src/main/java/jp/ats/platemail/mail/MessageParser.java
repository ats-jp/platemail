package jp.ats.platemail.mail;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.ParseException;

import org.jsoup.Jsoup;

import jp.ats.platemail.U;

public class MessageParser {

	private final MessageListener listener;

	private String plainTextBody;

	private String htmlTextBody;

	private String htmlCharset;

	public MessageParser(MessageListener listener) {
		this.listener = listener;
	}

	/**
	 * 今受信したメールをparseする場合
	 * @param message
	 * @throws IOException
	 * @throws MessagingException
	 */
	public void start(byte[] message) throws IOException, MessagingException {
		start(new Date(), message);
	}

	/**
	 * 今受信したメールをparseする場合
	 * @param recievedDate
	 * @param message
	 * @throws IOException
	 * @throws MessagingException
	 */

	/**
	 * 過去に受信済みのメールをparseする場合
	 * @param recievedDate
	 * @param messageBytes
	 * @throws IOException
	 * @throws MessagingException
	 */
	public void start(Date recievedDate, byte[] messageBytes) throws IOException, MessagingException {
		listener.notifyRecievedDate(recievedDate);
		listener.notifyRawMessage(messageBytes);

		MimeMessage mimeMessage = new MimeMessage(
			//メール解析のたびにSessionを生成せず、デフォルトを使用する
			Session.getDefaultInstance(System.getProperties(), null),
			new ByteArrayInputStream(messageBytes));

		start(mimeMessage);

		if (plainTextBody == null) plainTextBody = convertHtmlToPlainText(htmlTextBody);

		listener.notifyPlainTextBody(plainTextBody);
		listener.notifyHtmlTextBody(htmlTextBody, htmlCharset);
	}

	/**
	 * 今受信したメールをparseする場合
	 * @param message
	 * @throws IOException
	 * @throws MessagingException
	 */
	public void start(Path message) throws IOException, MessagingException {
		start(new Date(), message);
	}

	/**
	 * 今受信したメールをparseする場合
	 * @param recievedDate
	 * @param message
	 * @throws IOException
	 * @throws MessagingException
	 */
	public void start(Date recievedDate, Path message) throws IOException, MessagingException {
		try (InputStream input = new BufferedInputStream(Files.newInputStream(message))) {
			start(recievedDate, U.readBytes(input));
		}
	}

	boolean alreadyHasTextBody() {
		return plainTextBody != null || htmlTextBody != null;
	}

	void processTextBody(Part part, PartListener listener) throws IOException, MessagingException {
		Object content = part.getContent();

		if (content instanceof String) {
			String stringContent = (String) content;
			if (part.isMimeType("text/plain")) {
				listener.notifyPlainText(stringContent);

				//メール全体で一番最初に現れたものをメール本文（plain）とみなす
				if (plainTextBody == null) plainTextBody = stringContent;
			} else if (part.isMimeType("text/html")) {
				String htmlCharset = getCharsetFrom(part.getContentType());
				listener.notifyHtmlText(stringContent, htmlCharset);

				//メール全体で一番最初に現れたものをメール本文（html）とみなす
				if (htmlTextBody == null) {
					htmlTextBody = stringContent;
					this.htmlCharset = htmlCharset;
				}
			} else {
				//その他の text/* は添付ファイルとして処理
				processAttachment(part, listener);
			}
		} else if (content instanceof InputStream
			&&
			part instanceof MimePart
			&&
			isQuotedPrintable(((MimePart) part).getEncoding())) {
			//"Content-Transfer-Encoding: quoted-printable"の場合
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			try (InputStream input = (InputStream) content) {
				int c;
				while ((c = input.read()) != -1) {
					output.write(c);
				}

				//"quoted-printable"の場合、Content-typeに指定されているcharsetを見ている（はず）
				//InputStreamから読み込めるbyteは、デコード済のUTF-8なので、UTF-8決め打ちで文字列化
				listener.notifyOtherText(output.toString("UTF-8"));
			}
		} else {
			//不明なものは添付ファイルとして処理
			processAttachment(part, listener);
		}
	}

	void processAttachment(Part part, PartListener listener) throws IOException, MessagingException {
		String fileName = part.getFileName();
		fileName = MimeUtility.decodeText(fileName == null ? "" : fileName);
		String disposition = part.getDisposition();
		if (disposition == null
			|| disposition.equalsIgnoreCase(Part.ATTACHMENT)
			//埋め込みでも、添付ファイルとして扱う
			|| disposition.equalsIgnoreCase(Part.INLINE)) {
			try (InputStream attachment = part.getInputStream()) {
				listener.notifyAttachment(fileName, attachment);
			}
		}
	}

	/**
	 * 解析処理を開始する
	 * @param mimeMessage
	 * @throws IOException
	 * @throws MessagingException
	 */
	private void start(Message mimeMessage) throws IOException, MessagingException {
		parseHeader(mimeMessage);
		parsePart(PartContext.Plain, listener.createPartListener(0), mimeMessage);
	}

	private void parsePart(PartContext context, PartListener listener, Part part) throws IOException, MessagingException {
		String contentType = part.getContentType();
		try {
			listener.notifyContentType(new ContentType(contentType).toString());
		} catch (ParseException e) {
			throw new IllegalStateException(e);
		}

		if (part.isMimeType("text/*")) {
			context.processTextBody(this, part, listener);
		} else if (part.isMimeType("multipart/*")) {
			if (part.isMimeType("multipart/mixed")) {
				listener.notifyMultipartMixed(part.getInputStream());
				context = PartContext.MultipartMixed;
			} else if (part.isMimeType("multipart/alternative")) {
				listener.notifyMultipartAlternative(part.getInputStream());
				context = PartContext.MultipartAlternative;
			}

			Multipart multiPart = (Multipart) part.getContent();
			int count = multiPart.getCount();
			for (int i = 0; i < count; i++)
				parsePart(
					context,
					listener.createMultipartListener(i),
					multiPart.getBodyPart(i));
		} else if (part.isMimeType("message/rfc822")) {
			if (listener.handlesMessageRfc822AsAttachment()) {
				// message/rfc822を添付ファイルとして扱う
				context.processAttachment(this, part, listener);
			} else {
				// message/rfc822を再帰的にparseする
				new MessageParser(listener.createMessageListener()).start(U.readBytes(part.getInputStream()));
			}
		} else if (part.isMimeType("message/partial")) {
			// partがmessage/partialの場合は添付ファイルとして扱う
			context.processAttachment(this, part, listener);
		} else if (part instanceof Part) {
			context.processAttachment(this, part, listener);
		} else {
			throw new IllegalStateException("Unknown Content.");
		}
	}

	/**
	 * メールヘッダ部の処理
	 */
	private void parseHeader(Message message) throws MessagingException {
		Address[] addresses;

		if ((addresses = message.getFrom()) != null) {
			for (Address address : addresses) {
				InternetAddress inetAddress = (InternetAddress) address;
				listener.notifyFrom(inetAddress.getPersonal(), inetAddress.getAddress());
			}
		}

		if ((addresses = message.getReplyTo()) != null) {
			for (Address address : addresses) {
				InternetAddress inetAddress = (InternetAddress) address;
				listener.notifyReplyTo(inetAddress.getPersonal(), inetAddress.getAddress());
			}
		}

		if ((addresses = message.getRecipients(Message.RecipientType.TO)) != null)
			processRecipient(Message.RecipientType.TO, addresses, listener);

		if ((addresses = message.getRecipients(Message.RecipientType.CC)) != null)
			processRecipient(Message.RecipientType.CC, addresses, listener);

		if ((addresses = message.getRecipients(Message.RecipientType.BCC)) != null)
			processRecipient(Message.RecipientType.BCC, addresses, listener);

		listener.notifySubject(message.getSubject());

		listener.notifySendDate(message.getSentDate());

		Enumeration<?> headers = message.getAllHeaders();
		while (headers.hasMoreElements()) {
			Header header = (Header) headers.nextElement();

			String name = header.getName();
			String value = header.getValue();

			listener.notifyHeader(name, value);

			if (name.equalsIgnoreCase("Message-ID")) listener.notifyMessageId(extractMessageId(value));
			else if (name.equalsIgnoreCase("In-Reply-To")) listener.notifyInReplyTo(extractMessageId(value));
		}
	}

	private static String extractMessageId(String messageId) {
		if (messageId.indexOf("<") < 0 || messageId.lastIndexOf(">") < 0)
			return messageId;
		return messageId.substring(messageId.indexOf("<") + 1, messageId.lastIndexOf(">"));
	}

	private static boolean isQuotedPrintable(String encoding) {
		encoding = encoding == null ? "" : encoding;
		return "quoted-printable".equals(encoding.toLowerCase());
	}

	private static final Pattern charsetPattern = Pattern.compile(
		";\\s*charset *= *\"?([^\"]+)\"?",
		Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

	private static String getCharsetFrom(String contentType) {
		Matcher matcher = charsetPattern.matcher(contentType);
		if (!matcher.find()) return null;
		return matcher.group(1);
	}

	private static void processRecipient(RecipientType type, Address[] addresses, MessageListener listener) {
		for (Address address : addresses) {
			InternetAddress inetAddress = (InternetAddress) address;

			String addressString = inetAddress.toString();

			//BCCのみで送信されたメールの場合
			if (addressString.startsWith("undisclosed-recipients")) {
				listener.notifyRecipient(type, inetAddress.getPersonal(), inetAddress.getAddress());
				return;
			}

			listener.notifyRecipient(type, inetAddress.getPersonal(), inetAddress.getAddress());
		}
	}

	/**
	 * HTMLをプレーンテキスト化する
	 */
	private static String convertHtmlToPlainText(String html) {
		if (html == null) return null;
		return HtmlToPlainText.getPlainText(Jsoup.parse(html));
	}
}
