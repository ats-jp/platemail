package jp.ats.platemail.mail;

import java.io.ByteArrayInputStream;

import jp.ats.platemail.common.Config;
import jp.ats.relay.CommandExecutor;

public class MailRedirectorImpl extends MailRedirector {

	@Override
	protected void redirectInternal(byte[] message, String senderAddress, String resentToAddress) {
		try {
			Config config = Config.getInstance();
			CommandExecutor.getInstance(config.getCommandExecutorClass()).execute(
				new ByteArrayInputStream(message),
				config.getMailSendCommand(),
				senderAddress,
				resentToAddress);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
