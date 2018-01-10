package jp.ats.platemail;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class MailRedirectorStub extends MailRedirector {

	private static final Logger logger = LogManager.getLogger(MailRedirectorStub.class.getName());

	@Override
	protected void redirectInternal(byte[] message, String senderAddress, String resentToAddress) {
		logger.info("senderAddress: " + senderAddress);
		logger.info("resentToAddress: " + resentToAddress);
		logger.info(new String(message));
	}
}
