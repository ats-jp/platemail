package jp.ats.platemail;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jp.ats.platemail.mail.AccountFinder;
import jp.ats.platemail.mail.MessageParser;
import jp.ats.platemail.mail.SimpleMessageListener;
import jp.ats.relay.QueueProcess;
import me.normanmaurer.niosmtp.SMTPClientFutureListener;
import me.normanmaurer.niosmtp.core.SMTPByteArrayMessageImpl;
import me.normanmaurer.niosmtp.delivery.DeliveryRecipientStatus;
import me.normanmaurer.niosmtp.delivery.LMTPDeliveryAgent;
import me.normanmaurer.niosmtp.delivery.SMTPDeliveryEnvelope;
import me.normanmaurer.niosmtp.delivery.impl.SMTPDeliveryEnvelopeImpl;
import me.normanmaurer.niosmtp.delivery.impl.StrictLMTPDeliveryAgentConfig;
import me.normanmaurer.niosmtp.transport.FutureResult;
import me.normanmaurer.niosmtp.transport.netty.NettyLMTPClientTransportFactory;

public class LmtpProcess extends QueueProcess {

	private static final Logger logger = LogManager.getLogger(LmtpProcess.class.getName());

	public static final String SPEED_FILE_NAME = "lmtp.speed";

	@Override
	protected Path getQueueDirectory() {
		return Paths.get(Config.getInstance().getWorkDirectoryForLmtp());
	}

	@Override
	protected Path getLockDirectory() {
		return Paths.get(Config.getInstance().getLockDirectoryForLmtp());
	}

	@Override
	protected boolean hasNext() {
		return false;
	}

	@Override
	public boolean usesDatabase() {
		return false;
	}

	@Override
	protected String getNextCommandPath() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Path getNextCommandLockDirectory() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Path getNextCommandQueueDirectory() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int getMaxConcurrency() {
		return Integer.parseInt(Config.getInstance().getMaxConcurrencyOfLmtp());
	}

	@Override
	protected String getSpeedFileName() {
		return SPEED_FILE_NAME;
	}

	private final InetSocketAddress host = new InetSocketAddress(24);

	private final LMTPDeliveryAgent agent = new LMTPDeliveryAgent(NettyLMTPClientTransportFactory.createNio().createPlain());

	private static final SMTPClientFutureListener<Collection<FutureResult<Iterator<DeliveryRecipientStatus>>>> lmtpListener = future -> {
		try {
			future.get().forEach(result -> {
				if (result.isSuccess()) {
					result.getResult().forEachRemaining(status -> {
						logger.info("LMTP success. address=<{}>", status.getAddress());
					});

					return;
				}

				logger.error(result.getException());
			});
		} catch (InterruptedException e) {
			logger.warn(e);
		} catch (ExecutionException e) {
			logger.error(e);
		}
	};

	@Override
	public Path process(Path eml) {
		byte[] bytes;
		try (InputStream input = new BufferedInputStream(Files.newInputStream(eml))) {
			//受信したメールのヘッダと本文
			bytes = U.readBytesWithToCRLF(input);

			FromLineRemover.Result result = FromLineRemover.remove(bytes);

			byte[] message = result.messageBytes;

			SimpleMessageListener listener = new SimpleMessageListener();

			try {
				new MessageParser(listener).start(message);
				String sentToAddress = AccountFinder.findAddress(listener);

				List<String> recipients = new LinkedList<>();
				recipients.add(sentToAddress);

				SMTPDeliveryEnvelope[] envelopes = SMTPDeliveryEnvelopeImpl.create(result.fromAddress.get()/*必須*/, recipients, new SMTPByteArrayMessageImpl(message));

				agent.deliver(host, new StrictLMTPDeliveryAgentConfig(), envelopes).addListener(lmtpListener);

				return null;
			} catch (Exception e) {
				Path path = ErrorMailSaver.saveEvidence("mail-lmtp", message);

				throw ProcessUtils.error(logger, "処理中に、想定外のエラーが発生しました。 eml=[" + path.toAbsolutePath() + "]", e);
			}
		} catch (IOException e) {
			throw ProcessUtils.error(logger, "処理中に、想定外のエラーが発生しました", e);
		}
	}
}
