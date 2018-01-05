package jp.ats.platemail.account.filter;

import java.io.IOException;

import javax.mail.MessagingException;

import jp.ats.platemail.account.filter.MessageTruncator.Result;
import jp.ats.platemail.mail.AccountFinder;
import jp.ats.platemail.mail.MailRedirector;
import jp.ats.platemail.mail.MessageDigest;
import jp.ats.platemail.mail.MessageParser;
import jp.ats.platemail.mail.SimpleMessageListener;

/**
 * メールサイズによる処理切り替えの定義
 */
public enum MailSizeOperator {

	/**
	 * 使用しない
	 */
	PASS {

		@Override
		public boolean delegateRedirection(
			byte[] message,
			String resentToAddress,
			long limitBytes) {
			redirect(message, resentToAddress);
			return true;
		}
	},

	/**
	 * 転送拒否
	 */
	REJECT {

		@Override
		public boolean delegateRedirection(
			byte[] message,
			String resentToAddress,
			long limitBytes) {
			long bodySize = MessageTruncator.getBodySize(message);

			if (bodySize > limitBytes) return false;

			redirect(message, resentToAddress);
			return true;
		}
	},

	/**
	 * サイズ制限転送
	 */
	TRUNCATE {

		@Override
		public boolean delegateRedirection(
			byte[] message,
			String resentToAddress,
			long limitBytes) {
			long bodySize = MessageTruncator.getBodySize(message);
			if (bodySize <= limitBytes) {
				redirect(message, resentToAddress);
				return true;
			}

			Result result = MessageTruncator.truncateAndBuildMessage(message, limitBytes);
			redirectTruncatedMessage(result.truncated, result.originalMessage, resentToAddress);
			return true;
		}
	};

	public abstract boolean delegateRedirection(
		byte[] message,
		String resentToAddress,
		long limitBytes);

	private static void redirect(
		byte[] message,
		String resentToAddress) {
		MailRedirector redirector = MailRedirector.getInstance();
		SimpleMessageListener listener = new SimpleMessageListener();
		try {
			new MessageParser(listener).start(message);
		} catch (IOException | MessagingException e) {
			throw new RuntimeException(e);
		}

		redirector.setMessage(listener);
		redirector.redirect(resentToAddress);
	}

	private static void redirectTruncatedMessage(
		byte[] message,
		MessageDigest original,
		String resentToAddress) {
		MailRedirector redirector = MailRedirector.getInstance();
		SimpleMessageListener listener = new SimpleMessageListener();
		try {
			new MessageParser(listener).start(message);
		} catch (IOException | MessagingException e) {
			throw new RuntimeException(e);
		}

		//オリジナルのメールから転送に必要な情報を抽出
		String resentFromAddress = AccountFinder.findAddress(original);
		String envelopeSender = MailRedirector.selectEnvelopeSender(resentToAddress, resentFromAddress, original);

		redirector.setMessage(listener);
		redirector.redirect(resentToAddress, resentFromAddress, envelopeSender, false);
	}
}
