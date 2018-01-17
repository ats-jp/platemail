package jp.ats.platemail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jp.ats.relay.QueueProcess;

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

	@Override
	public Path process(Path eml) {
		byte[] message;
		try {
			message = U.readBytes(Files.newInputStream(eml));
		} catch (IOException e) {
			throw ProcessUtils.error(logger, "処理中に、想定外のエラーが発生しました", e);
		}

		try {
			LmtpClient.send(message);
			return null;
		} catch (Exception e) {
			Path path = ErrorMailSaver.saveEvidence("mail-lmtp", message);
			throw ProcessUtils.error(logger, "処理中に、想定外のエラーが発生しました。 eml=[" + path.toAbsolutePath() + "]", e);
		}
	}
}
