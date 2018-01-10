package jp.ats.platemail.process;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jp.ats.platemail.account.Account;
import jp.ats.platemail.account.AccountManager;
import jp.ats.platemail.common.Config;
import jp.ats.platemail.common.U;
import jp.ats.platemail.mail.AccountFinder;
import jp.ats.platemail.mail.MailRedirector;
import jp.ats.platemail.mail.MessageParser;
import jp.ats.platemail.mail.SimpleMessageListener;
import jp.ats.relay.QueueProcess;

public class MailTransferProcess extends QueueProcess {

	private static final Logger logger = LogManager.getLogger(MailTransferProcess.class.getName());

	public static final String SPEED_FILE_NAME = "transfer.speed";

	private final Config config = Config.getInstance();

	@Override
	protected Path getQueueDirectory() {
		return Paths.get(config.getWorkDirectoryForTransfer());
	}

	@Override
	protected Path getLockDirectory() {
		return Paths.get(config.getLockDirectoryForTransfer());
	}

	@Override
	protected boolean hasNext() {
		return Boolean.parseBoolean(config.hasNext());
	}

	@Override
	public boolean usesDatabase() {
		return Boolean.parseBoolean(config.usesDatabase());
	}

	@Override
	protected String getNextCommandPath() {
		return config.getNextCommand();
	}

	@Override
	protected Path getNextCommandLockDirectory() {
		return Paths.get(config.getLockDirectoryForNext());
	}

	@Override
	protected Path getNextCommandQueueDirectory() {
		return Paths.get(config.getWorkDirectoryForNext());
	}

	@Override
	protected int getMaxConcurrency() {
		return Integer.parseInt(config.getMaxConcurrency());
	}

	@Override
	protected String getSpeedFileName() {
		return SPEED_FILE_NAME;
	}

	@Override
	protected void postProcessWithLock() {
		//自身のプロセスロックを取得している中で、トリガーの多重起動を抑制するロックファイルを削除
		try {
			Files.deleteIfExists(Paths.get(config.getLockDirectoryForTrigger()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Path process(Path eml) {
		byte[] bytes;
		try (InputStream input = new BufferedInputStream(Files.newInputStream(eml))) {
			//受信したメールのヘッダと本文
			//Postfixから受け取った場合は改行がLFなので、CRLFに変換
			bytes = U.readBytesWithToCRLF(input);
			byte[] message = FromLineRemover.remove(bytes).messageBytes;
			boolean result;
			try {
				result = execute(message);
			} catch (Exception e) {
				Path path = ErrorMailSaver.saveEvidence("mail-transfer", message);
				throw error(logger, "処理中に、想定外のエラーが発生しました。 eml=[" + path.toAbsolutePath() + "]", e);
			}

			//falseの場合、ローカルアカウントではないので、以降の処理に行かないように後続処理を行わない
			if (!result) return null;
		} catch (IOException e) {
			throw error(logger, "処理中に、想定外のエラーが発生しました", e);
		}

		//以後のプロセスが変換しなくても済むように、改行コードをCRLF化したデータで書き換え
		try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(eml))) {
			U.sendBytes(new ByteArrayInputStream(bytes), output);
		} catch (IOException e) {
			throw error(logger, "処理中に、想定外のエラーが発生しました", e);
		}

		return eml;
	}

	private static final ThreadLocal<AccountManager> accountManager = new ThreadLocal<>();

	//アカウントが更新されている場合、最新版を読込なおす
	//連続で処理する場合に、何度もAccountManagerをロックしないための処置
	private static AccountManager accountManager(Config config) {
		AccountManager manager = accountManager.get();
		if (manager == null || manager.wasUpdated()) {
			manager = AccountManager.getInstance(config);
			accountManager.set(manager);
		}

		return manager;
	}

	private static RuntimeException error(Logger logger, String message, Throwable t) {
		logger.error(message, t);
		return new RuntimeException(message, t);
	}

	private boolean execute(byte[] message) throws Exception {
		SimpleMessageListener listener = new SimpleMessageListener();
		new MessageParser(listener).start(message);

		String sentToAddress = AccountFinder.findAddress(listener);

		Account account = accountManager(config).getAccount(sentToAddress);

		//バウンスメールだった場合、以下の処理を中断
		if (!checkBounceMail(listener)) {
			//ローカルアカウントだった場合、自アプリケーションで内容を確認できるため
			//ループする転送処理を行わずに自処理に流す
			return account.isLocalAccount();
		}

		if (account == null) {
			logger.warn(
				MailTransferProcess.class.getSimpleName()
					+
					" unknown address. <"
					+ sentToAddress
					+ ">, mid=<"
					+ listener.getMessageId()
					+ ">");
			return false;
		}

		boolean deniesTransferOutside = Boolean.parseBoolean(config.deniesTransferOutside());

		boolean[] redirected = { false };
		account.getFilters().forEach(filter -> {
			if (!filter.isUses()) return;

			if (filter.getLogicalOperator().matches(filter.getConditions(), listener)) {
				String sendTo = filter.getSendTo();

				//外部転送禁止で外部アドレスの場合
				if (deniesTransferOutside && isNotLocalDomainAddress(config, sendTo)) {
					logger.warn(
						MailTransferProcess.class.getSimpleName()
							+
							" do not transfer [filter]. account=<"
							+ sentToAddress
							+ ">, to=<"
							+ sendTo
							+ ">, mid=<"
							+ listener.getMessageId()
							+ ">");
					return;
				}

				redirected[0] |= filter.getMailSizeOperator().delegateRedirection(
					message,
					sendTo,
					filter.getMailSizeKBytes() * 1024);
			}
		});

		//エイリアス先に転送しない場合
		if (redirected[0] && !account.isAliasWhenFilterRedirection()) {
			logger.info(MailTransferProcess.class.getSimpleName() + " filter redirected. <" + sentToAddress + ">, mid=<" + listener.getMessageId() + ">");
			return account.isLocalAccount();
		}

		//エイリアスに転送
		account.getAliases().forEach(alias -> {
			//外部転送禁止で外部アドレスの場合
			if (deniesTransferOutside && isNotLocalDomainAddress(config, alias)) {
				logger.warn(MailTransferProcess.class.getSimpleName() + " do not transfer [alias]. account=<" + sentToAddress + ">, to=<" + alias + ">, mid=<" + listener.getMessageId() + ">");
				return;
			}

			MailRedirector redirector = MailRedirector.getInstance();
			redirector.setMessage(listener);
			redirector.redirect(alias);
		});

		logger.info(MailTransferProcess.class.getSimpleName() + " end. <" + sentToAddress + ">, mid=<" + listener.getMessageId() + ">");

		//次処理に移るかどうかを返す
		return account.isLocalAccount();
	}

	private static final Pattern domainPattern = Pattern.compile("@([^@]+)$");

	private static boolean isNotLocalDomainAddress(Config config, String address) {
		Matcher matcher = domainPattern.matcher(U.trim(address));

		//そもそもアドレスがおかしい場合、とりあえず外部のアドレスと判断、後の処理に任せる
		if (!matcher.find()) return true;

		String domain = U.removeWhiteSpaces(matcher.group(1));

		return !config.getLocalDomains().contains(domain);
	}

	private boolean checkBounceMail(SimpleMessageListener listener) {
		String bounceMailFrom = config.getBounceMailFrom();
		boolean isBounceMail = listener.getHeaderElements()
			.stream()
			.filter(header -> header.getHeader().equalsIgnoreCase("from") && header.getValue().contains(bounceMailFrom))
			.findFirst()
			.isPresent();

		if (isBounceMail) {
			Path path = ErrorMailSaver.saveEvidence("mail-transfer", "bounce-mail", listener.getRawMessage());
			logger.warn(MailTransferProcess.class.getSimpleName() + " bounce mail detected. <" + path + ">, mid=<" + listener.getMessageId() + ">");
			return false;
		}

		return true;
	}
}
