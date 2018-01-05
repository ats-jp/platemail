package jp.ats.platemail.mail;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class DummyMailRedirector extends MailRedirector {

	private static final Logger logger = LogManager.getLogger(DummyMailRedirector.class.getName());

	@Override
	protected void redirectInternal(byte[] message, String senderAddress, String resentToAddress) {
		logger.info("senderAddress: " + senderAddress);
		logger.info("resentToAddress: " + resentToAddress);
		logger.info(new String(message));
	}
}
