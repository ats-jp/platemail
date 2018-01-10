package jp.ats.platemail;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jp.ats.platemail.account.AccountDataStore;
import jp.ats.platemail.account.AddressUtils;
import jp.ats.platemail.mail.AccountFinder;
import jp.ats.platemail.mail.MessageDigest;
import jp.ats.platemail.mail.MessageIdGenerator;
import jp.ats.relay.CommandExecutor;

public abstract class MailRedirector {

	private static final Logger logger = LogManager.getLogger(MailRedirector.class.getName());

	//ループ検出用オリジナルヘッダ
	private static final String loopDetectHeaderName = "X-Platemail-Redirected";

	private static final String separator = "\r\n";

	private static final ThreadLocal<MailRedirector> mailRedirectorThreadLocal = new ThreadLocal<>();

	private final AccountDataStore dataStore = AccountDataStore.getInstance(Config.getInstance());

	private MessageDigest message;

	public static void prepare(MailRedirector redirector) {
		mailRedirectorThreadLocal.set(redirector);
	}

	public static MailRedirector get() {
		MailRedirector redirector = mailRedirectorThreadLocal.get();
		if (redirector == null) throw new IllegalStateException("prepareが実行されていません");

		return redirector;
	}

	public static MailRedirector getInstance() {
		return U.getInstance(Config.getInstance().getMailRedirectorClass());
	}

	public void setMessage(MessageDigest message) {
		this.message = message;
	}

	protected abstract void redirectInternal(byte[] message, String senderAddress, String resentToAddress);

	public void redirect(String resentToAddress) {
		String resentFromAddress = AccountFinder.findAddress(message);
		String envelopeSender = selectEnvelopeSender(resentToAddress, resentFromAddress, message);
		redirect(resentToAddress, resentFromAddress, envelopeSender, true);
	}

	/**
	 * 転送を行う
	 * @param resentToAddress 転送先アドレス
	 * @param resentFromAddress Resent-Fromアドレス
	 * @param envelopeSender 送信者アドレス
	 * @param needsResentMessageId Resent-Message-IDヘッダが必要かどうか
	 */
	public void redirect(String resentToAddress, String resentFromAddress, String envelopeSender, boolean needsResentMessageId) {
		StringBuilder builder = new StringBuilder();

		//ループ検出用ヘッダ取得
		Set<String> addressesHistory = message.getHeaderElements()
			.stream()
			.filter(header -> header.getHeader().equalsIgnoreCase(loopDetectHeaderName))
			.map(header -> header.getValue())
			.collect(Collectors.toSet());

		if (addressesHistory.contains(resentToAddress)) {
			//今回の宛先が既にヘッダに含まれていた場合、転送を中断
			logger.warn(
				MailRedirector.class.getSimpleName()
					+ " loop detected. to=<"
					+ resentToAddress
					+ ">, all=<"
					+ String.join(", ", addressesHistory)
					+ ">");
			return;
		}

		//転送するメールのヘッダに今回の宛先を追加し、ループ検出の材料とする
		builder.append(loopDetectHeaderName + ": " + resentToAddress);
		builder.append(separator);

		if (isInternalAccount(resentToAddress)) {
			//postfixのループ検出に引っ掛かるため、すべての管理ドメインへの転送は直接転送及び取込を行う
			doInternalRedirection(
				resentToAddress,
				U.concatByteArray(
					builder.toString().getBytes(),
					message.getRawMessage()));
			return;
		}

		/*
		 * 以降は、外部への転送処理
		 */

		builder.append("Resent-Sender: " + resentFromAddress);
		builder.append(separator);
		builder.append("Resent-From: " + resentFromAddress);
		builder.append(separator);
		builder.append("Resent-To: " + resentToAddress);
		builder.append(separator);

		if (needsResentMessageId) {
			builder.append("Resent-Message-ID: <" + MessageIdGenerator.getInstance().generate() + ">");
			builder.append(separator);
		}

		redirectInternal(
			U.concatByteArray(
				builder.toString().getBytes(),
				message.getRawMessage()),
			envelopeSender,
			resentToAddress);

		logger.info(MailRedirector.class.getSimpleName() + " redirected to <" + resentToAddress + ">");
	}

	/**
	 * エンベロープ送信者を特定する
	 * @param resentToAddress 転送先アドレス
	 * @param resentFromAddress 元配信先アドレス
	 * @param message
	 * @return エンベロープ送信者アドレス`
	 */
	public static String selectEnvelopeSender(String resentToAddress, String resentFromAddress, MessageDigest message) {
		if (usesOriginalEnvelopeSender(resentToAddress)) {
			//転送対象メールからReturn-Pathヘッダを取り出し、それをエンベロープ送信者として使用する
			//Gmailなど、転送時にエンベロープ送信者を変更しないことを推奨するサイト用
			/* 参照URL：
			 * https://support.google.com/mail/answer/175365?hl=ja
			 */
			return message.getHeaderElements()
				.stream()
				.filter(e -> U.trim(e.getHeader()).equalsIgnoreCase("return-path"))
				.map(e -> e.getValue())
				.findFirst()
				.orElseThrow(() -> new RuntimeException("転送対象メールにReturn-Pathが見つかりません"));
		}

		//エンベロープ送信者を転送者とする
		//SPFレコードによるドメイン認証がPASSしないと迷惑メール扱いされるサイト用
		return resentFromAddress;
	}

	//管理下のドメインへのメールかを判定
	private boolean isInternalAccount(String address) {
		String[] splitted = AddressUtils.splitMailAddress(address);
		return dataStore.getDomains().contains(splitted[1]);
	}

	private static void doInternalRedirection(String deliveredToAddress, byte[] message) {
		//メール取込側に、エイリアスのアドレスに配信されてきたように見せるため、配送先を表すヘッダを追加
		String deliverToPart = AccountFinder.DELIVERED_TO_HEADER + ": " + deliveredToAddress + separator;

		byte[] deliverToPartBytes = deliverToPart.getBytes(StandardCharsets.UTF_8);

		byte[] concated = U.concatByteArray(deliverToPartBytes, message);

		ByteArrayInputStream inputForTrigger = new ByteArrayInputStream(concated);

		try {
			Config config = Config.getInstance();
			CommandExecutor.getInstance(config.getCommandExecutorClass()).execute(inputForTrigger, config.getTriggerCommand());
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean usesOriginalEnvelopeSender(String toAddress) {
		return U.splitByComma(
			Config.getInstance().getOriginalSenderRedirectionDomains())
			.stream()
			.map(MailRedirector::supplyAtmark)
			.filter(toAddress::endsWith)
			.findFirst()
			.orElse(null) != null;
	}

	private static String supplyAtmark(String target) {
		if (!target.startsWith("@")) return "@" + target;
		return target;
	}
}
