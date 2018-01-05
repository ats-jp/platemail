package jp.ats.platemail.account.filter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import jp.ats.platemail.common.CP932;
import jp.ats.platemail.common.LineSpliterator;
import jp.ats.platemail.common.U;
import jp.ats.platemail.common.LineSpliterator.Brake;
import jp.ats.platemail.mail.Address;
import jp.ats.platemail.mail.MailBuilder;
import jp.ats.platemail.mail.MessageDigest;
import jp.ats.platemail.mail.MessageParser;
import jp.ats.platemail.mail.PartListener;
import jp.ats.platemail.mail.Recipient;
import jp.ats.platemail.mail.SimpleMessageListener;

class MessageTruncator {

	static long getBodySize(byte[] message) {
		long headerLength[] = { 0 };
		try {
			new LineSpliterator().split(new ByteArrayInputStream(message), (buffer, length) -> {
				int choppedLength = U.getNewLineChoppedLength(buffer, length);
				if (choppedLength == 0) {
					headerLength[0] += length; //ヘッダとボディを区切る改行のみの行サイズを追加
					throw new Brake();
				}

				headerLength[0] += length;
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return message.length - headerLength[0];
	}

	static Result truncateAndBuildMessage(byte[] message, long limitBytes) {
		MyMessageListener listener = new MyMessageListener();
		try {
			new MessageParser(listener).start(message);
		} catch (IOException | MessagingException e) {
			throw new RuntimeException(e);
		}

		MailBuilder builder = MailBuilder.getInstance();

		listener.getRecipients().forEach(header -> moveRecipients(builder, header));

		try {
			if (listener.from != null)
				builder.setFrom(listener.from.getStringExpression());

			if (listener.subject != null)
				builder.setSubject(listener.subject);

			//postfixにResent-Message-Idを付与させないために、ここで付けておく
			builder.addHeader("Resent-Message-ID", "<" + builder.getGeneratedMessageId().get() + ">");
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}

		Result result = new Result();
		result.originalMessage = listener;

		if (listener.multipartAlternative != null && listener.multipartAlternative.length <= limitBytes) {
			//本文がmultipart/alternativeで制限サイズ以下の場合

			//以下は、元のmultipart/alternativeデータをそのまま使うための処置
			try {
				byte[] built = builder.buildHeader();

				//書き換えるContent-Typeヘッダ
				byte[] contentTypeBytes = ("Content-Type: " + listener.multipartAlternativeContentType).getBytes();

				//完成eml状態のサイズを計算してバッファを作成
				ByteBuffer resultBytes = ByteBuffer.allocate(
					built.length//ヘッダのサイズ
						+ contentTypeBytes.length //Content-Type行分
						+ 4 //Content-Type行の改行コード分(2byte) + ヘッダとボディ区切りの空行分(2byte)
						+ listener.multipartAlternative.length //alternative分
				);

				//ヘッダ
				resultBytes.put(built);
				//ContentType行
				resultBytes.put(contentTypeBytes);
				//Content-Type行の改行コード分 + ヘッダとボディ区切りの空行分
				//Content-Transfer-Encodingは不要なはず・・・
				resultBytes.put(new byte[] { '\r', '\n', '\r', '\n' });

				//ボディ
				resultBytes.put(listener.multipartAlternative);

				result.truncated = new byte[resultBytes.position()];
				System.arraycopy(resultBytes.array(), 0, result.truncated, 0, result.truncated.length);

				return result;
			} catch (MessagingException e) {
				throw new RuntimeException(e);
			}
		}

		try {
			String html = listener.htmlText;
			if (html != null && html.getBytes(MailBuilder.CHARSET).length <= limitBytes) {
				builder.setHtmlMessage(CP932.treatForCP932(html));
			} else {
				builder.setMessage(CP932.treatForCP932(U.truncate(MailBuilder.CHARSET, listener.plainText, limitBytes)));
			}

			result.truncated = builder.build();

			return result;
		} catch (IOException | MessagingException e) {
			throw new RuntimeException(e);
		}
	}

	private static void moveRecipients(MailBuilder builder, Recipient recipient) {
		try {
			RecipientType type = recipient.getType();

			String personal = recipient.getPersonal();
			InternetAddress address;
			if (U.presents(personal)) {
				address = new InternetAddress(recipient.getAddress(), personal, MailBuilder.CHARSET.name());
			} else {
				address = new InternetAddress(recipient.getAddress());
			}

			if (type == RecipientType.TO) builder.addMailTo(address);
			else if (type == RecipientType.CC) builder.addCC(address);
		} catch (UnsupportedEncodingException | MessagingException e) {
			throw new RuntimeException(e);
		}
	}

	static class Result {

		byte[] truncated;

		MessageDigest originalMessage;
	}

	private static class MyMessageListener extends SimpleMessageListener {

		private Address from;

		private String subject;

		private String plainText;

		private String htmlText;

		private String multipartAlternativeContentType;

		private byte[] multipartAlternative;

		@Override
		public void notifyFrom(String personal, String address) {
			from = new Address(personal, address);
		}

		@Override
		public void notifySubject(String subject) {
			this.subject = subject;
		}

		@Override
		public void notifyPlainTextBody(String body) {
			plainText = body;
		}

		@Override
		public void notifyHtmlTextBody(String body, String charset) {
			htmlText = body;
		}

		@Override
		public PartListener createPartListener(int index) {
			return new MyPartListener();
		}

		private class MyPartListener implements PartListener {

			private String contentType;

			@Override
			public void notifyContentType(String contentType) {
				this.contentType = contentType;
			}

			@Override
			public void notifyMultipartAlternative(InputStream multipart) {
				multipartAlternativeContentType = contentType;
				try {
					multipartAlternative = U.readBytes(multipart);
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}

			@Override
			public PartListener createMultipartListener(int multipartIndex) {
				return new MyPartListener();
			}
		}
	}
}
